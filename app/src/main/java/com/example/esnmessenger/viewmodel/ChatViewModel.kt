package com.example.esnmessenger.viewmodel

import androidx.lifecycle.ViewModel
import com.example.esnmessenger.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _otherUserName = MutableStateFlow<String?>(null)
    val otherUserName: StateFlow<String?> = _otherUserName

    private val _otherUserPhotoBase64 = MutableStateFlow<String?>(null)
    val otherUserPhotoBase64: StateFlow<String?> = _otherUserPhotoBase64

    private var listenerRegistration: ListenerRegistration? = null

    private fun chatId(uid1: String, uid2: String) =
        listOf(uid1, uid2).sorted().joinToString("_")

    fun loadMessages(otherUserId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val cid = chatId(currentUid, otherUserId)

        db.collection("users").document(otherUserId).get()
            .addOnSuccessListener { doc ->
                _otherUserName.value = doc.getString("name")
                _otherUserPhotoBase64.value = doc.getString("photoBase64")
            }

        listenerRegistration?.remove()
        listenerRegistration = db.collection("chats")
            .document(cid)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = e.message
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents ?: emptyList()
                _messages.value = docs.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                }
                // Mark unread incoming messages as read
                val batch = db.batch()
                var hasUnread = false
                docs.forEach { doc ->
                    val msg = doc.toObject(Message::class.java) ?: return@forEach
                    if (msg.toId == currentUid && !msg.read) {
                        batch.update(doc.reference, "read", true)
                        hasUnread = true
                    }
                }
                if (hasUnread) batch.commit()
            }
    }

    fun sendMessage(otherUserId: String, text: String) {
        val currentUid = auth.currentUser?.uid ?: return
        if (text.isBlank()) return

        val cid = chatId(currentUid, otherUserId)
        val message = Message(
            fromId = currentUid,
            toId = otherUserId,
            text = text.trim()
        )

        db.collection("chats").document(cid)
            .set(
                mapOf(
                    "participants" to listOf(currentUid, otherUserId),
                    "lastMessage" to text.trim(),
                    "timestamp" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )

        db.collection("chats")
            .document(cid)
            .collection("messages")
            .add(message)
            .addOnFailureListener { e -> _error.value = e.message }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
