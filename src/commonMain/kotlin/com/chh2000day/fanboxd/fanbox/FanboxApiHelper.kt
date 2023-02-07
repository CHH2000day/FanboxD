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

import co.touchlab.kermit.Logger
import com.chh2000day.fanboxd.fanbox.struct.FanboxResult
import com.chh2000day.fanboxd.fanbox.struct.creator.CreatorPosts
import com.chh2000day.fanboxd.fanbox.struct.creator.CreatorPostsUrls
import com.chh2000day.fanboxd.fanbox.struct.creator.SupportingCreators
import com.chh2000day.fanboxd.fanbox.struct.post.Post
import com.chh2000day.fanboxd.fanbox.struct.post.PostWithOriginalContent
import com.chh2000day.fanboxd.json
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

    private const val maxRetries = 3

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

    private suspend fun <T : FanboxResult> getValidResult(block: suspend () -> T): T? {
        var errCounter = 0
        var result = kotlin.runCatching { block() }.onFailure {
            Logger.e(it) { "Failed to access fanbox api:" }
        }.getOrNull()
        while (result == null || result.hasError && errCounter < maxRetries) {
            Logger.w { "Getting error from fanbox api.Consider to check your fanbox session id" }
            Logger.w { "Fanbox api error:${result?.error}" }
            errCounter++
            result = kotlin.runCatching { block() }.onFailure {
                Logger.e(it) { "Failed to access fanbox api:" }
            }.getOrNull()
        }
        if (result.hasError) {
            Logger.e { "Getting errors multiple time from fanbox api.Consider to check your fanbox session id" }
            return null
        }
        return result
    }

    private suspend fun doGetSupportingCreators(): SupportingCreators = withContext(coroutineContext) {
        //Obtain token
        obtainToken()
        return@withContext httpClient.get(SUPPORTING_CREATORS_URL).body()
    }

    suspend fun getSupportingCreators(): SupportingCreators? = withContext(coroutineContext) {
        return@withContext getValidResult { doGetSupportingCreators() }
    }

    private suspend fun doGetCreatorPostsList(creatorId: String): CreatorPostsUrls = withContext(coroutineContext) {
        //Obtain token
        obtainToken()
        return@withContext httpClient.get("https://api.fanbox.cc/post.paginateCreator?creatorId=$creatorId")
            .body<CreatorPostsUrls>()
    }

    suspend fun getCreatorPostsList(creatorId: String): CreatorPostsUrls? = withContext(coroutineContext) {
        return@withContext getValidResult { doGetCreatorPostsList(creatorId) }
    }

    private suspend fun doGetCreatorPosts(pageUrl: String): CreatorPosts = withContext(coroutineContext) {
        //Obtain token
        obtainToken()
        return@withContext httpClient.get(pageUrl).body()
    }

    suspend fun getCreatorPosts(pageUrl: String): CreatorPosts? = withContext(coroutineContext) {
        return@withContext getValidResult { doGetCreatorPosts(pageUrl) }
    }

    private suspend fun doGetPost(postId: String): PostWithOriginalContent = withContext(coroutineContext) {
        obtainToken()
        val content = httpClient.get("https://api.fanbox.cc/post.info?postId=$postId").body<String>()
        val post = json.decodeFromString(Post.serializer(), content)
        return@withContext PostWithOriginalContent(post, content, post.error)
    }

    suspend fun getPost(postId: String): PostWithOriginalContent? = withContext(coroutineContext) {
        return@withContext getValidResult { doGetPost(postId) }
    }

    fun stop() {
        coroutineScope.cancel()
        coroutineContext.cancel()
    }
}