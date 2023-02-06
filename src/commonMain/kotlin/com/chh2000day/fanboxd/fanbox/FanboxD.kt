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
import com.chh2000day.fanboxd.Version
import com.chh2000day.fanboxd.enum.ExitCode
import com.chh2000day.fanboxd.logger
import io.ktor.client.*
import kotlinx.coroutines.*
import platform.posix.exit
import platform.posix.usleep

/**
 * @Author CHH2000day
 * @Date 2023/2/1 16:50
 **/
class FanboxD(private val config: Config) {
    private val httpClient: HttpClient = createHttpClient(config.fanboxSessionId, ClientType.TYPE_DOWNLOADER)
    private val coroutineContext = newFixedThreadPoolContext(2, "FanboxD Worker")
    private val coroutineScope = CoroutineScope(coroutineContext)

    init {
        FanboxApiHelper.init(config.fanboxSessionId)
    }

    fun start() {
        logger.info { "Starting FanboxD version ${Version.versionName}" }
        runBlocking(coroutineContext) {
            //Check whether to start downloader
            if (config.downloadFanbox) {
                //Starter downloader
                downloader()
            }
            if (config.asDaemon) {
                //Start monitor
                coroutineScope.launch {
                    monitor()
                }
                awaitCancellation()
            }
            exit(ExitCode.NORMAL.value)
        }
    }


    fun stop() {
        coroutineContext.cancel()
        FanboxApiHelper.stop()
        //Sleep for 50 ms to allow everything goes
        usleep(50_000)
    }

    private suspend fun downloader() {

    }

    private suspend fun monitor() {
        delay(config.interval.toLong())
    }
}