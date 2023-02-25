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

package com.chh2000day.fanboxd.fanbox

import co.touchlab.kermit.Logger
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.winhttp.*

/**
 * @Author CHH2000day
 * @Date 2023/2/1 16:51
 **/
actual fun createHttpClient(fanboxSessionId: String,clientType: ClientType): HttpClient {
    return HttpClient(WinHttp) {
        //To avoid problem with mutability
        val mProxyConfig=proxyConfig
        if (mProxyConfig!=null) {
            if (mProxyConfig.type==ProxyType.SOCKS){
                Logger.w{"Socks5 proxy on Windows is not supported yet"}
            }else {
                Logger.i { "Using proxy $mProxyConfig" }
                engine {
                    proxy = mProxyConfig
                }
            }
        }
        applyCustomSettings(fanboxSessionId,clientType)
    }
}