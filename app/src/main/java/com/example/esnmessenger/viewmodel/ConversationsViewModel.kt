package com.example.esnmessenger.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esnmessenger.model.ChatSummary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatListViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var blockedUids = setOf<String>()
    private var allChats = listOf<ChatSummary>()

    private val _chats = MutableStateFlow<List<ChatSummary>>(emptyList())
    val chats: StateFlow<List<ChatSummary>> = _chats.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchActive = MutableStateFlow(false)
    val searchActive: StateFlow<Boolean> = _searchActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Triple<String, String, String>>>(emptyList())
    val searchResults: StateFlow<List<Triple<String, String, String>>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    init {
        val myUid = auth.currentUser?.uid
        if (myUid != null) {
            listenToBlocks(myUid)
            listenForExistingChats()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(700)
            _isRefreshing.value = false
        }
    }

    private fun applyFilter() {
        _chats.value = allChats.filter { it.otherUserId !in blockedUids }
    }

    private fun listenToBlocks(myUid: String) {
        firestore.collection("blocks").document(myUid)
            .addSnapshotListener { doc, _ ->
                blockedUids = (doc?.get("uids") as? List<*>)
                    ?.filterIsInstance<String>()?.toSet() ?: emptySet()
                applyFilter()
            }
    }

    private fun listenForExistingChats() {
        val currentUserUid = auth.currentUser?.uid ?: return

        firestore.collection("chats")
            .whereArrayContains("participants", currentUserUid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatListViewModel", "Query failed: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val docs = snapshot.documents
                if (docs.isEmpty()) {
                    allChats = emptyList()
                    applyFilter()
                    return@addSnapshotListener
                }

                val chatList = mutableListOf<ChatSummary>()
                var remaining = docs.size

                for (doc in docs) {
                    val participants = doc.get("participants") as? List<String>
                    val otherUserId = participants?.firstOrNull { it != currentUserUid }

                    if (participants == null || otherUserId == null) {
                        remaining--
                        if (remaining == 0) { allChats = chatList.sortedByDescending { it.timestamp }; applyFilter() }
                        continue
                    }

                    val lastMessage = doc.getString("lastMessage") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    val unreadCount = doc.getLong("unreadCount_$currentUserUid")?.toInt() ?: 0

                    firestore.collection("users").document(otherUserId).get()
                        .addOnSuccessListener { userDoc ->
                            val name = userDoc.getString("name")
                                ?: userDoc.getString("email")
                                ?: otherUserId
                            chatList.add(ChatSummary(
                                chatId = doc.id,
                                otherUserId = otherUserId,
                                otherUserName = name,
                                lastMessage = lastMessage,
                                timestamp = timestamp,
                                otherUserPhotoBase64 = userDoc.getString("photoBase64"),
                                unreadCount = unreadCount,
                                otherUserLastSeen = userDoc.getLong("lastSeen") ?: 0L
                            ))
                            remaining--
                            if (remaining == 0) { allChats = chatList.sortedByDescending { it.timestamp }; applyFilter() }
                        }
                        .addOnFailureListener {
                            chatList.add(ChatSummary(
                                chatId = doc.id,
                                otherUserId = otherUserId,
                                otherUserName = otherUserId,
                                lastMessage = lastMessage,
                                timestamp = timestamp,
                                unreadCount = unreadCount
                            ))
                            remaining--
                            if (remaining == 0) { allChats = chatList.sortedByDescending { it.timestamp }; applyFilter() }
                        }
                }
            }
    }

    fun setSearchActive(active: Boolean) {
        _searchActive.value = active
        if (!active) {
            _searchQuery.value = ""
            _searchResults.value = emptyList()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _isSearching.value = true
            try {
                val q = query.trim().lowercase()
                val myUid = auth.currentUser?.uid
                val byEmail = firestore.collection("users")
                    .whereGreaterThanOrEqualTo("email", q)
                    .whereLessThan("email", q + '')
                    .limit(20).get().await()
                val byName = firestore.collection("users")
                    .whereGreaterThanOrEqualTo("name", query.trim())
                    .whereLessThan("name", query.trim() + '')
                    .limit(20).get().await()
                _searchResults.value = (byEmail.documents + byName.documents)
                    .distinctBy { it.id }
                    .filter { it.id != myUid }
                    .map { Triple(it.id, it.getString("email") ?: "", it.getString("name") ?: "") }
            } catch (_: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun markAllAsRead() {
        val myUid = auth.currentUser?.uid ?: return
        val unreadChats = allChats.filter { it.unreadCount > 0 }
        if (unreadChats.isEmpty()) return
        val batch = firestore.batch()
        unreadChats.forEach { chat ->
            batch.update(firestore.collection("chats").document(chat.chatId), "unreadCount_$myUid", 0)
        }
        batch.commit()
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}
