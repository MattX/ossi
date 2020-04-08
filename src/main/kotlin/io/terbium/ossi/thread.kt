package io.terbium.ossi

fun makeThread(comments: List<Comment>): CommentThread {
    val threadComments = comments.map { it.rendered().toThreadComment() }
    val byParent = threadComments.groupBy { it.parent }
    val byId = threadComments.associateBy { it.id }
    for ((parentId, comments) in byParent) {
        if (parentId != null) {
            val parent = byId.getValue(parentId)
            parent.replies = comments
            parent.totalReplies = comments.size
        }
    }
    return CommentThread(
        replies = byParent.getOrDefault(null, listOf()),
        totalReplies = comments.size,
        id = null,
        hiddenReplies = 0
    )
}
