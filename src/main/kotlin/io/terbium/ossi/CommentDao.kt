package io.terbium.ossi

interface CommentDao {
    fun get(uri: String): List<Comment>

    fun getId(id: Long): Comment?

    fun getRecent(uri: String, limit: Int): List<Comment>

    fun new(comment: DaoNewComment, authorization: String): Comment

    fun edit(id: Long, comment: DaoEditComment, authorization: String): EditResponse

    fun delete(id: Long, authorization: String): Boolean

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

    sealed class EditResponse {
        class Ok(comment: Comment) : EditResponse()
        object NotFound : EditResponse()
        object NotAuthorized : EditResponse()
    }
}
