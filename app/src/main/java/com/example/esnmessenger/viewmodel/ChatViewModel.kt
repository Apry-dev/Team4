package com.example.esnmessenger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esnmessenger.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _otherUserName = MutableStateFlow<String?>(null)
    val otherUserName: StateFlow<String?> = _otherUserName

    private val _otherUserPhotoBase64 = MutableStateFlow<String?>(null)
    val otherUserPhotoBase64: StateFlow<String?> = _otherUserPhotoBase64

    private val _otherUserIsTyping = MutableStateFlow(false)
    val otherUserIsTyping: StateFlow<Boolean> = _otherUserIsTyping

    private var listenerRegistration: ListenerRegistration? = null
    private var typingListenerRegistration: ListenerRegistration? = null
    private var typingJob: Job? = null
    private var currentOtherUserId: String? = null

    private fun chatId(uid1: String, uid2: String) =
        listOf(uid1, uid2).sorted().joinToString("_")

    fun loadMessages(otherUserId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        currentOtherUserId = otherUserId
        val cid = chatId(currentUid, otherUserId)

        _isLoading.value = true

        db.collection("users").document(otherUserId).get()
            .addOnSuccessListener { doc ->
                _otherUserName.value = doc.getString("name")
                _otherUserPhotoBase64.value = doc.getString("photoBase64")
            }

        // Listen for typing indicator from the other user
        typingListenerRegistration?.remove()
        typingListenerRegistration = db.collection("users").document(otherUserId)
            .addSnapshotListener { snapshot, _ ->
                val typingTo = snapshot?.getString("isTypingTo")
                _otherUserIsTyping.value = typingTo == currentUid
            }

        listenerRegistration?.remove()
        var firstSnapshot = true
        listenerRegistration = db.collection("chats")
            .document(cid)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (firstSnapshot) {
                    _isLoading.value = false
                    firstSnapshot = false
                }
                if (e != null) {
                    _error.value = e.message
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents ?: emptyList()
                _messages.value = docs.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                }
                val batch = db.batch()
                var hasUnread = false
                docs.forEach { doc ->
                    val msg = doc.toObject(Message::class.java) ?: return@forEach
                    if (msg.toId == currentUid && !msg.read) {
                        batch.update(doc.reference, "read", true)
                        hasUnread = true
                    }
                }
                if (hasUnread) {
                    batch.commit()
                    db.collection("chats").document(cid)
                        .update("unreadCount_$currentUid", 0)
                }
            }
    }

    fun updateTypingState(isTyping: Boolean) {
        val currentUid = auth.currentUser?.uid ?: return
        val otherUserId = currentOtherUserId ?: return
        typingJob?.cancel()
        if (isTyping) {
            db.collection("users").document(currentUid)
                .set(mapOf("isTypingTo" to otherUserId), SetOptions.merge())
            typingJob = viewModelScope.launch {
                delay(3000)
                clearTypingState()
            }
        } else {
            clearTypingState()
        }
    }

    private fun clearTypingState() {
        val currentUid = auth.currentUser?.uid ?: return
        db.collection("users").document(currentUid)
            .update("isTypingTo", null)
    }

    fun sendMessage(otherUserId: String, text: String) {
        val currentUid = auth.currentUser?.uid ?: return
        if (text.isBlank()) return

        val cid = chatId(currentUid, otherUserId)
        val trimmed = text.trim()

        clearTypingState()
        typingJob?.cancel()

        db.collection("chats").document(cid)
            .set(
                mapOf(
                    "participants" to listOf(currentUid, otherUserId),
                    "lastMessage" to trimmed,
                    "timestamp" to System.currentTimeMillis(),
                    "unreadCount_$otherUserId" to FieldValue.increment(1)
                ),
                SetOptions.merge()
            )

        db.collection("chats")
            .document(cid)
            .collection("messages")
            .add(Message(fromId = currentUid, toId = otherUserId, text = trimmed))
            .addOnFailureListener { e -> _error.value = e.message }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
        typingListenerRegistration?.remove()
        typingJob?.cancel()
        clearTypingState()
    }
}
