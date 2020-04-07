package io.terbium.ossi

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun Application.module() {
    install(ContentNegotiation) {
        gson {}
    }
    routing {
        get("/snippets") {
            call.respondText("OK")
        }
    }
}

fun main(args: Array<String>) {
    val server = embeddedServer(Netty, 8080) {
        module()
    }
    server.start(wait = true)
}
