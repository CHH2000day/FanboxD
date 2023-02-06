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

import com.chh2000day.fanboxd.fanbox.struct.FanboxResult

/**
 * @Author CHH2000day
 * @Date 2023/2/6 17:25
 **/
//A container that contains post object and original content before deserializing
class PostWithOriginalContent(val post: Post, val originalContent: String, override val error: String?) :
    FanboxResult() {
}