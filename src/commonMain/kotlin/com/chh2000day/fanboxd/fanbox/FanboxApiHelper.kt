package com.chh2000day.fanboxd.fanbox

import io.ktor.client.*

/**
 * @Author CHH2000day
 * @Date 2023/2/5 20:28
 **/
object FanboxApiHelper {
    private lateinit var httpClient: HttpClient
    internal fun init(fanboxSessionId: String) {
        httpClient = createHttpClient(fanboxSessionId, CLIENT_TYPE.TYPE_API)
    }
}