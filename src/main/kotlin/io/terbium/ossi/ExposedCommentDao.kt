package io.terbium.ossi

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

class ExposedCommentDao(private val jdbcUrl: String): CommentDao {
    init {
        Database.connect(jdbcUrl)
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        transaction {
            SchemaUtils.create(Comments)
        }
    }

    override fun get(uri: String): List<Comment> {
        val comments = transaction {
            CommentMapper.find { Comments.uri eq uri }.toList()
        }
        return comments.map(CommentMapper::toComment)
    }

    override fun get_recent(uri: String, limit: Int): List<Comment> {
        TODO("Not yet implemented")
    }

    override fun new(comment: CommentDao.DaoNewComment): Comment {
        val created = transaction {
            CommentMapper.new {
                uri = comment.uri
                hash = comment.hash
                parent = comment.parent
                txt = comment.text
                author = comment.author
                website = comment.website
                created = comment.creationTime
                mode = comment.mode.code
            }
        }
        return created.toComment()
    }

    override fun edit(comment: CommentDao.DaoEditComment): Comment {
        TODO("Not yet implemented")
    }

    override fun delete(id: Long) {
        TODO("Not yet implemented")
    }

    object Comments: LongIdTable() {
        val uri = text("uri")
        val parent = long("parent").nullable()
        val txt = text("text")
        val mode = integer("mode")
        val hash = text("hash")
        val author = text("author").nullable()
        val website = text("website").nullable()
        val likes = integer("likes").default(0)
        val dislikes = integer("dislikes").default(0)
        val created = long("created")
        val modified = long("modified").nullable()
    }

    class CommentMapper(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<CommentMapper>(Comments)

        var uri by Comments.uri
        var parent by Comments.parent
        var txt by Comments.txt
        var mode by Comments.mode
        var hash by Comments.hash
        var author by Comments.author
        var website by Comments.website
        var likes by Comments.likes
        var dislikes by Comments.dislikes
        var created by Comments.created
        var modified by Comments.modified

        fun toComment(): Comment = Comment(
            id = id.value,
            parent = parent,
            text = txt,
            mode = CommentMode.fromInt(mode),
            hash = hash,
            author = author,
            website = website,
            likes = likes,
            dislikes = dislikes,
            created = created,
            modified = modified
        )
    }
}