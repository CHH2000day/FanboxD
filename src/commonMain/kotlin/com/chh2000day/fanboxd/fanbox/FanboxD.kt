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
import com.chh2000day.fanboxd.fanbox.struct.post.JsonPostContentBody
import com.chh2000day.fanboxd.logger
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import platform.posix.exit
import platform.posix.usleep

/**
 * @Author CHH2000day
 * @Date 2023/2/1 16:50
 **/
class FanboxD(private val config: Config) {
    /**
     * 16kB write buffer
     */
    private val bufferSize = 16 * 1024L
    private val httpClient: HttpClient = createHttpClient(config.fanboxSessionId, ClientType.TYPE_DOWNLOADER)
    private val coroutineContext = newFixedThreadPoolContext(2, "FanboxD Worker")
    private val coroutineScope = CoroutineScope(coroutineContext)
    private val downloadDir = config.downloadDir.toPath(true)
    private val fileSystem = FileSystem.SYSTEM

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
        logger.info { "Starting downloader" }
        logger.info { "Getting supporting creators" }
        val supportingCreators = FanboxApiHelper.getSupportingCreators()
        if (supportingCreators == null) {
            logger.error { "Could not get supporting creators.Aborting download" }
            return
        }
        val resultList = mutableListOf<Result>()
        for (creator in supportingCreators.creatorInfos) {
            val result = coroutineScope.async {
                downloadCreator(creator.creatorId)
            }.await()
            resultList.add(result)
        }
        val result = resultList.getResult()
        logger.info { "All downloads done!Result is " + result.result }
    }

    private fun Collection<Boolean>.getResult(): Result {
        if (this.isEmpty()) {
            return Result.SUCCESS
        }
        return when (this.count { it }) {
            this.size -> {
                Result.SUCCESS
            }

            0 -> {
                Result.FAILED
            }

            else -> {
                Result.PARTLY_SUCCESS
            }
        }
    }

    private fun Collection<Result>.getResult(): Result {
        val isAllSuccess = this.all { it == Result.SUCCESS }
        val isAllFailed = this.all { it == Result.FAILED }
        if (isAllSuccess) {
            return Result.SUCCESS
        }
        return if (isAllFailed) {
            Result.FAILED
        } else {
            Result.PARTLY_SUCCESS
        }
    }

    private suspend fun downloadCreator(creatorId: String): Result {
        logger.info { "Downloading posts from creator:$creatorId" }
        val pageLists = FanboxApiHelper.getCreatorPostsList(creatorId)
        if (pageLists == null) {
            logger.error { "Could not get posts list for creator : $creatorId. Aborting download" }
            return Result.FAILED
        }
        val resultList = mutableListOf<Deferred<Result>>()
        for (pageUrl in pageLists.pagedUrls) {
            resultList.add(coroutineScope.async { downloadWholePostsPage(pageUrl) })
        }
        val result = resultList.awaitAll().getResult()
        logger.info { "Download done for creator:$creatorId .Result is " + result.result }
        return result
    }

    private suspend fun downloadWholePostsPage(pageUrl: String): Result {
        logger.info { "Downloading page:$pageUrl" }
        val postLists = FanboxApiHelper.getCreatorPosts(pageUrl)
        if (postLists == null) {
            logger.error { "Failed to get page $pageUrl." }
            return Result.FAILED
        }
        val postInfos = postLists.creatorPostsBody.creatorPostInfos
        val resultList = mutableListOf<Deferred<Result>>()
        for (post in postInfos) {
            resultList.add(coroutineScope.async { downloadPost(post.id) })
        }
        val result = resultList.awaitAll().getResult()
        logger.info {
            "Download done for page:$pageUrl .Result is " + result.result
        }
        return result
    }

    private suspend fun downloadPost(postId: String): Result {
        logger.info { "Post:$postId :Starting to download " }
        val postComplex = FanboxApiHelper.getPost(postId)
        if (postComplex == null) {
            logger.error { "Failed to get post:$postId" }
            return Result.FAILED
        }
        val post = postComplex.post
        val postsBody = post.postBody
        val postDir = downloadDir / postsBody.creatorId / "posts" / postId
        kotlin.runCatching { fileSystem.createDirectories(postDir, false) }.onFailure {
            logger.error { "Download post $post failed!Failed to create dir:$postDir" }
            logger.error { it }
            return Result.FAILED
        }
        //Write post content
        coroutineScope.launch {
            val postFile = postDir / "post.json"
            kotlin.runCatching {
                val sink = fileSystem.sink(postFile, false).buffer()
                sink.use {
                    sink.writeUtf8(postComplex.originalContent)
                }
            }.onFailure {
                logger.error { "Failed to write post file : $postFile  for post :$postId" }
            }
        }
        //Download other things
        //Cover
        val coverUrl = postsBody.coverImageUrl
        if (coverUrl != null) {
            logger.info { "Post:$postId:Downloading cover" }
            coroutineScope.launch { downloadFile(coverUrl, postDir, postId, "cover.png") }
        }
        if (postsBody.body == null) {
            logger.warn { "No access permission to post:$postId" }
            return Result.FAILED
        }
        val jobList = mutableListOf<Deferred<Boolean>>()
        val contentBody = postsBody.body
        if (contentBody is JsonPostContentBody) {
            //Images
            val images = contentBody.imageMap?.values
            if (images != null) {
                val imagePath = postDir / "images"
                val thumbnailPath = postDir / "thumbnails"
                images.forEachIndexed { index, imageInfo ->
                    val imageName = "${imageInfo.id}.${imageInfo.extension}"
                    logger.info { "Post:$postId:Downloading thumbnail:[${index + 1}/${images.size + 1}] $imageName" }
                    jobList.add(coroutineScope.async {
                        downloadFile(
                            imageInfo.thumbnailUrl,
                            thumbnailPath,
                            postId,
                            imageName
                        )
                    })
                    logger.info { "Post:$postId:Downloading image:[${index + 1}/${images.size + 1}] $imageName" }
                    jobList.add(coroutineScope.async {
                        downloadFile(
                            imageInfo.originalUrl,
                            imagePath,
                            postId,
                            imageName
                        )
                    })
                }
            } else {
                logger.info { "No images for post $postId found,skipping it." }
            }
            //Files
            val files = contentBody.fileMap?.values
            if (files != null) {
                val filePath = postDir / "files"
                files.forEachIndexed { index, fileInfo ->
                    val filename = "${fileInfo.id}.${fileInfo.extension}"
                    logger.info { "Post:$postId:Downloading file:[${index + 1}/${files.size + 1}] $filename" }
                    jobList.add(coroutineScope.async { downloadFile(fileInfo.url, filePath, postId, filename) })
                }
            } else {
                logger.info { "No files for post $postId found,skipping it." }
            }
            //Wait for complete
            val result = jobList.awaitAll().getResult()
            logger.info {
                "Post:$postId:Done!Result is " + result.result
            }
            return result
        } else {
            logger.warn { "Support for legacy posts are not ready yet.Won't download other contents" }
            return Result.FAILED
        }
    }

    private suspend fun downloadFile(
        url: String,
        destinationDir: Path,
        postId: String?,
        customFileName: String? = null
    ): Boolean {
        val filename = customFileName ?: url.substring(url.lastIndexOf('/') + 1)
        val targetFile = destinationDir / filename
        if (!fileSystem.exists(destinationDir)) {
            fileSystem.createDirectories(destinationDir, false)
        }
        kotlin.runCatching {
            httpClient.prepareGet(url).execute { httpResponse ->
                val channel: ByteReadChannel = httpResponse.bodyAsChannel()
                val sink = fileSystem.sink(targetFile, false).buffer()
                sink.use {
                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(bufferSize)
                        while (packet.isNotEmpty) {
                            sink.write(packet.readBytes())
                        }
                    }
                }
            }
        }.onFailure {
            logger.error {
                "Download failed!" + if (postId != null) {
                    "Post id:"
                } else {
                    ""
                } + ".Url:$url"
            }
            logger.error { it }
            return false
        }.onSuccess {
            logger.info { "File downloaded :$targetFile" }
        }
        return true
    }

    private suspend fun monitor() {
        delay(config.interval)
    }

    private enum class Result(val result: String) {
        SUCCESS("SUCCESS!"), PARTLY_SUCCESS("partly success."), FAILED("FAILED.")
    }
}