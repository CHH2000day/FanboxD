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

import com.chh2000day.fanboxd.Config
import io.ktor.client.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import platform.posix.usleep

/**
 * @Author CHH2000day
 * @Date 2023/2/1 16:50
 **/
class FanboxD(private val config: Config) {
    private val httpClient: HttpClient = createHttpClient(config.fanboxSessionId,CLIENT_TYPE.TYPE_DOWNLOADER)
    private val coroutineContext= newFixedThreadPoolContext(2,"FanboxD Worker")
    init{
        FanboxApiHelper.init(config.fanboxSessionId)
    }
    fun start(){
        runBlocking(coroutineContext) {

        }
    }

    fun stop() {
        coroutineContext.cancel()
        FanboxApiHelper.stop()
        //Sleep for 50 ms to allow everything goes
        usleep(50_000)
    }

}