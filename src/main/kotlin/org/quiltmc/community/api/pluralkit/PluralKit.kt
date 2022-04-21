/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.api.pluralkit

import dev.kord.common.entity.Snowflake
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import mu.KotlinLogging

internal const val PK_BASE_URL = "https://api.pluralkit.me/v2"
internal const val MESSAGE_URL = "$PK_BASE_URL/messages/{id}"

class PluralKit {
    private val logger = KotlinLogging.logger { }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json { ignoreUnknownKeys = true },
                ContentType.Any
            )
        }

        expectSuccess = true
    }

    suspend fun getMessage(id: Snowflake) =
        getMessage(id.toString())

    @Suppress("MagicNumber")
    suspend fun getMessage(id: String): PKMessage {
        val url = MESSAGE_URL.replace("id" to id)

        try {
            val result: PKMessage = client.get(url).body()

            logger.debug { "/messages/$id -> 200" }

            return result
        } catch (e: ClientRequestException) {
            if (e.response.status.value in 400 until 600) {
                if (e.response.status.value == HttpStatusCode.NotFound.value) {
                    logger.debug { "/messages/$id -> ${e.response.status}" }
                } else {
                    logger.error(e) { "/messages/$id -> ${e.response.status}" }
                }
            }

            throw e
        }
    }

    suspend fun getMessageOrNull(id: Snowflake) =
        getMessageOrNull(id.toString())

    @Suppress("MagicNumber")
    suspend fun getMessageOrNull(id: String): PKMessage? {
        try {
            return getMessage(id)
        } catch (e: ClientRequestException) {
            if (e.response.status.value !in 400 until 499) {
                throw e
            }
        }

        return null
    }

    private fun String.replace(vararg pairs: Pair<String, Any>): String {
        var result = this

        pairs.forEach { (k, v) ->
            result = result.replace("{$k}", v.toString())
        }

        return result
    }
}
