package com.chh2000day.fanboxd.fanbox

import io.ktor.client.*

/**
 * @Author CHH2000day
 * @Date 2023/2/1 16:51
 **/
expect fun createHttpClient(fanboxSessionId: String,clientType: CLIENT_TYPE): HttpClient
internal fun getUA(): String {
    return listOf("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36").random()
}
enum class CLIENT_TYPE{
    TYPE_API,TYPE_DOWNLOADER
}