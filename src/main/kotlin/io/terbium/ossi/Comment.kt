package io.terbium.ossi

import arrow.core.Option
import arrow.optics.optics

@optics
data class Comment(
    val id: Long,
    val uri: String,
    val parent: Option<Long>,
    val text: String,
    val mode: CommentMode,
    val hash: String,
    val author: Option<String>,
    val website: Option<String>,
    val likes: Int,
    val dislikes: Int,
    val created: Long,
    val modified: Option<Long>
) {
    companion object
}

enum class CommentMode(val code: Int) {
    ACCEPTED(1),
    IN_QUEUE(2),
    DELETED(4);

    companion object {
        private val map = values().associateBy(CommentMode::code)
        fun fromInt(code: Int): CommentMode = map[code] ?: error("invalid code $code")
    }
}

@optics
data class NewComment(
    val parent: Option<Long>,
    val text: String,
    val author: Option<String>,
    val website: Option<String>,
    val email: String
) {
    companion object
}

@optics
data class EditComment(
    val text: String,
    val author: Option<String>,
    val website: Option<String>
) {
    companion object
}
