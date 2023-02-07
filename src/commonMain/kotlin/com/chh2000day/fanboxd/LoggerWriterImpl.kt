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

import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Severity
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

/**
 * @Author CHH2000day
 * @Date 2023/2/7 11:35
 **/
class LoggerWriterImpl : CommonWriter() {
    @OptIn(ExperimentalTime::class)
    override fun formatMessage(severity: Severity, message: String, tag: String, throwable: Throwable?): String {
        val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val origTimeString = currentTime.toString()
        origTimeString.substring(0, origTimeString.lastIndexOf('.'))
        return origTimeString.substring(0, origTimeString.lastIndexOf('.')) + " " + super.formatMessage(
            severity,
            message,
            tag,
            throwable
        )
    }
}