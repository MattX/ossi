package io.terbium.ossi

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import java.lang.RuntimeException
import java.lang.reflect.Type

data class Comment(
    val id: Long,
    val parent: Long?,
    val text: String,
    val mode: CommentMode,
    val hash: String,
    val author: String?,
    val website: String?,
    val likes: Int,
    val dislikes: Int,
    val created: Long,
    val modified: Long?,
    val email: Void? = null
) {
    fun toThreadComment() = ThreadComment(
        id = id,
        parent = parent,
        text = text,
        mode = mode,
        hash = hash,
        author = author,
        website = website,
        likes = likes,
        dislikes = dislikes,
        created = created,
        modified = modified,
        replies = listOf(),
        totalReplies = 0
    )

    fun rendered() = copy(text = renderMarkdown(text))
}

enum class CommentMode(val code: Int) {
    ACCEPTED(1),
    IN_QUEUE(2),
    DELETED(4);

    companion object {
        private val map = values().associateBy(CommentMode::code)
        fun fromInt(code: Int): CommentMode = map[code] ?: error("invalid code $code")
    }

    object SerDe : JsonDeserializer<CommentMode>, JsonSerializer<CommentMode> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): CommentMode {
            return if (json is JsonPrimitive && json.isNumber) {
                fromInt(json.asInt)
            } else {
                throw RuntimeException("invalid serialized CommentMode: null")
            }
        }

        override fun serialize(
            src: CommentMode?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement {
            return when(src) {
                null -> JsonNull.INSTANCE
                else -> JsonPrimitive(src.code)
            }
        }

    }
}

data class CommentThread(
    val replies: List<ThreadComment>,
    @SerializedName("total_replies")
    val totalReplies: Int,
    val id: Void?,
    @SerializedName("hidden_replies")
    val hiddenReplies: Int
)

data class ThreadComment(
    val id: Long,
    val parent: Long?,
    val text: String,
    val mode: CommentMode,
    val hash: String,
    val author: String?,
    val website: String?,
    val likes: Int,
    val dislikes: Int,
    val created: Long,
    val modified: Long?,
    var replies: List<ThreadComment>,
    @SerializedName("total_replies")
    var totalReplies: Int,
    @SerializedName("hidden_replies")
    val hiddenReplies: Int = 0
)

data class NewCommentRequest(
    val parent: Long?,
    val text: String,
    val author: String?,
    val website: String?,
    val email: String?
) {
    fun toDaoNewComment(uri: String, time: Long, ip: String) = CommentDao.DaoNewComment(
        uri = uri,
        parent = parent,
        text = text,
        author = author,
        website = website,
        creationTime = time,
        hash = pbkdf2(email ?: ip),
        mode = CommentMode.ACCEPTED
    )
}

data class EditCommentRequest(
    val text: String,
    val author: String?,
    val website: String?
) {
    fun toDaoEditComment(time: Long) = CommentDao.DaoEditComment(
        text = text,
        author = author,
        website = website,
        modificationTime = time
    )
}

data class PreviewRequest(val text: String)

data class PreviewResponse(val text: String)

data class LikeResponse(val likes: Int, val dislikes: Int)
