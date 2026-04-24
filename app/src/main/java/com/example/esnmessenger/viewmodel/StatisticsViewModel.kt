package com.example.esnmessenger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esnmessenger.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume

data class MessagingStats(
    val totalSent: Int = 0,
    val totalReceived: Int = 0,
    val totalConversations: Int = 0,
    val avgMessagesPerDay: Float = 0f,
    val longestStreak: Int = 0,
    val currentStreak: Int = 0,
    val mostActiveDayOfWeek: String = "",
    val mostActiveHour: Int = -1,
    val avgWordCount: Float = 0f,
    val totalWordsSent: Int = 0,
    val firstMessageDate: Date? = null,
    val mostChattedWith: List<Pair<String, Int>> = emptyList(),
    val last7DaysActivity: List<Pair<String, Int>> = emptyList(),
    val avgResponseTimeMinutes: Double? = null,
    val totalDaysActive: Int = 0
)

class StatisticsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _stats = MutableStateFlow(MessagingStats())
    val stats: StateFlow<MessagingStats> = _stats

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var sentListener: ListenerRegistration? = null
    private var receivedListener: ListenerRegistration? = null

    @Volatile private var allSentMessages: List<Message> = emptyList()
    @Volatile private var allReceivedMessages: List<Message> = emptyList()
    private val userNameCache = mutableMapOf<String, String>()

    init {
        startListening()
    }

    private fun startListening() {
        val uid = auth.currentUser?.uid ?: return

        sentListener = db.collectionGroup("messages")
            .whereEqualTo("fromId", uid)
            .addSnapshotListener { snapshot, err ->
                if (err != null) {
                    _error.value = "Stats require a Firestore index — check Logcat for setup link."
                    _isLoading.value = false
                    return@addSnapshotListener
                }
                allSentMessages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                triggerRecompute()
            }

        receivedListener = db.collectionGroup("messages")
            .whereEqualTo("toId", uid)
            .addSnapshotListener { snapshot, err ->
                if (err != null) {
                    _error.value = "Stats require a Firestore index — check Logcat for setup link."
                    _isLoading.value = false
                    return@addSnapshotListener
                }
                allReceivedMessages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                triggerRecompute()
            }
    }

    private fun triggerRecompute() {
        val uid = auth.currentUser?.uid ?: return
        val sent = allSentMessages.toList()
        val received = allReceivedMessages.toList()

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                computeStats(uid, sent, received)
            }
            userNameCache.putAll(result.second)
            _stats.value = result.first
            _isLoading.value = false
        }
    }

    private suspend fun computeStats(
        uid: String,
        sent: List<Message>,
        received: List<Message>
    ): Pair<MessagingStats, Map<String, String>> {

        val partnerIds = (sent.map { it.toId } + received.map { it.fromId })
            .filter { it.isNotEmpty() && it != uid }
            .toSet()

        val resolvedNames = resolveNames(partnerIds)

        val mostChattedWith = sent
            .groupBy { it.toId }
            .mapValues { it.value.size }
            .entries.sortedByDescending { it.value }
            .take(3)
            .map { (partnerId, count) -> Pair(resolvedNames[partnerId] ?: "Unknown", count) }

        val sentDates = sent.mapNotNull { it.timestamp?.toDate() }
        val firstDate = sentDates.minOrNull()
        val daysSinceFirst = if (firstDate != null)
            ((Date().time - firstDate.time) / 86_400_000L).coerceAtLeast(1L) else 1L
        val avgPerDay = if (sent.isEmpty()) 0f else sent.size.toFloat() / daysSinceFirst

        val dayFormat = SimpleDateFormat("EEE", Locale.ENGLISH)
        val dayOfWeekCounts = sentDates.groupBy { dayFormat.format(it) }.mapValues { it.value.size }
        val mostActiveDOW = dayOfWeekCounts.maxByOrNull { it.value }?.key ?: ""

        val hourCounts = sentDates.groupBy {
            Calendar.getInstance().apply { time = it }.get(Calendar.HOUR_OF_DAY)
        }.mapValues { it.value.size }
        val mostActiveHour = hourCounts.maxByOrNull { it.value }?.key ?: -1

        val wordCounts = sent.map { it.text.trim().split("\\s+".toRegex()).count { w -> w.isNotEmpty() } }
        val totalWords = wordCounts.sum()
        val avgWords = if (sent.isEmpty()) 0f else wordCounts.average().toFloat()

        val sentDayKeys = sentDates.map { date ->
            Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.toSet().sorted()

        val longestStreak = calcLongestStreak(sentDayKeys)
        val currentStreak = calcCurrentStreak(sentDayKeys)

        val last7Days = (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = cal.timeInMillis
            val label = when (daysAgo) {
                0 -> "Today"; 1 -> "Yest"
                else -> SimpleDateFormat("EEE", Locale.ENGLISH).format(Date(dayStart))
            }
            val count = sent.count { msg ->
                val t = msg.timestamp?.toDate()?.time ?: return@count false
                t in dayStart until dayEnd
            }
            Pair(label, count)
        }

        val avgResponseTime = calcAvgResponseTime(sent, received)

        return Pair(
            MessagingStats(
                totalSent = sent.size,
                totalReceived = received.size,
                totalConversations = partnerIds.size,
                avgMessagesPerDay = avgPerDay,
                longestStreak = longestStreak,
                currentStreak = currentStreak,
                mostActiveDayOfWeek = mostActiveDOW,
                mostActiveHour = mostActiveHour,
                avgWordCount = avgWords,
                totalWordsSent = totalWords,
                firstMessageDate = firstDate,
                mostChattedWith = mostChattedWith,
                last7DaysActivity = last7Days,
                avgResponseTimeMinutes = avgResponseTime,
                totalDaysActive = sentDayKeys.size
            ),
            resolvedNames
        )
    }

    private suspend fun resolveNames(uids: Set<String>): Map<String, String> =
        withContext(Dispatchers.IO) {
            val result = mutableMapOf<String, String>()
            for (uid in uids) {
                result[uid] = userNameCache[uid] ?: fetchUserName(uid)
            }
            result
        }

    private suspend fun fetchUserName(uid: String): String =
        suspendCancellableCoroutine { cont ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc -> cont.resume(doc.getString("name") ?: "Unknown") }
                .addOnFailureListener { cont.resume("Unknown") }
        }

    private fun calcAvgResponseTime(sent: List<Message>, received: List<Message>): Double? {
        val times = mutableListOf<Long>()
        val receivedByPartner = received
            .filter { it.fromId.isNotEmpty() && it.timestamp != null }
            .groupBy { it.fromId }

        for ((partnerId, msgs) in receivedByPartner) {
            val sentToPartner = sent
                .filter { it.toId == partnerId && it.timestamp != null }
                .sortedBy { it.timestamp!!.toDate().time }
            for (msg in msgs.sortedBy { it.timestamp!!.toDate().time }) {
                val receivedAt = msg.timestamp!!.toDate().time
                val reply = sentToPartner.firstOrNull { it.timestamp!!.toDate().time > receivedAt } ?: continue
                val diff = reply.timestamp!!.toDate().time - receivedAt
                if (diff < 86_400_000L) times.add(diff)
            }
        }
        return if (times.isNotEmpty()) times.average() / 60_000.0 else null
    }

    private fun calcLongestStreak(days: List<Long>): Int {
        if (days.isEmpty()) return 0
        var max = 1; var cur = 1
        for (i in 1 until days.size) {
            if (days[i] - days[i - 1] <= 86_400_000L) { cur++; if (cur > max) max = cur } else cur = 1
        }
        return max
    }

    private fun calcCurrentStreak(days: List<Long>): Int {
        if (days.isEmpty()) return 0
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        if (days.last() < todayStart - 86_400_000L) return 0
        var streak = 1
        for (i in days.size - 2 downTo 0) {
            if (days[i + 1] - days[i] <= 86_400_000L) streak++ else break
        }
        return streak
    }

    override fun onCleared() {
        super.onCleared()
        sentListener?.remove()
        receivedListener?.remove()
    }
}
