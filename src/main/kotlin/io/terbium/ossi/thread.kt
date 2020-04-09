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
