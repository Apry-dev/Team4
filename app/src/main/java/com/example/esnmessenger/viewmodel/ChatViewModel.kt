package com.example.esnmessenger.viewmodel

import androidx.lifecycle.ViewModel
import com.example.esnmessenger.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var listenerRegistration: ListenerRegistration? = null

    private fun chatId(uid1: String, uid2: String) =
        listOf(uid1, uid2).sorted().joinToString("_")

    fun loadMessages(otherUserId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val cid = chatId(currentUid, otherUserId)

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
                _messages.value = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()
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
