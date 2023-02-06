/*
 *    Copyright 2023 Rengesou(CHH2000day)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.chh2000day.fanboxd.fanbox

import com.chh2000day.fanboxd.json
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * @Author CHH2000day
 * @Date 2023/2/1 16:51
 **/
expect fun createHttpClient(fanboxSessionId: String, clientType: ClientType): HttpClient
internal fun <T : HttpClientEngineConfig> HttpClientConfig<T>.applyCustomSettings(
    fanboxSessionId: String,
    clientType: ClientType
) {
    install(HttpCookies) {
        @Suppress("SpellCheckingInspection")
        storage =
            ConstantCookiesStorage(
                Cookie(name = "FANBOXSESSID", value = fanboxSessionId, domain = "fanbox.cc"),
                Cookie(name = "privacy_policy_notification", value = "0", domain = "fanbox.cc")
            )
    }
    install(UserAgent) {
        agent = getUA()
    }
    install(DefaultRequest) {
        if (clientType == ClientType.TYPE_API) {
            header("Accept", "application/json, text/plain, */*")
        } else {
            header("Accept", "*/*")
        }
        header("Origin", "https://www.fanbox.cc")
        header("Referer", "https://www.fanbox.cc")
        header("sec-ch-ua", "\"Chromium\";v=\"110\", \"Not A(Brand\";v=\"24\", \"Google Chrome\";v=\"110\"")
        header("sec-ch-ua-mobile", "?0")
        header("sec-ch-ua-platform", "Windows")
        header("sec-fetch-dest", "empty")
        header("sec-fetch-mode", "cors")
        header("sec-fetch-site", "same-site")
    }
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = 5)
        exponentialDelay()
    }
    install(ContentNegotiation) {
        json
    }
}

@Suppress("SpellCheckingInspection")
private fun getUA(): String {
    return listOf("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36").random()
}

enum class ClientType {
    TYPE_API, TYPE_DOWNLOADER
}