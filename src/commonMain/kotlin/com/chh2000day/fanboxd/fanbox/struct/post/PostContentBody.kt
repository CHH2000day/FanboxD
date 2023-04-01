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

package com.chh2000day.fanboxd.fanbox.struct.post



import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
abstract class PostContentBody()

@Serializable
data class JsonPostContentBody(
    @SerialName("blocks")
    val blocks: List<Block>? = null,
    @SerialName("imageMap")
    val imageMap: Map<String, ImageInfo>? = null,
    @SerialName("images")
    val images: List<ImageInfo>? = null,
    @SerialName("fileMap")
    val fileMap: Map<String, FileInfo>? = null,
    @SerialName("files")
    val files: List<FileInfo>? = null,
    @SerialName("embedMap")
    val embedMap: Map<String, EmbedInfo>? = null,
    @SerialName("urlEmbedMap")
    val urlEmbedMap: UrlEmbedInfo? = null,
    @SerialName("text")
    val text: String? = null
) : PostContentBody()

@Serializable
data class HtmlPostContentBody(val html: String) : PostContentBody() {
    @Transient
    private val mutex = Mutex()
    private var hasParsed = false
    private val filesUrlList: MutableList<String> = mutableListOf()
    private val thumbnailUrlList: MutableList<String> = mutableListOf()
    private val contentList: MutableList<String> = mutableListOf()
    suspend fun getFilesUrlList(): List<String>{
        ensureParsed()
        return filesUrlList
    }
    suspend fun getThumbnailUrlList(): List<String> {
        ensureParsed()
        return thumbnailUrlList
    }
    suspend fun getContent():List<String>{
        ensureParsed()
        return contentList
    }
    private suspend fun ensureParsed(){
        if (!hasParsed){
            parse()
        }
    }
    private suspend fun parse() {
        mutex.lock()
        if (hasParsed){
            return
        }
        val tagRegex=Regex("<.*?>")
        val tagsRegex = Regex("<.*?>(.*?)</.*?>")
        val aTagRegex=Regex("<a.*?href=\"(.*?\\.(?!php)(?:[a-z0-9]+))\".*?>")
        val imgTagRegex = Regex("<img.*?src=\"(.*?)\".*?>")
        val contentTags=tagsRegex.findAll(html)
        contentTags.forEach {
            val content=it.groupValues[1]
            val contentWithoutTags=tagRegex.replace(content,"")
            if (contentWithoutTags.isNotEmpty()){
                contentList.add(contentWithoutTags)
            }
        }
        val aTags=aTagRegex.findAll(html)
        aTags.forEach {
            filesUrlList.add(it.groupValues[1])
        }
        val imgTags=imgTagRegex.findAll(html)
        imgTags.forEach {
            thumbnailUrlList.add(it.groupValues[1])
        }
        hasParsed=true
        mutex.unlock()
    }
}

object PostContentBodySerializer : JsonContentPolymorphicSerializer<PostContentBody>(PostContentBody::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<PostContentBody> = when {
        "html" in element.jsonObject -> {
            HtmlPostContentBody.serializer()
        }

        else -> {
            JsonPostContentBody.serializer()
        }
    }
}