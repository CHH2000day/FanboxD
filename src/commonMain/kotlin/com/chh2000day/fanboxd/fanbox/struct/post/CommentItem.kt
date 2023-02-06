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
data class CommentItem(
    @SerialName("id")
    val id: String,
    @SerialName("parentCommentId")
    val parentCommentId: String,
    @SerialName("rootCommentId")
    val rootCommentId: String,
    @SerialName("body")
    val body: String,
    @SerialName("createdDatetime")
    val createdDatetime: String,
    @SerialName("likeCount")
    val likeCount: Int,
    @SerialName("isLiked")
    val isLiked: Boolean,
    @SerialName("isOwn")
    val isOwn: Boolean,
    @SerialName("user")
    val user: FanboxUser,
    @SerialName("replies")
    val replies: List<Reply>
)