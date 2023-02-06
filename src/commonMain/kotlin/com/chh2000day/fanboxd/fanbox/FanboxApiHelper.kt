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

import io.ktor.client.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlin.random.Random

/**
 * @Author CHH2000day
 * @Date 2023/2/5 20:28
 * Helper that allow us to access fanbox api
 * Rate limit is introduced to prevent overloading fanbox
 */
object FanboxApiHelper {
    /**
     * Base interval between two api calls
     */
    private const val baseInterval = 100L

    /**
     * Max additional interval
     * Actual interval between two api calls is [baseInterval]+[maxAdditionalInterval]
     */
    private const val maxAdditionalInterval = 500
    private lateinit var httpClient: HttpClient

    @OptIn(ExperimentalCoroutinesApi::class)
    private val coroutineContext = newSingleThreadContext("API worker")
    private val coroutineScope = CoroutineScope(coroutineContext)
    private val tokenChannel = Channel<Long>(3, BufferOverflow.DROP_OLDEST)
    private val random = Random.Default
    internal fun init(fanboxSessionId: String) {
        httpClient = createHttpClient(fanboxSessionId, CLIENT_TYPE.TYPE_API)
        coroutineScope.launch {
            var counter = 0L
            //Add some tokens to prevent a slow start-up
            repeat(2) {
                tokenChannel.send(counter)
                counter++
            }
            //Add a token after a certain period
            while (isActive) {
                delay(baseInterval + random.nextInt() % maxAdditionalInterval)
                tokenChannel.send(counter)
            }
        }
    }

    private suspend fun obtainToken() {
        tokenChannel.receive()
    }

    suspend fun getAllSubscribedUsers() = withContext(coroutineContext) {
        //Obtain token
        tokenChannel.receive()
    }

    fun stop() {
        coroutineScope.cancel()
        coroutineContext.cancel()
    }
}