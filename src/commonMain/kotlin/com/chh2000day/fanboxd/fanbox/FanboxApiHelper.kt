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

import com.chh2000day.fanboxd.fanbox.struct.CreatorPosts
import com.chh2000day.fanboxd.fanbox.struct.CreatorPostsUrls
import com.chh2000day.fanboxd.fanbox.struct.SupportingCreators
import com.chh2000day.fanboxd.fanbox.struct.post.Post
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
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

    private const val SUPPORTING_CREATORS_URL = "https://api.fanbox.cc/plan.listSupporting"

    private lateinit var httpClient: HttpClient

    @OptIn(ExperimentalCoroutinesApi::class)
    private val coroutineContext = newSingleThreadContext("API worker")
    private val coroutineScope = CoroutineScope(coroutineContext)
    private val tokenChannel = Channel<Long>(3, BufferOverflow.DROP_OLDEST)
    private val random = Random.Default
    internal fun init(fanboxSessionId: String) {
        httpClient = createHttpClient(fanboxSessionId, ClientType.TYPE_API)
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

    suspend fun getSupportingCreators(): SupportingCreators = withContext(coroutineContext) {
        //Obtain token
        obtainToken()
        return@withContext httpClient.get(SUPPORTING_CREATORS_URL).body<SupportingCreators>()
    }

    suspend fun getCreatorPostsList(creatorId: String): CreatorPostsUrls = withContext(coroutineContext) {
        //Obtain token
        obtainToken()
        return@withContext httpClient.get("https://api.fanbox.cc/post.paginateCreator?creatorId=$creatorId")
            .body<CreatorPostsUrls>()
    }

    suspend fun getCreatorPosts(pageUrl: String): CreatorPosts = withContext(coroutineContext) {
        //Obtain token
        obtainToken()
        return@withContext httpClient.get(pageUrl).body()
    }

    suspend fun getPost(postId: String): Post = withContext(coroutineContext) {
        obtainToken()
        return@withContext httpClient.get("https://api.fanbox.cc/post.info?postId=$postId").body()
    }

    fun stop() {
        coroutineScope.cancel()
        coroutineContext.cancel()
    }
}