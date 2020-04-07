package io.terbium.ossi

interface CommentDao {
    fun get(uri: String): List<Comment>

    fun get_recent(uri: String, limit: Int): List<Comment>

    fun new(uri: String, comment: NewComment, hash: String): Long

    fun edit(uri: String, comment: EditComment): Comment

    fun delete(uri: String, id: Long)
}
