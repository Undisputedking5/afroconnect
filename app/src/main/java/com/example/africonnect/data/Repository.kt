package com.example.africonnect.data

import android.net.Uri
import com.example.africonnect.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

/** Emits the snapshot at [query] every time it changes. */
fun Query.observe(): Flow<DataSnapshot> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) { trySend(snapshot) }
        override fun onCancelled(error: DatabaseError) { close(error.toException()) }
    }
    addValueEventListener(listener)
    awaitClose { removeEventListener(listener) }
}

/** Atomically adds [delta] to a numeric node. */
suspend fun DatabaseReference.increment(delta: Long = 1) = suspendCancellableCoroutine { cont ->
    runTransaction(object : Transaction.Handler {
        override fun doTransaction(data: MutableData): Transaction.Result {
            val current = (data.value as? Number)?.toLong() ?: 0L
            data.value = (current + delta).coerceAtLeast(0)
            return Transaction.success(data)
        }
        override fun onComplete(e: DatabaseError?, committed: Boolean, s: DataSnapshot?) {
            if (cont.isActive) cont.resume(Unit)
        }
    })
}

/**
 * Single access point for Firebase. Schema:
 *  users/{uid}                       profile
 *  jobs/{jobId}                      job, with applicants/{uid}: true
 *  proposals/{jobId}/{uid}           proposal (cover letter, bid)
 *  userApplications/{uid}/{jobId}    applied timestamp
 *  savedJobs/{uid}/{jobId}           true
 *  chats/{chatId}/messages/{id}      chat messages
 *  conversations/{uid}/{otherUid}    inbox entry
 *  reviews/{freelancerUid}/{id}      review
 *  notifications/{uid}/{id}          in-app notification
 */
object Repo {
    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    /** Set once at startup (an emulator-bound instance in CI test builds). */
    var database: FirebaseDatabase? = null
    private val fdb: FirebaseDatabase get() = database ?: FirebaseDatabase.getInstance()
    val db: DatabaseReference get() = fdb.reference
    private val storage get() = FirebaseStorage.getInstance().reference

    val uid: String get() = auth.currentUser?.uid ?: ""

