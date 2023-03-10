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

package com.chh2000day.fanboxd

import kotlinx.serialization.Serializable

/**
 * @Author CHH2000day
 * @Date 2023/2/5 19:59
 **/

@Serializable
data class Config(
    var fanboxSessionId: String = "",
    var asDaemon: Boolean = true,
    var downloadFanbox: Boolean = true,
    var interval: Long = 180,
    var downloadDir: String = ".",
    var proxy: String?=null
)

@Serializable
data class NullableConfig(
    var fanboxSessionId: String? = null,
    var asDaemon: Boolean? = null,
    var downloadFanbox: Boolean? = null,
    var interval: Int? = null,
    var downloadDir: String? = null,
    var proxy:String ?=null
)
