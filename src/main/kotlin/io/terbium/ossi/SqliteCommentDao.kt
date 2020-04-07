package io.terbium.ossi

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import java.sql.DriverManager
import java.sql.ResultSet

class SqliteCommentDao(sqliteAddress: String): CommentDao {
    private val connectionAddress = "jdbc:sqlite:$sqliteAddress"

    init {
        val connection = DriverManager.getConnection(connectionAddress)
        val statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS comments (" +
                "id INTEGER PRIMARY KEY," +
                "uri TEXT NOT NULL," +
                "parent INTEGER," +
                "text TEXT NOT NULL," +
                "mode INTEGER NOT NULL," +
                "hash TEXT NOT NULL," +
                "author TEXT," +
                "website TEXT," +
                "likes INTEGER DEFAULT 0," +
                "dislikes INTEGER DEFAULT 0," +
                "created INTEGER NOT NULL," +
                "modified INTEGER)")
    }

    override fun get(uri: String): List<Comment> {
        val connection = DriverManager.getConnection(connectionAddress)
        val prepared = connection.prepareStatement("SELECT id, uri, parent, text, mode, hash, " +
                "author, website, likes, dislikes, created, modified FROM comments WHERE uri = ?1")
        prepared.setString(1, uri)
        return prepared.executeQuery().use { rs ->
            sequence {
                while (rs.next()) {
                    yield(Comment(
                        id = rs.getLong("id"),
                        uri = rs.getString("uri"),
                        parent = rs.getNullableLong("parent"),
                        text = rs.getString("text"),
                        mode = CommentMode.fromInt(rs.getInt("mode")),
                        hash = rs.getString("hash"),
                        author = rs.getNullableString("author"),
                        website = rs.getNullableString("website"),
                        likes = rs.getInt("likes"),
                        dislikes = rs.getInt("dislikes"),
                        created = rs.getLong("created"),
                        modified = rs.getNullableLong("modified")
                    ))
                }
            }.toList()
        }
    }

    override fun get_recent(uri: String, limit: Int): List<Comment> {
        TODO("Not yet implemented")
    }

    override fun new(uri: String, comment: NewComment, hash: String): Long {
        val connection = DriverManager.getConnection(connectionAddress)
        val prepared = connection.prepareStatement("INSERT INTO " +
                "comments(uri, parent, text, mode, hash, author, website, created) " +
                "VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)")
        prepared.setString(1, uri)
        prepared.setObject(2, comment.parent.orNull())
        prepared.setString(3, comment.text)
        prepared.setString(4, )
    }

    override fun edit(uri: String, comment: EditComment): Comment {
        TODO("Not yet implemented")
    }

    override fun delete(uri: String, id: Long) {
        TODO("Not yet implemented")
    }

    companion object {
        fun ResultSet.getNullableLong(fieldName: String): Option<Long> {
            val tempVal = getLong(fieldName)
            return if (wasNull()) {
                None
            } else {
                Some(tempVal)
            }
        }

        fun ResultSet.getNullableString(fieldName: String): Option<String> {
            val tempVal = getString(fieldName)
            return if (wasNull()) {
                None
            } else {
                Some(tempVal)
            }
        }
    }
}
