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


import com.chh2000day.fanboxd.fanbox.struct.FanboxUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostBody(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("feeRequired")
    val feeRequired: Int,
    @SerialName("publishedDatetime")
    val publishedDatetime: String,
    @SerialName("updatedDatetime")
    val updatedDatetime: String,
    @SerialName("tags")
    val tags: List<String>,
    @SerialName("isLiked")
    val isLiked: Boolean,
    @SerialName("likeCount")
    val likeCount: Int,
    @SerialName("commentCount")
    val commentCount: Int,
    @SerialName("isRestricted")
    val isRestricted: Boolean,
    @SerialName("user")
    val user: FanboxUser,
    @SerialName("creatorId")
    val creatorId: String,
    @SerialName("hasAdultContent")
    val hasAdultContent: Boolean,
    @SerialName("type")
    val type: String,
    @SerialName("coverImageUrl")
    val coverImageUrl: String,
    @SerialName("body")
    val body: PostContentBody?,
    @SerialName("excerpt")
    val excerpt: String,
    @SerialName("commentList")
    val commentList: CommentList,
    @SerialName("nextPost")
    val nextPost: BasicPostInfo?,
    @SerialName("prevPost")
    val prevPost: BasicPostInfo?,
    @SerialName("imageForShare")
    val imageForShare: String
)