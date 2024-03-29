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
import com.chh2000day.fanboxd.StartupConfig
import com.chh2000day.fanboxd.Version
import com.chh2000day.fanboxd.enum.ExitCode
import com.chh2000day.fanboxd.enum.LaunchMode
import com.chh2000day.fanboxd.fanbox.struct.post.HtmlPostContentBody
import com.chh2000day.fanboxd.fanbox.struct.post.JsonPostContentBody
import com.chh2000day.fanboxd.json
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.toInstant
import kotlinx.serialization.encodeToString
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import platform.posix.exit

/**
 * @Author CHH2000day
 * @Date 2023/2/1 16:50
 **/
class FanboxD(private val startupConfig: StartupConfig) {
    /**
     * 16kB write buffer
     */
    private val bufferSize = 16 * 1024
    private val config = startupConfig.config
    private val httpClient: HttpClient = createHttpClient(config.fanboxSessionId, ClientType.TYPE_DOWNLOADER)

    private val coroutineContext = Dispatchers.Default
    private val coroutineScope = CoroutineScope(coroutineContext)
    private val downloadDir = config.downloadDir.toPath(true)
    private val fileSystem = FileSystem.SYSTEM

    init {
        FanboxApiHelper.init(config.fanboxSessionId)
    }

    fun start() {
        Logger.i { "Starting FanboxD version ${Version.versionName}" }
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
        coroutineScope.cancel()
        FanboxApiHelper.stop()
    }

    private suspend fun downloader() {
        Logger.i { "Starting downloader" }
        if (startupConfig.launchMode == LaunchMode.DOWNLOAD_POST) {
            Logger.i { "Download posts:${startupConfig.extraArgs.joinToString()}" }
            downloadPosts(startupConfig.extraArgs)
            return
        }
        val creatorIds: List<String> = if (startupConfig.launchMode == LaunchMode.DOWNLOAD_CREATOR) {
            Logger.i { "Would download creators:${startupConfig.extraArgs.joinToString()}" }
            startupConfig.extraArgs
        } else {
            Logger.i { "Getting supporting creators" }
            val supportingCreators = FanboxApiHelper.getSupportingCreators()
            if (supportingCreators == null) {
                Logger.e { "Could not get supporting creators.Aborting download" }
                return
            }
            supportingCreators.creatorInfos.map { it.creatorId }
        }
        val resultList = mutableListOf<Result>()
        for (creatorId in creatorIds) {
            val result = coroutineScope.async {
                downloadCreator(creatorId)
            }.await()
            resultList.add(result)
        }
        val result = resultList.getResult()
        Logger.i { "All downloads done!Result is " + result.result }
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
        Logger.i { "Downloading posts from creator:$creatorId" }
        val pageLists = FanboxApiHelper.getCreatorPostsList(creatorId)
        if (pageLists == null) {
            Logger.e { "Could not get posts list for creator : $creatorId. Aborting download" }
            return Result.FAILED
        }
        val resultList = mutableListOf<Result>()
        for (pageUrl in pageLists.pagedUrls) {
            resultList.add(coroutineScope.async { downloadWholePostsPage(pageUrl) }.await())
        }
        val result = resultList.getResult()
        Logger.i { "Download done for creator:$creatorId .Result is " + result.result }
        return result
    }

    private suspend fun downloadWholePostsPage(pageUrl: String): Result {
        Logger.i { "Downloading page:$pageUrl" }
        val postLists = FanboxApiHelper.getCreatorPosts(pageUrl)
        if (postLists == null) {
            Logger.e { "Failed to get page $pageUrl." }
            return Result.FAILED
        }
        val postInfos = postLists.creatorPostsBody.creatorPostInfos
        val postIds = mutableListOf<String>()
        for (post in postInfos) {
            if (post.isRestricted) {
                Logger.i { "No access to post${post.id} .Skipping it" }
                continue
            }
            postIds.add(post.id)
        }
        val result = downloadPosts(postIds)
        Logger.i {
            "Download done for page:$pageUrl .Result is " + result.result
        }
        return result
    }

    private suspend fun downloadPosts(postIds: List<String>): Result {
        val resultList = mutableListOf<Result>()
        for (postId in postIds) {
            resultList.add(coroutineScope.async { downloadPost(postId) }.await())
        }
        return resultList.getResult()
    }