    // ── Users ────────────────────────────────────────────────────────────────
    fun observeUser(uid: String): Flow<UserProfile?> =
        if (uid.isBlank()) flowOf(null)
        else callbackFlow {
            val ref = db.child("users").child(uid)
            val l = object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    trySend(if (s.exists()) UserProfile.from(s) else null)
                }
                override fun onCancelled(error: DatabaseError) { close(error.toException()) }
            }
            ref.addValueEventListener(l)
            awaitClose { ref.removeEventListener(l) }
        }

    suspend fun getUser(uid: String): UserProfile? {
        val s = db.child("users").child(uid).get().await()
        return if (s.exists()) UserProfile.from(s) else null
    }

    fun observeFreelancers(): Flow<DataSnapshot> =
        db.child("users").orderByChild("role").equalTo(Roles.FREELANCER).observe()

    suspend fun updateUser(fields: Map<String, Any?>) {
        db.child("users").child(uid).updateChildren(fields).await()
    }

    suspend fun uploadImage(path: String, uri: Uri): String {
        val ref = storage.child(path)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun deleteByUrl(url: String) {
        runCatching { FirebaseStorage.getInstance().getReferenceFromUrl(url).delete().await() }
    }

    private var presenceListener: ValueEventListener? = null
    private val connectedRef get() = fdb.getReference(".info/connected")

    fun setupPresence() {
        val me = uid.ifBlank { return }
        presenceListener?.let { connectedRef.removeEventListener(it) }
        val online = db.child("users").child(me).child("isOnline")
        val lastSeen = db.child("users").child(me).child("lastSeen")
        presenceListener = connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                if (s.getValue(Boolean::class.java) == true && auth.currentUser?.uid == me) {
                    online.onDisconnect().setValue(false)
                    lastSeen.onDisconnect().setValue(ServerValue.TIMESTAMP)
                    online.setValue(true)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun goOffline() {
        presenceListener?.let { connectedRef.removeEventListener(it) }
        presenceListener = null
        val me = uid.ifBlank { return }
        db.child("users").child(me).child("isOnline").onDisconnect().cancel()
        db.child("users").child(me).updateChildren(
            mapOf("isOnline" to false, "lastSeen" to ServerValue.TIMESTAMP)
        )
    }

    // ── Jobs ─────────────────────────────────────────────────────────────────
    fun observeAllJobs(): Flow<DataSnapshot> = db.child("jobs").orderByChild("postedAt").observe()

    fun observeJobsBy(clientUid: String): Flow<DataSnapshot> =
        db.child("jobs").orderByChild("postedBy").equalTo(clientUid).observe()

    fun observeJob(jobId: String): Flow<DataSnapshot> = db.child("jobs").child(jobId).observe()

    suspend fun postJob(job: Map<String, Any?>): String {
        val ref = db.child("jobs").push()
        val id = ref.key!!
        ref.setValue(job + mapOf("id" to id, "postedBy" to uid, "postedAt" to ServerValue.TIMESTAMP)).await()
        return id
    }

    suspend fun updateJob(jobId: String, fields: Map<String, Any?>) {
        db.child("jobs").child(jobId).updateChildren(fields).await()
    }

    suspend fun deleteJob(job: Job) {
        val updates = mutableMapOf<String, Any?>(
            "jobs/${job.id}" to null,
            "proposals/${job.id}" to null,
        )
        job.applicants.forEach { updates["userApplications/$it/${job.id}"] = null }
        db.updateChildren(updates).await()
    }

    // ── Proposals ────────────────────────────────────────────────────────────
    fun observeProposals(jobId: String): Flow<DataSnapshot> = db.child("proposals").child(jobId).observe()

    fun observeMyApplications(): Flow<DataSnapshot> = db.child("userApplications").child(uid).observe()

    suspend fun applyToJob(job: Job, me: UserProfile, coverLetter: String, bid: String) {
        val updates = mapOf(
            "proposals/${job.id}/${me.uid}" to mapOf(
                "freelancerName" to me.displayName,
                "freelancerTitle" to me.title,
                "freelancerAvatar" to me.avatarUrl,
                "coverLetter" to coverLetter,
                "bid" to bid,
                "createdAt" to ServerValue.TIMESTAMP,
                "status" to "pending",
            ),
            "jobs/${job.id}/applicants/${me.uid}" to true,
            "userApplications/${me.uid}/${job.id}" to ServerValue.TIMESTAMP,
        )
        db.updateChildren(updates).await()
        notify(
            to = job.postedBy, type = "proposal",
            title = "New proposal",
            body = "${me.displayName} applied to \"${job.title}\"",
            jobId = job.id,
        )
    }

    suspend fun withdrawApplication(jobId: String) {
        db.updateChildren(
            mapOf(
                "proposals/$jobId/$uid" to null,
                "jobs/$jobId/applicants/$uid" to null,
                "userApplications/$uid/$jobId" to null,
            )
        ).await()
    }

    suspend fun hire(job: Job, proposal: Proposal, me: UserProfile) {
        db.updateChildren(
            mapOf(
                "jobs/${job.id}/status" to JobStatus.HIRED,
                "jobs/${job.id}/hiredFreelancerId" to proposal.freelancerId,
                "jobs/${job.id}/hiredFreelancerName" to proposal.freelancerName,
                "proposals/${job.id}/${proposal.freelancerId}/status" to "hired",
            )
        ).await()
        sendSystemMessage(
            otherUid = proposal.freelancerId,
            otherName = proposal.freelancerName,
            otherAvatar = proposal.freelancerAvatar,
            me = me,
            text = "🎉 ${me.displayName} hired you for \"${job.title}\". Say hello and agree on the next steps!"
        )
        notify(
            to = proposal.freelancerId, type = "hired",
            title = "You've been hired! 🎉",
            body = "You were hired for \"${job.title}\"",
            jobId = job.id,
        )
    }

    suspend fun completeJob(job: Job, rating: Int, comment: String, me: UserProfile) {
        val freelancer = job.hiredFreelancerId
        val reviewRef = db.child("reviews").child(freelancer).push()
        reviewRef.setValue(
            mapOf(
                "jobId" to job.id, "jobTitle" to job.title,
                "clientId" to me.uid, "clientName" to me.displayName,
                "rating" to rating, "comment" to comment,
                "createdAt" to ServerValue.TIMESTAMP,
            )
        ).await()
        db.child("jobs").child(job.id).child("status").setValue(JobStatus.COMPLETED).await()
        recomputeRating(freelancer)
        notify(
            to = freelancer, type = "review",
            title = "New ${rating}★ review",
            body = "${me.displayName} completed \"${job.title}\" and left you a review",
            jobId = job.id,
        )
    }

    private suspend fun recomputeRating(freelancerUid: String) {
        val all = db.child("reviews").child(freelancerUid).get().await().children.map { Review.from(it) }
        if (all.isEmpty()) return
        db.child("users").child(freelancerUid).updateChildren(
            mapOf(
                "rating" to (all.sumOf { it.rating }.toDouble() / all.size),
                "reviews" to all.size,
                "completedJobs" to all.map { it.jobId }.distinct().size,
            )
        ).await()
    }

    fun observeReviews(freelancerUid: String): Flow<DataSnapshot> =
        db.child("reviews").child(freelancerUid).orderByChild("createdAt").observe()

    // ── Saved jobs ───────────────────────────────────────────────────────────
    fun observeSaved(): Flow<DataSnapshot> = db.child("savedJobs").child(uid).observe()

    fun toggleSaved(jobId: String, saved: Boolean) {
        val ref = db.child("savedJobs").child(uid).child(jobId)
        if (saved) ref.removeValue() else ref.setValue(true)
    }

    // ── Chat ─────────────────────────────────────────────────────────────────
    fun chatId(a: String, b: String) = listOf(a, b).sorted().joinToString("_")

    fun observeMessages(otherUid: String): Flow<DataSnapshot> =
        db.child("chats").child(chatId(uid, otherUid)).child("messages")
            .orderByChild("timestamp").limitToLast(300).observe()

    fun observeConversations(): Flow<DataSnapshot> = db.child("conversations").child(uid).observe()

    suspend fun sendMessage(
        other: UserProfile,
        me: UserProfile,
        text: String,
        type: MessageType = MessageType.TEXT,
        mediaUrl: String = "",
    ) {
        val cid = chatId(me.uid, other.uid)
        val msgRef = db.child("chats").child(cid).child("messages").push()
        msgRef.setValue(
            mapOf(
                "id" to msgRef.key, "senderId" to me.uid, "text" to text,
                "timestamp" to ServerValue.TIMESTAMP, "type" to type.name,
                "mediaUrl" to mediaUrl, "isRead" to false,
            )
        ).await()
        val preview = if (type == MessageType.IMAGE) "📷 Photo" else text
        writeConversations(me, other.uid, other.displayName, other.avatarUrl, preview, me.uid)
    }

    private suspend fun sendSystemMessage(
        otherUid: String, otherName: String, otherAvatar: String, me: UserProfile, text: String
    ) {
        val cid = chatId(me.uid, otherUid)
        val msgRef = db.child("chats").child(cid).child("messages").push()
        msgRef.setValue(
            mapOf(
                "id" to msgRef.key, "senderId" to "system", "text" to text,
                "timestamp" to ServerValue.TIMESTAMP, "type" to "TEXT", "isRead" to false,
            )
        ).await()
        writeConversations(me, otherUid, otherName, otherAvatar, text, "system")
    }

    private suspend fun writeConversations(
        me: UserProfile, otherUid: String, otherName: String, otherAvatar: String,
        preview: String, senderId: String,
    ) {
        val now = ServerValue.TIMESTAMP
        db.updateChildren(
            mapOf(
                "conversations/${me.uid}/$otherUid/otherUserId" to otherUid,
                "conversations/${me.uid}/$otherUid/otherUserName" to otherName,
                "conversations/${me.uid}/$otherUid/otherUserAvatar" to otherAvatar,
                "conversations/${me.uid}/$otherUid/lastMessage" to preview,
                "conversations/${me.uid}/$otherUid/lastMessageTime" to now,
                "conversations/${me.uid}/$otherUid/lastSenderId" to senderId,
                "conversations/$otherUid/${me.uid}/otherUserId" to me.uid,
                "conversations/$otherUid/${me.uid}/otherUserName" to me.displayName,
                "conversations/$otherUid/${me.uid}/otherUserAvatar" to me.avatarUrl,
                "conversations/$otherUid/${me.uid}/lastMessage" to preview,
                "conversations/$otherUid/${me.uid}/lastMessageTime" to now,
                "conversations/$otherUid/${me.uid}/lastSenderId" to senderId,
            )
        ).await()
        db.child("conversations").child(otherUid).child(me.uid).child("unreadCount").increment()
    }

    fun markChatRead(otherUid: String, messages: List<ChatMessage>) {
        val me = uid.ifBlank { return }
        db.child("conversations").child(me).child(otherUid).child("unreadCount").setValue(0)
        val cid = chatId(me, otherUid)
        val updates = messages.filter { !it.isRead && it.senderId != me && it.senderId != "system" }
            .associate { "chats/$cid/messages/${it.id}/isRead" to true }
        if (updates.isNotEmpty()) db.updateChildren(updates)
    }

    fun deleteConversation(otherUid: String) {
        db.child("conversations").child(uid).child(otherUid).removeValue()
    }

    // ── Notifications ────────────────────────────────────────────────────────
    fun observeNotifications(): Flow<DataSnapshot> =
        db.child("notifications").child(uid).orderByChild("createdAt").limitToLast(100).observe()

    suspend fun notify(to: String, type: String, title: String, body: String, jobId: String = "") {
        if (to.isBlank() || to == uid) return
        runCatching {
            db.child("notifications").child(to).push().setValue(
                mapOf(
                    "type" to type, "title" to title, "body" to body,
                    "jobId" to jobId, "fromUid" to uid,
                    "createdAt" to ServerValue.TIMESTAMP, "read" to false,
                )
            ).await()
        }
    }

    fun markNotificationsRead(ids: List<String>) {
        if (ids.isEmpty()) return
        db.updateChildren(ids.associate { "notifications/$uid/$it/read" to true })
    }

    fun clearNotifications() { db.child("notifications").child(uid).removeValue() }

    // ── Account ──────────────────────────────────────────────────────────────
    /** Re-authenticates with [password] (Firebase requires a recent login), wipes data, deletes the user. */
    suspend fun deleteAccount(password: String) {
        val me = uid
        val user = auth.currentUser ?: return
        val email = user.email ?: error("No email on this account")
        user.reauthenticate(com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)).await()
        db.updateChildren(
            mapOf(
                "users/$me" to null, "conversations/$me" to null,
                "notifications/$me" to null, "savedJobs/$me" to null,
                "userApplications/$me" to null,
            )
        ).await()
        user.delete().await()
    }
}
