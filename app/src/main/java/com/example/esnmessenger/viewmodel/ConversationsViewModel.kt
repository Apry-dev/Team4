package com.example.esnmessenger.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.esnmessenger.model.ChatSummary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatListViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _chats = MutableStateFlow<List<ChatSummary>>(emptyList())
    val chats: StateFlow<List<ChatSummary>> = _chats.asStateFlow()

    init {
        listenForExistingChats()
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
                    _chats.value = emptyList()
                    return@addSnapshotListener
                }

                val chatList = mutableListOf<ChatSummary>()
                var remaining = docs.size

                for (doc in docs) {
                    val participants = doc.get("participants") as? List<String>
                    val otherUserId = participants?.firstOrNull { it != currentUserUid }

                    if (participants == null || otherUserId == null) {
                        remaining--
                        if (remaining == 0) _chats.value = chatList.sortedByDescending { it.timestamp }
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
                            chatList.add(
                                ChatSummary(
                                    chatId = doc.id,
                                    otherUserId = otherUserId,
                                    otherUserName = name,
                                    lastMessage = lastMessage,
                                    timestamp = timestamp,
                                    otherUserPhotoBase64 = userDoc.getString("photoBase64"),
                                    unreadCount = unreadCount
                                )
                            )
                            remaining--
                            if (remaining == 0) _chats.value = chatList.sortedByDescending { it.timestamp }
                        }
                        .addOnFailureListener {
                            chatList.add(
                                ChatSummary(
                                    chatId = doc.id,
                                    otherUserId = otherUserId,
                                    otherUserName = otherUserId,
                                    lastMessage = lastMessage,
                                    timestamp = timestamp,
                                    unreadCount = unreadCount
                                )
                            )
                            remaining--
                            if (remaining == 0) _chats.value = chatList.sortedByDescending { it.timestamp }
                        }
                }
            }
    }
}