    private suspend fun downloadPost(postId: String): Result {
        Logger.i { "Post:$postId :Starting to download " }
        val postComplex = FanboxApiHelper.getPost(postId)
        if (postComplex == null) {
            Logger.e { "Failed to get post:$postId" }
            return Result.FAILED
        }
        val post = postComplex.post
        val postsBody = post.postBody
        val postDir = downloadDir / postsBody.creatorId / "posts" / postId
        val timeString = postsBody.updatedDatetime.replace(':', '-')
        kotlin.runCatching { fileSystem.createDirectories(postDir, false) }.onFailure {
            Logger.e(it) { "Download post $post failed!Failed to create dir:$postDir" }
            return Result.FAILED
        }
        suspend fun downloadBody(){
            val postFile = postDir / "post.json"
            val postFileWithTimeStamp = postDir / "post-${timeString}.json"
            val contentFileWithTimeStamp=postDir/"post-${timeString}-content.txt"
            kotlin.runCatching {
                val content = json.encodeToString(json.parseToJsonElement(postComplex.originalContent))
                val postSink = fileSystem.sink(postFile, false).buffer()
                postSink.use {
                    postSink.writeUtf8(content)
                }
                val postWithTimeSink = fileSystem.sink(postFileWithTimeStamp, false).buffer()
                postWithTimeSink.use {
                    it.writeUtf8(content)
                }
                //Extract content to a txt file
                val contentSink=fileSystem.sink(contentFileWithTimeStamp,false).buffer()
                contentSink.use {sink->
                    val postBody=postComplex.post.postBody
                    sink.writeUtf8("${postBody.title} - ${postBody.creatorId} - ${postBody.feeRequired} \n")
                    sink.writeUtf8("$timeString \n")
                    sink.writeUtf8(postBody.tags.joinToString())
                    sink.writeUtf8("\n")
                    val contentBodyBlocks=postBody.body
                    if (contentBodyBlocks!=null){
                        sink.writeUtf8("\n")
                        if (contentBodyBlocks is JsonPostContentBody) {
                            contentBodyBlocks.text?.let {
                                sink.writeUtf8("\n")
                                sink.writeUtf8(it)
                            }
                            contentBodyBlocks.blocks?.forEach {
                                if (it.type=="p"){
                                    sink.writeUtf8("\n")
                                    sink.writeUtf8(it.text?:"")
                                }
                            }
                        }else{
                            contentBodyBlocks as HtmlPostContentBody
                            contentBodyBlocks.getContent().forEach {
                                sink.writeUtf8("\n")
                                sink.writeUtf8(it)
                            }
                        }
                    }
                }
                //Copy content
                fileSystem.copy(contentFileWithTimeStamp,postDir/"post_content.txt")
                return@runCatching
            }.onFailure {
                Logger.e(it) { "Failed to write post file : $postFile  for post :$postId" }
            }
        }
        //Write post content
        coroutineScope.launch {
            downloadBody()
        }
        //Download other things
        val downloadTaskList = mutableListOf<Deferred<Boolean>>()
        //Cover
        val coverUrl = postsBody.coverImageUrl
        if (coverUrl != null) {
            Logger.i { "Post:$postId:Downloading cover" }
            downloadTaskList.add(coroutineScope.async { downloadFile(coverUrl, postDir, postId, "cover.png") })
        }
        if (postsBody.body == null) {
            Logger.e { "No access permission to post:$postId" }
            return Result.FAILED
        }
        val imagePath = postDir / "images"
        val thumbnailPath = postDir / "thumbnails"
        val filePath = postDir / "files"
        val contentBody = postsBody.body
        if (contentBody is JsonPostContentBody) {
            //Images
            val images = contentBody.imageMap?.values?.toMutableList() ?: mutableListOf()
            images += contentBody.images ?: emptyList()
            if (images.isNotEmpty()) {
                images.forEachIndexed { index, imageInfo ->
                    val imageName = "${imageInfo.id}.${imageInfo.extension}"
                    Logger.i { "Post:$postId:Downloading thumbnail:[${index + 1}/${images.size}] $imageName" }
                    downloadTaskList.add(coroutineScope.async {
                        downloadFile(
                            imageInfo.thumbnailUrl,
                            thumbnailPath,
                            postId,
                            imageName
                        )
                    })
                    Logger.i { "Post:$postId:Downloading image:[${index + 1}/${images.size}] $imageName" }
                    downloadTaskList.add(coroutineScope.async {
                        downloadFile(
                            imageInfo.originalUrl,
                            imagePath,
                            postId,
                            imageName
                        )
                    })
                }
            } else {
                Logger.i { "No images for post $postId found,skipping it." }
            }
            //Files
            val files = contentBody.fileMap?.values?.toMutableList() ?: mutableListOf()
            files += contentBody.files ?: emptyList()
            if (files.isNotEmpty()) {
                files.forEachIndexed { index, fileInfo ->
                    val filename = "${fileInfo.id}.${fileInfo.extension}"
                    Logger.i { "Post:$postId:Downloading file:[${index + 1}/${files.size}] $filename" }
                    downloadTaskList.add(coroutineScope.async {
                        downloadFile(
                            fileInfo.url,
                            filePath,
                            postId,
                            filename
                        )
                    })
                }
            } else {
                Logger.i { "No files for post $postId found,skipping it." }
            }
            //Wait for complete
            val result = downloadTaskList.awaitAll().getResult()
            Logger.i {
                "Post:$postId:Done!Result is " + result.result
            }
            return result
        } else {
            contentBody as HtmlPostContentBody
            val thumbnails = contentBody.getThumbnailUrlList()
            thumbnails.forEachIndexed { index, thumbnailUrl ->
                Logger.i { "Post:$postId:Downloading image:[${index + 1}/${thumbnails.size}]" }
                downloadTaskList.add(coroutineScope.async {
                    downloadFile(
                        thumbnailUrl,
                        thumbnailPath,
                        postId
                    )
                })
            }
            val files = contentBody.getFilesUrlList()
            files.forEachIndexed { index, fileUrl ->
                Logger.i { "Post:$postId:Downloading file:[${index + 1}/${files.size}] $fileUrl" }
                downloadTaskList.add(coroutineScope.async {
                    downloadFile(
                        fileUrl,
                        filePath,
                        postId
                    )
                })
            }
            val result = downloadTaskList.awaitAll().getResult()
            Logger.i {
                "Post:$postId:Done!Result is " + result.result
            }
            return result
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
                val buffer = ByteArray(bufferSize)
                var size = 0
                val sink = fileSystem.sink(targetFile, false).buffer()
                sink.use {
                    while (!channel.isClosedForRead && channel.readAvailable(buffer, 0, bufferSize).also {
                            size = it
                        } >= 0) {
                        sink.write(buffer, 0, size)
                    }
                }
                //This saves some memory.....
                //Seems a 'FEATURE' of kotlin/native lol
                return@execute
            }
        }.onFailure {
            Logger.e(it) {
                "Download failed!" + if (postId != null) {
                    "Post id:$postId."
                } else {
                    ""
                } + "Url:$url"
            }
            return false
        }.onSuccess {
            Logger.i { "File downloaded: $targetFile" }
        }
        return true
    }

    private suspend fun monitor() = coroutineScope {
        delay(500L)
        var lastLatestPostTime = Clock.System.now()
        while (isActive) {
            coroutineScope.launch {
                Logger.i { "Getting update from fanbox" }
                val fanboxUpdateInfo = FanboxApiHelper.getFanboxUpdate()
                if (fanboxUpdateInfo == null) {
                    Logger.e { "Failed to get update from fanbox" }
                    return@launch
                }
                val updatePostInfo = fanboxUpdateInfo.fanboxUpdateBody.fanboxUpdateInfos
                val latestPostUpdateTime = updatePostInfo.maxOfOrNull {
                    it.fanboxUpdatePostInfo.updatedDatetime.toInstant()
                } ?: Clock.System.now()
                val postsShouldUpdate = updatePostInfo.filter {
                    it.fanboxUpdatePostInfo.updatedDatetime.toInstant() > lastLatestPostTime
                }
                lastLatestPostTime = latestPostUpdateTime
                if (postsShouldUpdate.isEmpty()) {
                    Logger.i { "No update available" }
                } else {
                    val postsList = postsShouldUpdate.filter { updateInfo ->
                        val isRestricted=updateInfo.fanboxUpdatePostInfo.isRestricted
                        if (isRestricted) {
                                Logger.i { "No access to post:${updateInfo.fanboxUpdatePostInfo.id}.Skipping it." }
                            }
                        !isRestricted
                    }.map {
                        it.fanboxUpdatePostInfo.id
                    }
                    Logger.i { "Posts to download:${postsList.joinToString()}" }
                    val result = downloadPosts(postsList)
                    Logger.i { "Posts download result:${result.result}" }
                }
            }
            delay(config.interval * 1000)
        }
    }

    private enum class Result(val result: String) {
        SUCCESS("SUCCESS!"), PARTLY_SUCCESS("partly success."), FAILED("FAILED.")
    }
}