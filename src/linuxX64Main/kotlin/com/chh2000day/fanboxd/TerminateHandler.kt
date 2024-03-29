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

@file:Suppress("UNUSED_PARAMETER")

package com.chh2000day.fanboxd

import com.chh2000day.fanboxd.enum.ExitCode
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import platform.posix.SIGINT
import platform.posix.SIGKILL
import platform.posix.exit
import platform.posix.signal

/**
 * @Author CHH2000day
 * @Date 2023/2/6 0:04
 **/
@OptIn(ExperimentalForeignApi::class)
actual fun setTerminateHandler() {
    signal(SIGINT, staticCFunction(::stopCallBack))
    signal(SIGKILL, staticCFunction(::emerStopCallback))
}

fun stopCallBack(signal:Int) {
    stopFanboxD()
}
fun emerStopCallback(signal:Int){
    exit(ExitCode.KILL.value)
}