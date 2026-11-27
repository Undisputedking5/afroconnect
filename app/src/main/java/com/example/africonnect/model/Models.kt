package com.example.africonnect.model

import com.google.firebase.database.DataSnapshot

object Roles {
    const val CLIENT = "CLIENT"
    const val FREELANCER = "FREELANCER"
}

object JobStatus {
    const val OPEN = "open"
    const val HIRED = "hired"
    const val COMPLETED = "completed"
    const val CLOSED = "closed"
}

val JobCategories = listOf(
    "Development", "Design", "Writing", "Marketing", "Data & AI",
    "Mobile", "Video & Audio", "Admin Support", "Finance", "Other"
)

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "",
    val title: String = "",
    val location: String = "",
    val about: String = "",
    val skills: List<String> = emptyList(),
    val hourlyRate: String = "",
    val experience: String = "",
    val github: String = "",
    val linkedin: String = "",
    val instagram: String = "",
    val website: String = "",
    val avatarUrl: String = "",
    val portfolioImages: List<String> = emptyList(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val showOnMap: Boolean = false,
    val workType: String = "Remote",
    val isAvailable: Boolean = true,
    val rating: Double = 0.0,
    val reviews: Int = 0,
    val completedJobs: Int = 0,
    val company: String = "",
    val createdAt: Long = 0L,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
) {
    val isClient get() = role == Roles.CLIENT
    val displayName get() = name.ifBlank { email.substringBefore("@").ifBlank { "AfroConnect user" } }
    val hasLocation get() = latitude != 0.0 || longitude != 0.0

    /** 0..1 — how complete the profile is, used to nudge users. */
    val completion: Float
        get() {
            val checks = if (isClient) listOf(
                name.isNotBlank(), avatarUrl.isNotBlank(), company.isNotBlank(),
                location.isNotBlank(), about.isNotBlank()
            ) else listOf(
                name.isNotBlank(), avatarUrl.isNotBlank(), title.isNotBlank(),
                location.isNotBlank(), about.isNotBlank(), skills.isNotEmpty(),
                hourlyRate.isNotBlank(), portfolioImages.isNotEmpty()
            )
            return checks.count { it } / checks.size.toFloat()
        }

    companion object {
        fun from(s: DataSnapshot): UserProfile = UserProfile(
            uid = s.key ?: "",
            name = s.str("name"),
            email = s.str("email"),
            phone = s.str("phone"),
            role = s.str("role").ifBlank { Roles.FREELANCER },
            title = s.str("title"),
            location = s.str("location"),
            about = s.str("about"),
            skills = s.strList("skills"),
            hourlyRate = s.str("hourlyRate"),
            experience = s.str("experience"),
            github = s.str("github"),
            linkedin = s.str("linkedin"),
            instagram = s.str("instagram"),
            website = s.str("website").ifBlank { s.str("dribbble") },
            avatarUrl = s.str("avatarUrl"),
            portfolioImages = s.strList("portfolioImages"),
            latitude = s.dbl("latitude"),
            longitude = s.dbl("longitude"),
            showOnMap = s.bool("showOnMap"),
            workType = s.str("workType").ifBlank { "Remote" },
            isAvailable = s.bool("isAvailable", true),
            rating = s.dbl("rating"),
            reviews = s.int("reviews"),
            completedJobs = s.int("completedJobs"),
            company = s.str("company"),
            createdAt = s.long("createdAt"),
            isOnline = s.bool("isOnline"),
            lastSeen = s.long("lastSeen"),
        )
    }
}

data class Job(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val skills: List<String> = emptyList(),
    val budget: String = "",
    val budgetAmount: Double = 0.0,
    val currency: String = "KES",
    val budgetType: String = "Fixed",
    val workType: String = "Remote",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isUrgent: Boolean = false,
    val postedBy: String = "",
    val postedByName: String = "",
    val postedAt: Long = 0L,
    val status: String = JobStatus.OPEN,
    val hiredFreelancerId: String = "",
    val hiredFreelancerName: String = "",
    val applicants: Set<String> = emptySet(),
    val deadline: String = "",
) {
    val isOpen get() = status == JobStatus.OPEN
    val hasLocation get() = latitude != 0.0 || longitude != 0.0
    val proposalCount get() = applicants.size

    companion object {
        fun from(s: DataSnapshot): Job = Job(
            id = s.key ?: s.str("id"),
            title = s.str("title"),
            description = s.str("description"),
            category = s.str("category"),
            skills = s.strList("skills"),
            budget = s.str("budget").ifBlank { s.str("rate") },
            budgetAmount = s.dbl("budgetAmount"),
            currency = s.str("currency").ifBlank { "KES" },
            budgetType = s.str("budgetType").ifBlank { "Fixed" },
            workType = s.str("workType").ifBlank { "Remote" },
            location = s.str("location").ifBlank { s.str("locationLabel") },
            latitude = s.dbl("latitude"),
            longitude = s.dbl("longitude"),
            // Old builds wrote "urgent" (Firebase bean naming) or "isUrgent".
            isUrgent = s.bool("isUrgent") || s.bool("urgent"),
            postedBy = s.str("postedBy"),
            postedByName = s.str("postedByName"),
            postedAt = s.long("postedAt"),
            status = s.str("status").ifBlank { JobStatus.OPEN },
            hiredFreelancerId = s.str("hiredFreelancerId"),
            hiredFreelancerName = s.str("hiredFreelancerName"),
            applicants = s.child("applicants").children.mapNotNull { it.key }.toSet(),
            deadline = s.str("deadline"),
        )
    }
}

