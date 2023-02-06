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


import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
    @SerialName("fileMap")
    val fileMap: Map<String, FileInfo>? = null,
    @SerialName("embedMap")
    val embedMap: Map<String,EmbedInfo>? = null,
    @SerialName("urlEmbedMap")
    val urlEmbedMap: UrlEmbedInfo? = null
) : PostContentBody()

@Serializable
data class HtmlPostContentBody(val html: String) : PostContentBody() {
    fun getImageUrlList(): List<String> = TODO()
    fun getFilesUrlList(): List<String> = TODO()
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