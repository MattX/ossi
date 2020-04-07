package io.terbium.ossi

interface CommentDao {
    fun get(uri: String): List<Comment>

    fun get_recent(uri: String, limit: Int): List<Comment>

    fun new(comment: DaoNewComment): Comment

    fun edit(comment: DaoEditComment): Comment

    fun delete(id: Long)

    data class DaoNewComment(
        val uri: String,
        val parent: Long?,
        val text: String,
        val author: String?,
        val website: String?,
        val hash: String,
        val creationTime: Long,
        val mode: CommentMode
    )

    data class DaoEditComment(
        val text: String,
        val author: String?,
        val website: String?,
        val modificationTime: Long
    )
}
