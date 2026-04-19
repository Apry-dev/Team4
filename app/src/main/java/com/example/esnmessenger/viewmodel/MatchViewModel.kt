package com.example.esnmessenger.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MatchProfile(
    val uid: String,
    val name: String,
    val photoBase64: String?,
    val country: String,
    val university: String,
    val year: String,
    val major: String,
    val interests: List<String>,
    val languages: List<String>,
    val studentType: String
)

class MatchViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _myInterests = MutableStateFlow<Set<String>>(emptySet())
    val myInterests: StateFlow<Set<String>> = _myInterests.asStateFlow()

    private var myName = ""

    private val _allCandidates = MutableStateFlow<List<MatchProfile>>(emptyList())

    private val _candidates = MutableStateFlow<List<MatchProfile>>(emptyList())
    val candidates: StateFlow<List<MatchProfile>> = _candidates.asStateFlow()

    private val _filterLanguage = MutableStateFlow<String?>(null)
    val filterLanguage: StateFlow<String?> = _filterLanguage.asStateFlow()

    private val _filterStudentType = MutableStateFlow<String?>(null)
    val filterStudentType: StateFlow<String?> = _filterStudentType.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _matchedWith = MutableStateFlow<MatchProfile?>(null)
    val matchedWith: StateFlow<MatchProfile?> = _matchedWith.asStateFlow()

    private val _pendingMatchNotification = MutableStateFlow<String?>(null)
    val pendingMatchNotification: StateFlow<String?> = _pendingMatchNotification.asStateFlow()

    init {
        loadCandidates()
    }

    fun setFilterLanguage(lang: String?) { _filterLanguage.value = lang; applyFilters() }
    fun setFilterStudentType(type: String?) { _filterStudentType.value = type; applyFilters() }
    fun clearMatch() { _matchedWith.value = null }
    fun clearPendingNotification() { _pendingMatchNotification.value = null }

    private fun applyFilters() {
        val lang = _filterLanguage.value
        val type = _filterStudentType.value
        val myInt = _myInterests.value
        _candidates.value = _allCandidates.value
            .filter { profile ->
                (lang == null || lang in profile.languages) &&
                (type == null || profile.studentType == type)
            }
            .sortedByDescending { profile -> profile.interests.count { it in myInt } }
    }

    private fun loadCandidates() {
        val myUid = auth.currentUser?.uid ?: run { _loading.value = false; return }
        val seenUids = mutableSetOf(myUid)

        // Load my profile (interests + name) and blocked users, then candidates
        firestore.collection("users").document(myUid).get()
            .addOnSuccessListener { doc ->
                val interests = (doc.get("interests") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                _myInterests.value = interests.toSet()
                myName = doc.getString("name") ?: ""
                applyFilters()
                checkPendingNotifications(myUid)
            }

        firestore.collection("blocks").document(myUid).get()
            .addOnSuccessListener { doc ->
                val blocked = (doc.get("uids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                seenUids.addAll(blocked)
            }

        firestore.collection("matches").document(myUid).collection("liked").get()
            .addOnSuccessListener { liked ->
                liked.forEach { seenUids.add(it.id) }
                firestore.collection("matches").document(myUid).collection("passed").get()
                    .addOnSuccessListener { passed ->
                        passed.forEach { seenUids.add(it.id) }
                        fetchUsers(seenUids)
                    }
                    .addOnFailureListener { fetchUsers(seenUids) }
            }
            .addOnFailureListener { fetchUsers(seenUids) }
    }

    private fun checkPendingNotifications(myUid: String) {
        firestore.collection("notifications").document(myUid)
            .collection("matches").get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) return@addOnSuccessListener
                val fromName = docs.documents.first().getString("fromName") ?: return@addOnSuccessListener
                _pendingMatchNotification.value = fromName
                docs.documents.forEach { it.reference.delete() }
            }
    }

    private fun fetchUsers(seenUids: Set<String>) {
        firestore.collection("users").get()
            .addOnSuccessListener { docs ->
                val profiles = docs.documents
                    .filter { it.id !in seenUids }
                    .mapNotNull { doc ->
                        val name = doc.getString("name") ?: return@mapNotNull null
                        MatchProfile(
                            uid = doc.id,
                            name = name,
                            photoBase64 = doc.getString("photoBase64"),
                            country = doc.getString("country") ?: "",
                            university = doc.getString("university") ?: "",
                            year = doc.getString("year") ?: "",
                            major = doc.getString("major") ?: "",
                            interests = (doc.get("interests") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            languages = (doc.get("languages") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            studentType = doc.getString("studentType") ?: ""
                        )
                    }
                _allCandidates.value = profiles
                applyFilters()
                _loading.value = false
            }
            .addOnFailureListener { _loading.value = false }
    }

    fun like(profile: MatchProfile) {
        val myUid = auth.currentUser?.uid ?: return
        firestore.collection("matches").document(myUid)
            .collection("liked").document(profile.uid)
            .set(mapOf("timestamp" to System.currentTimeMillis()))

        firestore.collection("matches").document(profile.uid)
            .collection("liked").document(myUid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    createConversationIfNeeded(myUid, profile.uid)
                    _matchedWith.value = profile
                    // Notify the other user
                    firestore.collection("notifications").document(profile.uid)
                        .collection("matches")
                        .add(mapOf(
                            "fromUid" to myUid,
                            "fromName" to myName,
                            "timestamp" to System.currentTimeMillis()
                        ))
                }
            }
        advance(profile)
    }

    fun pass(profile: MatchProfile) {
        val myUid = auth.currentUser?.uid ?: return
        firestore.collection("matches").document(myUid)
            .collection("passed").document(profile.uid)
            .set(mapOf("timestamp" to System.currentTimeMillis()))
        advance(profile)
    }

    private fun advance(profile: MatchProfile) {
        _allCandidates.value = _allCandidates.value.filter { it.uid != profile.uid }
        applyFilters()
    }

    private fun createConversationIfNeeded(uid1: String, uid2: String) {
        val participants = listOf(uid1, uid2).sorted()
        val chatId = participants.joinToString("_")
        val chatRef = firestore.collection("chats").document(chatId)
        chatRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                chatRef.set(mapOf(
                    "participants" to participants,
                    "lastMessage" to "",
                    "timestamp" to System.currentTimeMillis()
                ))
            }
        }
    }
}
