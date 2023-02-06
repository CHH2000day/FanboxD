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

package com.chh2000day.fanboxd.fanbox.struct

import kotlinx.serialization.Serializable

@Serializable
data class SupportingCreatorInfo(
    val coverImageUrl: String?,
    val fee: Int,
    val creatorId: String,
    val description: String,
    val hasAdultContent: Boolean,
    val paymentMethod: String,
    val id: String,
    val title: String,
    val user: FanboxUser
)