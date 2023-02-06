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

import com.chh2000day.fanboxd.enum.ExitCode
import kotlinx.cinterop.staticCFunction
import platform.posix.exit
import platform.windows.*

/**
 * @Author CHH2000day
 * @Date 2023/2/6 0:04
 **/
actual fun setTerminateHandler() {
    SetConsoleCtrlHandler(staticCFunction(::terminateCallback), TRUE)
}

fun terminateCallback(fdwCtrlType: DWORD): WINBOOL {
    when (fdwCtrlType.toInt()) {
        CTRL_C_EVENT -> {
            //Stops safely
            stopFanboxD()
            return TRUE
        }

        CTRL_BREAK_EVENT -> {
            //Stops immediately
            exit(ExitCode.KILL.value)
            return TRUE
        }

        else -> {
            return FALSE
        }
    }
}