data class Proposal(
    val jobId: String = "",
    val freelancerId: String = "",
    val freelancerName: String = "",
    val freelancerTitle: String = "",
    val freelancerAvatar: String = "",
    val coverLetter: String = "",
    val bid: String = "",
    val createdAt: Long = 0L,
    val status: String = "pending",
) {
    companion object {
        fun from(jobId: String, s: DataSnapshot) = Proposal(
            jobId = jobId,
            freelancerId = s.key ?: "",
            freelancerName = s.str("freelancerName"),
            freelancerTitle = s.str("freelancerTitle"),
            freelancerAvatar = s.str("freelancerAvatar"),
            coverLetter = s.str("coverLetter"),
            bid = s.str("bid"),
            createdAt = s.long("createdAt"),
            status = s.str("status").ifBlank { "pending" },
        )
    }
}

enum class MessageType { TEXT, IMAGE, SYSTEM }

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val type: MessageType = MessageType.TEXT,
    val mediaUrl: String = "",
    val isRead: Boolean = false,
) {
    companion object {
        fun from(s: DataSnapshot): ChatMessage {
            val sender = s.str("senderId")
            val rawType = s.str("type").ifBlank { "TEXT" }
            val type = when {
                sender == "system" -> MessageType.SYSTEM
                rawType == "IMAGE" -> MessageType.IMAGE
                else -> MessageType.TEXT
            }
            return ChatMessage(
                id = s.key ?: "",
                senderId = sender,
                text = s.str("text"),
                timestamp = s.long("timestamp"),
                type = type,
                mediaUrl = s.str("mediaUrl"),
                isRead = s.bool("isRead"),
            )
        }
    }
}

data class Conversation(
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserAvatar: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastSenderId: String = "",
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
) {
    companion object {
        fun from(s: DataSnapshot) = Conversation(
            otherUserId = s.str("otherUserId").ifBlank { s.key ?: "" },
            otherUserName = s.str("otherUserName"),
            otherUserAvatar = s.str("otherUserAvatar"),
            lastMessage = s.str("lastMessage"),
            lastMessageTime = s.long("lastMessageTime"),
            lastSenderId = s.str("lastSenderId"),
            unreadCount = s.int("unreadCount"),
        )
    }
}

data class Review(
    val id: String = "",
    val jobId: String = "",
    val jobTitle: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val createdAt: Long = 0L,
) {
    companion object {
        fun from(s: DataSnapshot) = Review(
            id = s.key ?: "",
            jobId = s.str("jobId"),
            jobTitle = s.str("jobTitle"),
            clientId = s.str("clientId"),
            clientName = s.str("clientName"),
            rating = s.int("rating").coerceIn(1, 5),
            comment = s.str("comment"),
            createdAt = s.long("createdAt"),
        )
    }
}

data class AppNotification(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val jobId: String = "",
    val fromUid: String = "",
    val createdAt: Long = 0L,
    val read: Boolean = false,
) {
    companion object {
        fun from(s: DataSnapshot) = AppNotification(
            id = s.key ?: "",
            type = s.str("type"),
            title = s.str("title"),
            body = s.str("body"),
            jobId = s.str("jobId"),
            fromUid = s.str("fromUid"),
            createdAt = s.long("createdAt"),
            read = s.bool("read"),
        )
    }
}

// ── Tolerant snapshot readers (old data stored numbers as strings etc.) ─────
fun DataSnapshot.str(key: String): String = child(key).value?.toString() ?: ""
fun DataSnapshot.long(key: String): Long = when (val v = child(key).value) {
    is Number -> v.toLong(); is String -> v.toLongOrNull() ?: 0L; else -> 0L
}
fun DataSnapshot.int(key: String): Int = long(key).toInt()
fun DataSnapshot.dbl(key: String): Double = when (val v = child(key).value) {
    is Number -> v.toDouble(); is String -> v.toDoubleOrNull() ?: 0.0; else -> 0.0
}
fun DataSnapshot.bool(key: String, default: Boolean = false): Boolean = when (val v = child(key).value) {
    is Boolean -> v; is String -> v.toBooleanStrictOrNull() ?: default; else -> default
}
fun DataSnapshot.strList(key: String): List<String> =
    child(key).children.mapNotNull { it.value?.toString()?.takeIf(String::isNotBlank) }
