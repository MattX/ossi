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

import com.moandjiezana.toml.Toml
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.content.TextContent
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.file
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.http.withCharset
import io.ktor.locations.*
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.date.toGMTDate
import java.io.File
import java.lang.RuntimeException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

val cookieValiditySeconds = TimeUnit.HOURS.toSeconds(24)

fun Application.ossi(dao: CommentDao, prefix: String, serveStatic: Boolean = false) {
    install(DefaultHeaders)

    install(ContentNegotiation) {
        gson {
            serializeNulls()
            registerTypeAdapter(CommentMode::class.java, CommentMode.SerDe)
        }
    }

    install(StatusPages) {
        status(HttpStatusCode.NotFound) {
            call.respond(TextContent("${it.value} ${it.description}",
                ContentType.Text.Plain.withCharset(Charsets.UTF_8), it))
        }
    }

    install(Locations)

    routing {
        route("/$prefix") {
            get("/") {
                val uri = call.request.queryParameters["uri"] ?: return@get call.response.status(HttpStatusCode.BadRequest)
                val comments = dao.get(uri)
                call.respond(makeThread(comments))
            }
            post("/new") {
                val uri = call.request.queryParameters["uri"] ?: return@post call.response.status(HttpStatusCode.BadRequest)
                val newComment = call.receive<NewCommentRequest>()
                if (!newComment.validate()) return@post call.response.status(HttpStatusCode.BadRequest)
                val authorization = getAuthenticationToken()
                val savedComment = dao.new(
                    newComment.toDaoNewComment(
                        uri,
                        Instant.now().toEpochMilli() / 1000L,
                        call.request.origin.remoteHost
                    ),
                    authorization
                )

                // TODO what is this madness
                val cookieExptime = Instant.now().plus(cookieValiditySeconds, ChronoUnit.SECONDS).toGMTDate()
                call.response.cookies.append(Cookie(savedComment.id.toString(), authorization, path = "/",
                    expires = cookieExptime))
                call.response.headers.append("X-Set-Cookie",
                "isso-${savedComment.id}=$authorization; Max-Age=$cookieValiditySeconds}; Path=/")
                call.respond(HttpStatusCode.Created, savedComment)
            }
            post("/count") {
                call.respondText("[]", ContentType.Application.Json)
            }
            get<GetId> { getId ->
                var obj = dao.getId(getId.id)
                val plain = call.request.queryParameters["plain"] == "1"
                if (obj == null) {
                    call.response.status(HttpStatusCode.NotFound)
                } else {
                    if (!plain) obj = obj.rendered()
                    call.respond(obj)
                }
            }
            put<GetId> { getId ->
                val editComment = call.receive<EditCommentRequest>()
                if (!editComment.validate()) return@put call.response.status(HttpStatusCode.BadRequest)
                val authorization = call.request.cookies[getId.id.toString()]
                    ?: return@put call.response.status(HttpStatusCode.Forbidden)
                val daoEditComment = editComment.toDaoEditComment(
                Instant.now().toEpochMilli() / 1000L
                )
                when (val response = dao.edit(getId.id, daoEditComment, authorization)) {
                    is CommentDao.EditResponse.NotFound -> call.response.status(HttpStatusCode.NotFound)
                    is CommentDao.EditResponse.NotAuthorized -> call.response.status(HttpStatusCode.Forbidden)
                    is CommentDao.EditResponse.Ok -> call.respond(response.comment.rendered())
                }
            }
            delete<GetId> { getId ->
                val authorization = call.request.cookies[getId.id.toString()]
                    ?: return@delete call.response.status(HttpStatusCode.Forbidden)
                when (dao.delete(getId.id, authorization)) {
                    is CommentDao.DeleteResponse.NotFound -> call.response.status(HttpStatusCode.NotFound)
                    is CommentDao.DeleteResponse.NotAuthorized -> call.response.status(HttpStatusCode.Forbidden)
                    is CommentDao.DeleteResponse.Ok -> call.respondText("null", ContentType.Application.Json)
                }
            }
            post("/preview") {
                val previewRequest = call.receive<PreviewRequest>()
                call.respond(PreviewResponse(renderMarkdown(previewRequest.text)))
            }
            post<Like> { vote(it.id, call, true, dao) }
            post<Dislike> { vote(it.id, call, false, dao) }

            if (serveStatic) {
                static("/js") {
                    files("static/js")
                }
                static("/css") {
                    files("static/css")
                }
                static("/img") {
                    files("static/img")
                }
            }
        }
        if (serveStatic) static("/") {
            file("index.html", "static/index.html")
        }
    }
}

suspend fun vote(id: Long, call: ApplicationCall, upvote: Boolean, dao: CommentDao) {
    when (val voteResult = dao.vote(id, call.request.origin.remoteHost, upvote)) {
        null -> call.response.status(HttpStatusCode.NotFound)
        else -> call.respond(LikeResponse(voteResult.likes, voteResult.dislikes))
    }
}

@Location("/id/{id}")
data class GetId(val id: Long)

@Location("/id/{id}/like")
data class Like(val id: Long)

@Location("/id/{id}/dislike")
data class Dislike(val id: Long)

fun main(args: Array<String>) {
    val configPath = args.getOrNull(0)
    val configFile = configPath?.let { Toml().read(File(it)) }
    val dao = if (configFile != null) {
        when (val dbType = configFile.getString("db").toLowerCase()) {
            "firestore" -> {
                val firestoreTable = configFile.getTable("firestore")
                FirestoreDao(firestoreTable.getString("project"), firestoreTable.getString("kind"))
            }
            "sqlite" -> {
                val sqliteTable = configFile.getTable("firestore")
                SqliteDao("jdbc:sqlite:${sqliteTable.getString("dbPath")})")
            }
            else -> throw RuntimeException("unknown database type: $dbType")
        }
    } else {
        SqliteDao("jdbc:sqlite:test.db")
    }
    val serveStatic = configFile?.getBoolean("serve_static") ?: true

    val server = embeddedServer(Netty, 8080) {
        ossi(dao, "isso", serveStatic = true)
    }
    server.start(wait = true)
}
