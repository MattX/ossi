package io.terbium.ossi

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.content.TextContent
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.features.origin
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.file
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.http.withCharset
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.locations.get
import io.ktor.locations.put
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
import java.time.Instant
import java.time.temporal.ChronoUnit

fun Application.module(dao: CommentDao) {
    install(DefaultHeaders) // Date header is required by isso

    install(ContentNegotiation) {
        gson {
            serializeNulls()
            registerTypeAdapter(CommentMode::class.java, CommentMode.SerDe)
        }
    }

    install(StatusPages) {
        status(HttpStatusCode.NotFound) {
            call.respond(TextContent("${it.value} ${it.description}", ContentType.Text.Plain.withCharset(Charsets.UTF_8), it))
        }
    }

    install(Locations)

    routing {
        route("/isso") {
            get("/") {
                val uri = call.request.queryParameters["uri"]!!
                val comments = dao.get(uri)
                call.respond(makeThread(comments))
            }
            post("/new") {
                val uri = call.request.queryParameters["uri"]!!
                val newComment = call.receive<NewComment>()
                val authorization = getAuthenticationToken()
                val savedComment = dao.new(
                    newComment.toDaoNewComment(
                        uri,
                        System.currentTimeMillis() / 1000L,
                        call.request.origin.remoteHost
                    ),
                    authorization
                )
                // TODO set expiration
                val cookieExptime = Instant.now().plus(1, ChronoUnit.DAYS).toGMTDate()
                call.response.cookies.append(Cookie(savedComment.id.toString(), authorization, path = "/", expires = cookieExptime))
                call.respond(HttpStatusCode.Created, savedComment)
            }
            post("/count") {
                call.respondText("[]", ContentType.Application.Json)
            }
            get<GetId> { getId ->
                val obj = dao.getId(getId.id)
                if (obj == null) {
                    call.response.status(HttpStatusCode.NotFound)
                } else {
                    call.respond(obj)
                }
            }
            put<GetId> {

            }
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
        static("/") {
            file("index.html", "static/index.html")
        }
    }
}

@Location("/id/{id}")
data class GetId(val id: Long)

fun main(args: Array<String>) {
    val server = embeddedServer(Netty, 8080) {
        module(ExposedCommentDao("jdbc:sqlite:test.db"))
    }
    server.start(wait = true)
}
