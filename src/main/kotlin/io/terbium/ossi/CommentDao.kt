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

interface CommentDao {
    fun get(uri: String): List<Comment>

    fun getId(id: Long): Comment?

    fun new(comment: DaoNewComment, authorization: String): Comment

    fun edit(id: Long, comment: DaoEditComment, authorization: String): EditResponse

    fun delete(id: Long, authorization: String): DeleteResponse

    fun vote(id: Long, clientId: String, upvote: Boolean): VoteResponse?

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
        class Ok(val comment: Comment) : EditResponse()
        object NotFound : EditResponse()
        object NotAuthorized : EditResponse()
    }

    sealed class DeleteResponse {
        object Ok : DeleteResponse()
        object NotFound : DeleteResponse()
        object NotAuthorized : DeleteResponse()
    }

    data class VoteResponse(val likes: Int, val dislikes: Int, val success: Boolean)
}
