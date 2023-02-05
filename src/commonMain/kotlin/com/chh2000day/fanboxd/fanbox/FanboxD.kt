package com.chh2000day.fanboxd.fanbox

import com.chh2000day.fanboxd.Config
import io.ktor.client.*

/**
 * @Author CHH2000day
 * @Date 2023/2/1 16:50
 **/
class FanboxD(private val config: Config) {
    private val httpClient: HttpClient = createHttpClient(config.fanboxSessionId,CLIENT_TYPE.TYPE_DOWNLOADER)
    init{
        FanboxApiHelper.init(config.fanboxSessionId)
    }

}