package io.terbium.ossi

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.origin
import io.ktor.gson.gson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun Application.module(dao: CommentDao) {
    install(ContentNegotiation) {
        gson {
            serializeNulls()
        }
    }
    routing {
        get("/") {
            val uri = call.request.queryParameters["uri"]!!
            call.respond(dao.get(uri))
        }
        post("/new") {
            val uri = call.request.queryParameters["uri"]!!
            val newComment = call.receive<NewComment>()
            val savedComment = dao.new(newComment.toDaoNewComment(
                uri,
                System.currentTimeMillis() / 1000L,
                call.request.origin.remoteHost
            ))
            call.respond(savedComment)
        }
    }
}

fun main(args: Array<String>) {
    val server = embeddedServer(Netty, 8080) {
        module(ExposedCommentDao("jdbc:sqlite:test.db"))
    }
    server.start(wait = true)
}
