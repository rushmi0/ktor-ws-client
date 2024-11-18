package org.demo

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.websocket.*
import kotlinx.coroutines.*


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*


@Serializable
data class ReceiveEvent(
    val id: String,
    val pubkey: String,
    @SerialName("created_at") val createdAt: Long,
    val kind: Int,
    val tags: List<List<String>>,
    val content: String,
    val sig: String
)


fun String.toJsonEltArray(): JsonArray {
    val json = Json { ignoreUnknownKeys = true }
    return json.parseToJsonElement(this).jsonArray
}


// * https://ktor.io/docs/client-websockets.html#frame-types
fun main() {
    val client = HttpClient(CIO) {
        install(WebSockets) {
            pingIntervalMillis = 20_000
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
        engine {
            endpoint {
                // this: EndpointConfig
                maxConnectionsPerRoute = 100
                pipelineMaxSize = 20
                keepAliveTime = 5000
                connectTimeout = 5000
                connectAttempts = 5
            }
        }
    }

    runBlocking {
        client.webSocket(
            method = HttpMethod.Get,
            host = "relay.rushmi0.win",
            path = "/"
        ) {

            val cmd = """["REQ","hsZEO2taDsENYkP5H-JIWp",{"limit": 100}]"""
            send(cmd)

            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val data = frame.readText().toJsonEltArray()

                    if (data[0].jsonPrimitive.content == "EVENT") {
                        val jsonObject = data[2].jsonObject
                        val newData = Json.decodeFromJsonElement<ReceiveEvent>(jsonObject)
                        println(newData)
                    }
                }
            }
        }
    }

    client.close()
}
