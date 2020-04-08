package io.terbium.ossi

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.RuntimeException
import java.sql.Connection
import java.util.*

class ExposedCommentDao(jdbcUrl: String): CommentDao {
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

    override fun getId(id: Long): Comment? = transaction {
        CommentMapper.findById(id)?.toComment()
    }

    override fun getRecent(uri: String, limit: Int): List<Comment> {
        TODO("Not yet implemented")
    }

    override fun new(comment: CommentDao.DaoNewComment, authorization: String): Comment {
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
                this.authorization = authorization
            }
        }
        return created.toComment()
    }

    override fun edit(id: Long, comment: CommentDao.DaoEditComment, authorization: String): CommentDao.EditResponse {
        return transaction {
            val existing = CommentMapper.findById(id) ?: return@transaction CommentDao.EditResponse.NotFound
            if (existing.authorization != authorization) return@transaction CommentDao.EditResponse.NotAuthorized
            existing.txt = comment.text
            existing.modified = comment.modificationTime
            if (comment.author != null) existing.author = comment.author
            if (comment.website != null) existing.website = comment.website
            CommentDao.EditResponse.Ok(existing.toComment())
        }
    }

    override fun delete(id: Long, authorization: String): CommentDao.DeleteResponse = transaction {
        val existing = CommentMapper.findById(id) ?: return@transaction CommentDao.DeleteResponse.NotFound
        if (existing.authorization == authorization) {
            existing.delete()
            CommentDao.DeleteResponse.Ok
        } else {
            CommentDao.DeleteResponse.NotAuthorized
        }
    }

    override fun vote(id: Long, clientId: String, upvote: Boolean): CommentDao.VoteResponse? {
        if (clientId.contains(',')) throw RuntimeException("client id may not contain commas: $clientId")
        return transaction {
            val existing = CommentMapper.findById(id) ?: return@transaction null
            val voterBloomFilter = VoterBloomFilter(b64dec.decode(existing.voters))
            val canSave = voterBloomFilter.put(clientId)
            if (canSave) {
                if (upvote) existing.likes += 1
                else existing.dislikes += 1
                existing.voters = b64enc.encodeToString(voterBloomFilter.save())
            }
            CommentDao.VoteResponse(existing.likes, existing.dislikes, canSave)
        }
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
        val authorization = text("authorization")
        val voters = text("voters").default(b64enc.encodeToString(VoterBloomFilter.new().save()))
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
        var authorization by Comments.authorization
        var voters by Comments.voters

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

    companion object {
        val b64dec = Base64.getDecoder()
        val b64enc = Base64.getEncoder()
    }
}