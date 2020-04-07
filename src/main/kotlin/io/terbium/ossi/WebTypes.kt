package io.terbium.ossi

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
    val modified: Long?
)

enum class CommentMode(val code: Int) {
    ACCEPTED(1),
    IN_QUEUE(2),
    DELETED(4);

    companion object {
        private val map = values().associateBy(CommentMode::code)
        fun fromInt(code: Int): CommentMode = map[code] ?: error("invalid code $code")
    }
}

data class NewComment(
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

data class EditComment(
    val text: String,
    val author: String?,
    val website: String?
) {
    fun toDaoEditComment(uri: String, time: Long) = CommentDao.DaoEditComment(
        text = text,
        author = author,
        website = website,
        modificationTime = time
    )
}
