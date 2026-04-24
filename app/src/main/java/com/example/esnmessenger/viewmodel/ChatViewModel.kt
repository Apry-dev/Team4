package com.example.esnmessenger.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.esnmessenger.model.Message
import com.example.esnmessenger.network.CloudinaryUploader
import com.example.esnmessenger.util.AudioRecorder
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

sealed class RecordingState {
    object Idle : RecordingState()
    data class Recording(val startTimeMs: Long) : RecordingState()
    object Uploading : RecordingState()
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val audioRecorder = AudioRecorder(application)

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _otherUserName = MutableStateFlow<String?>(null)
    val otherUserName: StateFlow<String?> = _otherUserName

    private val _otherUserPhotoBase64 = MutableStateFlow<String?>(null)
    val otherUserPhotoBase64: StateFlow<String?> = _otherUserPhotoBase64

    private val _otherUserIsTyping = MutableStateFlow(false)
    val otherUserIsTyping: StateFlow<Boolean> = _otherUserIsTyping

    private val _otherUserLastSeen = MutableStateFlow(0L)
    val otherUserLastSeen: StateFlow<Long> = _otherUserLastSeen

    private val _isBlocked = MutableStateFlow(false)
    val isBlocked: StateFlow<Boolean> = _isBlocked

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

        db.collection("blocks").document(currentUid).get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val blocked = doc.get("uids") as? List<String> ?: emptyList()
                _isBlocked.value = blocked.contains(otherUserId)
            }

        db.collection("users").document(currentUid)
            .set(mapOf("lastSeen" to System.currentTimeMillis()), SetOptions.merge())

        typingListenerRegistration?.remove()
        typingListenerRegistration = db.collection("users").document(otherUserId)
            .addSnapshotListener { snapshot, _ ->
                val typingTo = snapshot?.getString("isTypingTo")
                _otherUserIsTyping.value = typingTo == currentUid
                _otherUserLastSeen.value = snapshot?.getLong("lastSeen") ?: 0L
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

    fun sendMessage(otherUserId: String, text: String, imageUrl: String? = null) {
        val currentUid = auth.currentUser?.uid ?: return
        val trimmed = text.trim()
        if (trimmed.isBlank() && imageUrl == null) return

        val cid = chatId(currentUid, otherUserId)
        clearTypingState()
        typingJob?.cancel()

        val lastMessagePreview = if (trimmed.isNotBlank()) trimmed else "[Photo]"

        db.collection("chats").document(cid)
            .set(
                mapOf(
                    "participants" to listOf(currentUid, otherUserId),
                    "lastMessage" to lastMessagePreview,
                    "timestamp" to System.currentTimeMillis(),
                    "unreadCount_$otherUserId" to FieldValue.increment(1)
                ),
                SetOptions.merge()
            )

        db.collection("chats")
            .document(cid)
            .collection("messages")
            .add(Message(fromId = currentUid, toId = otherUserId, text = trimmed, imageUrl = imageUrl))
            .addOnFailureListener { e -> _error.value = e.message }
    }

    fun deleteMessage(messageId: String, otherUserId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val cid = chatId(currentUid, otherUserId)
        db.collection("chats").document(cid)
            .collection("messages").document(messageId)
            .update("deleted", true, "text", "")
    }

    fun editMessage(messageId: String, otherUserId: String, newText: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val cid = chatId(currentUid, otherUserId)
        db.collection("chats").document(cid)
            .collection("messages").document(messageId)
            .update("text", newText.trim(), "edited", true)
    }

    fun blockUser(otherUserId: String) {
        val myUid = auth.currentUser?.uid ?: return
        db.collection("blocks").document(myUid)
            .set(mapOf("uids" to FieldValue.arrayUnion(otherUserId)), SetOptions.merge())
    }

    fun reportUser(otherUserId: String, reason: String) {
        val myUid = auth.currentUser?.uid ?: return
        db.collection("reports").add(mapOf(
            "reporterId" to myUid,
            "targetId" to otherUserId,
            "reason" to reason,
            "timestamp" to System.currentTimeMillis()
        ))
    }

    fun sendMessageWithImage(otherUserId: String, text: String, imageUri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            _error.value = null
            try {
                val url = CloudinaryUploader.upload(getApplication(), imageUri)
                sendMessage(otherUserId, text, url)
            } catch (e: Exception) {
                _error.value = "Upload failed: ${e.message}"
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun startRecording() {
        if (_recordingState.value !is RecordingState.Idle) return
        audioRecorder.start()
        _recordingState.value = RecordingState.Recording(System.currentTimeMillis())
    }

    fun stopAndSendRecording(otherUserId: String, durationMs: Long) {
        val currentUid = auth.currentUser?.uid ?: return
        val file = audioRecorder.stop() ?: run {
            _recordingState.value = RecordingState.Idle
            return
        }
        val cid = chatId(currentUid, otherUserId)

        _recordingState.value = RecordingState.Uploading
        viewModelScope.launch {
            _error.value = null
            try {
                val url = CloudinaryUploader.uploadAudio(file)
                file.delete()
                db.collection("chats").document(cid)
                    .set(
                        mapOf(
                            "participants" to listOf(currentUid, otherUserId),
                            "lastMessage" to "[Voice note]",
                            "timestamp" to System.currentTimeMillis(),
                            "unreadCount_$otherUserId" to FieldValue.increment(1)
                        ),
                        SetOptions.merge()
                    )
                db.collection("chats").document(cid).collection("messages")
                    .add(Message(fromId = currentUid, toId = otherUserId, audioUrl = url, audioDurationMs = durationMs))
                    .addOnFailureListener { e -> _error.value = e.message }
            } catch (e: Exception) {
                _error.value = "Upload failed: ${e.message}"
                file.delete()
            } finally {
                _recordingState.value = RecordingState.Idle
            }
        }
    }

    fun cancelRecording() {
        audioRecorder.cancel()
        _recordingState.value = RecordingState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
        typingListenerRegistration?.remove()
        typingJob?.cancel()
        clearTypingState()
        audioRecorder.cancel()
    }
}
