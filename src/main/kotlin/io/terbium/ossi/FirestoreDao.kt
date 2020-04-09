// Copyright 2020 Matthieu Felix
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.terbium.ossi

import com.google.cloud.datastore.*
import java.util.*

class FirestoreDao(projectName: String, private val kind: String) : CommentDao {
    private val datastore = DatastoreOptions.getDefaultInstance().toBuilder()
        .setProjectId(projectName)
        .build()
        .service

    override fun get(uri: String): List<Comment> {
        val query = Query.newEntityQueryBuilder()
            .setKind(kind)
            .setFilter(StructuredQuery.PropertyFilter.eq("uri", uri))
            .build()
        val comments = datastore.run(query)
        return comments.iterator().asSequence().map { it.toComment() }.toList()
    }

    override fun getId(id: Long): Comment? {
        return datastore.get(datastore.newKeyFactory().setKind(kind).newKey(id))?.toComment()
    }

    override fun new(comment: CommentDao.DaoNewComment, authorization: String): Comment {
        val key = datastore.newKeyFactory().setKind(kind).newKey()
        val commentEntity = Entity.newBuilder(key)
            .set("uri", comment.uri)
            .setNullable("parent", comment.parent)
            .set("text", StringValue.newBuilder(comment.text).setExcludeFromIndexes(true).build())
            .set("mode", comment.mode.code.toLong())
            .set("hash", comment.hash)
            .setNullable("author", comment.author)
            .setNullable("website", comment.website)
            .set("likes", 0L)
            .set("dislikes", 0L)
            .set("created", comment.creationTime)
            .setNull("modified")
            .set("authorization", authorization)
            .set("voters", StringValue.newBuilder(b64enc.encodeToString(VoterBloomFilter.new().save()))
                .setExcludeFromIndexes(true)
                .build())
            .build()
        val createdEntity = datastore.put(commentEntity)
        return createdEntity.toComment()
    }

    override fun edit(id: Long, comment: CommentDao.DaoEditComment, authorization: String): CommentDao.EditResponse {
        val key = datastore.newKeyFactory().setKind(kind).newKey(id)
        val entity = datastore.get(key)
        return when {
            entity == null -> CommentDao.EditResponse.NotFound
            entity.getString("authorization") != authorization -> CommentDao.EditResponse.NotAuthorized
            else -> {
                val builder = Entity.newBuilder(entity)
                builder.set("text", StringValue.newBuilder(comment.text).setExcludeFromIndexes(true).build())
                if (comment.author != null) builder.set("author", comment.author)
                if (comment.website != null) builder.set("website", comment.website)
                builder.set("modified", comment.modificationTime)
                val editedEntity = builder.build()
                datastore.update(editedEntity)
                CommentDao.EditResponse.Ok(editedEntity.toComment())
            }
        }
    }

    override fun delete(id: Long, authorization: String): CommentDao.DeleteResponse {
        val key = datastore.newKeyFactory().setKind(kind).newKey(id)
        val entity = datastore.get(key)
        return when {
            entity == null -> CommentDao.DeleteResponse.NotFound
            entity.getString("authorization") != authorization -> CommentDao.DeleteResponse.NotAuthorized
            else -> {
                datastore.delete(key)
                CommentDao.DeleteResponse.Ok
            }
        }
    }

    override fun vote(id: Long, clientId: String, upvote: Boolean): CommentDao.VoteResponse? {
        val key = datastore.newKeyFactory().setKind(kind).newKey(id)
        var entity = datastore.get(key) ?: return null
        val entityBuilder = Entity.newBuilder(entity)
        val voterBloomFilter = VoterBloomFilter(b64dec.decode(entity.getString("voters")))
        val canSave = voterBloomFilter.put(clientId)
        if (canSave) {
            if (upvote) entityBuilder.set("likes", entity.getLong("likes") + 1)
            else entityBuilder.set("dislikes", entity.getLong("dislikes") + 1)
            entityBuilder.set("voters", StringValue.newBuilder(b64enc.encodeToString(voterBloomFilter.save()))
                .setExcludeFromIndexes(true)
                .build())
            entity = entityBuilder.build()
            datastore.update(entity)
        }
        return CommentDao.VoteResponse(
            entity.getLong("likes").toInt(),
            entity.getLong("dislikes").toInt(),
            canSave
        )
    }

    companion object {
        private fun<K : IncompleteKey> FullEntity.Builder<K>.setNullable(n: String, v: String?): FullEntity.Builder<K> {
            return when (v) {
                null -> setNull(n)
                else -> set(n, v)
            }
        }

        private fun<K : IncompleteKey> FullEntity.Builder<K>.setNullable(n: String, v: Long?): FullEntity.Builder<K> {
            return when (v) {
                null -> setNull(n)
                else -> set(n, v)
            }
        }

        private fun Entity.toComment() = Comment(
            id = key.id,
            parent = getNullableLong("parent"),
            text = getString("text"),
            mode = CommentMode.fromInt(getLong("mode").toInt()),
            hash = getString("hash"),
            author = getNullableString("author"),
            website = getNullableString("website"),
            likes = getLong("likes").toInt(),
            dislikes = getLong("dislikes").toInt(),
            created = getLong("created"),
            modified = getNullableLong("modified")
        )

        private fun Entity.getNullableString(n: String): String? {
            return if (isNull(n)) { null } else { getString(n) }
        }

        private fun Entity.getNullableLong(n: String): Long? {
            return if (isNull(n)) { null } else { getLong(n) }
        }

        val b64dec = Base64.getDecoder()
        val b64enc = Base64.getEncoder()
    }
}
