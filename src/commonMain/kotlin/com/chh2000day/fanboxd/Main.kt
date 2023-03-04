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

import co.touchlab.kermit.Logger
import com.chh2000day.fanboxd.enum.ExitCode
import com.chh2000day.fanboxd.enum.LaunchMode
import com.chh2000day.fanboxd.fanbox.FanboxD
import com.chh2000day.fanboxd.fanbox.proxyConfig
import io.ktor.client.engine.*
import kotlinx.cinterop.toKString
import kotlinx.serialization.SerializationException
import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toPath
import okio.buffer
import platform.posix.exit
import platform.posix.getenv

lateinit var fanboxDInstance: FanboxD
fun main(args: Array<String>) {
    if (args.contains("--help")) {
        printHelpMessage()
        exit(ExitCode.NORMAL.value)
    }
    Logger.setTag("FanboxD")
    Logger.setLogWriters(LoggerWriterImpl())
    setTerminateHandler()
    //Parse runtime configure
    val config = parseConfig(args)
    proxyConfig= parseProxyConfig(config.config.proxy)
    //Start up

    fanboxDInstance = FanboxD(config)
    fanboxDInstance.start()
}

fun stopFanboxD() {
    Logger.i { "Shutting down FanboxD" }
    fanboxDInstance.stop()
    exit(ExitCode.NORMAL.value)
}

private fun parseConfig(args: Array<String>): StartupConfig {
    //Handle args
    var currentState = ArgState.WAIT_FOR_ARG
    var configFilePath = "./config.json".toPath()
    var lastOption: Options? = null
    val cmdLineArgs = NullableConfig()
    cmdLineArgs.proxy=getenv("https_proxy")?.toKString()
    var startMode = LaunchMode.NORMAL
    var extraArgs: List<String>? = null
    for (arg in args) {
        when (currentState) {
            ArgState.WAIT_FOR_ARG -> {
                when (arg) {
                    "--config" -> {
                        lastOption = Options.CONFIG
                        currentState = ArgState.WAIT_FOR_PARAMETER
                    }

                    "--fanbox-session-id" -> {
                        lastOption = Options.SESSION_ID
                        currentState = ArgState.WAIT_FOR_PARAMETER
                    }

                    "--interval" -> {
                        lastOption = Options.INTERVAL
                        currentState = ArgState.WAIT_FOR_PARAMETER
                    }

                    "--download-dir" -> {
                        lastOption = Options.DOWNLOAD_DIR
                        currentState = ArgState.WAIT_FOR_PARAMETER
                    }

                    "--daemon" -> {
                        cmdLineArgs.asDaemon = true
                    }

                    "--no-daemon" -> {
                        cmdLineArgs.asDaemon = false
                    }

                    "--download-fanbox" -> {
                        cmdLineArgs.downloadFanbox = true
                    }

                    "--no-download-fanbox" -> {
                        cmdLineArgs.downloadFanbox = false
                    }

                    "--download-post" -> {
                        startMode = LaunchMode.DOWNLOAD_POST
                        currentState = ArgState.WAIT_FOR_PARAMETER
                        cmdLineArgs.downloadFanbox = true
                        cmdLineArgs.asDaemon = false
                    }

                    "--download-creator" -> {
                        startMode = LaunchMode.DOWNLOAD_CREATOR
                        currentState = ArgState.WAIT_FOR_PARAMETER
                        cmdLineArgs.downloadFanbox = true
                        cmdLineArgs.asDaemon = false
                    }
                    "--proxy" -> {
                        lastOption=Options.PROXY
                        currentState = ArgState.WAIT_FOR_PARAMETER
                    }

                    else -> {
                        Logger.e { "Unknown option :$arg" }
                        exit(ExitCode.WRONG_OPTION.value)
                    }
                }
            }

            ArgState.WAIT_FOR_PARAMETER -> {
                when (lastOption) {
                    Options.CONFIG -> {
                        runCatching {
                            configFilePath = arg.toPath(true)
                        }.onFailure {
                            Logger.e(it) { "Failed to parse configure: $arg" }
                            exit(ExitCode.WRONG_OPTION.value)
                        }
                    }

                    Options.SESSION_ID -> {
                        cmdLineArgs.fanboxSessionId = arg
                    }

                    Options.INTERVAL -> {
                        arg.toIntOrNull()?.let {
                            cmdLineArgs.interval = it
                        } ?: {
                            Logger.e { "Invalid interval:$arg" }
                            exit(ExitCode.WRONG_OPTION.value)
                        }
                    }

                    Options.DOWNLOAD_DIR -> {
                        runCatching {
                            cmdLineArgs.downloadDir = arg
                            arg.toPath(true)
                        }.onFailure {
                            Logger.e(it) { "Failed to parse download dir: $arg" }
                            exit(ExitCode.WRONG_OPTION.value)
                        }
                    }
                    Options.PROXY->{
                        cmdLineArgs.proxy=arg
                    }

                    null -> {
                        Logger.e { "Illegal state" }
                        exit(ExitCode.RUNTIME_ERR.value)
                    }

                    Options.DOWNLOAD_POSTS,Options.DOWNLOAD_CREATORS -> {
                        if (startMode != LaunchMode.NORMAL) {
                            extraArgs = arg.split(',')
                        }else{
                            throw IllegalStateException("Internal Error")
                        }
                    }
                }
                //Update state
                currentState = ArgState.WAIT_FOR_ARG
            }
        }
    }

    val config: Config =
        if (cmdLineArgs.asDaemon != null && cmdLineArgs.downloadFanbox != null && cmdLineArgs.downloadDir != null && cmdLineArgs.fanboxSessionId != null) {
            with(cmdLineArgs) {
                val conf = Config(fanboxSessionId!!, asDaemon!!, downloadFanbox!!, downloadDir = downloadDir!!)
                cmdLineArgs.interval?.also {
                    conf.interval = it.toLong()
                }
                conf
            }
        } else {
            //Read conf if necessary
            runCatching {
                val configFileSource = FileSystem.SYSTEM.source(configFilePath).buffer()
                val confString = configFileSource.readUtf8()
                configFileSource.close()
                val fileConfig = json.decodeFromString(Config.serializer(), confString)
                //Override config from file
                cmdLineArgs.fanboxSessionId?.also {
                    fileConfig.fanboxSessionId = it
                }
                cmdLineArgs.asDaemon?.also {
                    fileConfig.asDaemon = it
                }
                cmdLineArgs.interval?.also {
                    fileConfig.interval = it.toLong()
                }
                cmdLineArgs.downloadDir?.also {
                    fileConfig.downloadDir = it
                }
                cmdLineArgs.downloadFanbox?.also {
                    fileConfig.downloadFanbox = it
                }
                cmdLineArgs.proxy?.also {
                    fileConfig.proxy=it
                }
                fileConfig
            }.onFailure {
                when (it) {
                    is IOException -> {
                        Logger.e(it) { "Failed to read config file" }
                    }

                    is SerializationException -> {
                        Logger.e(it) { "Failed to parse config file" }
                    }

                    else -> {
                        Logger.e(it) { "Failed to create runtime config" }
                    }
                }
                exit(ExitCode.WRONG_OPTION.value)
            }.getOrNull()!!
        }
    return StartupConfig(config, startMode, extraArgs ?: emptyList())
}

private fun parseProxyConfig(proxy:String?):ProxyConfig?{
    if (proxy==null){
        return null
    }
    val protocol:String
    val host:String
    val port:Int
    val schemeSplitInfo=proxy.split("://", limit = 2)
    if (schemeSplitInfo.size!=2){
        Logger.e{"Failed to parse proxy:Could not determine protocol:$proxy"}
        return null
    }
    protocol=schemeSplitInfo[0]
    val hostInfo=schemeSplitInfo[1]
    if (protocol.equals("http",true)){
        return ProxyBuilder.http(proxy)
    }
    if (protocol.equals("socks5",true)){
        val lastIndexOfColon=hostInfo.lastIndexOf(':')+1
        if (lastIndexOfColon<1||lastIndexOfColon>=hostInfo.lastIndex){
            Logger.e { "Could not determine port:$proxy" }
            return null
        }
        host=hostInfo.substring(0,lastIndexOfColon-1)
        val portStr=hostInfo.substring(lastIndexOfColon)
        port=portStr.toIntOrNull()?:run{
            Logger.e { "Failed to parse proxy:Invalid port:$portStr" }
            return null
        }
        return ProxyBuilder.socks(host,port)
    }
    Logger.e { "Proxy protocol $protocol is not supported!" }
    return null
}
private enum class ArgState {
    WAIT_FOR_ARG, WAIT_FOR_PARAMETER
}

private enum class Options {
    CONFIG, SESSION_ID, INTERVAL, DOWNLOAD_DIR, DOWNLOAD_POSTS, DOWNLOAD_CREATORS,PROXY
}

class StartupConfig(val config: Config, val launchMode: LaunchMode, val extraArgs: List<String>)

private fun printHelpMessage() {
    println("FanboxD version ${Version.versionName}")
    println(
        """
        Usage: fanboxd [OPTION]
        
        To launch in normal mode,the following parameters must be passed to this program via either command line arguments or config file.
            'fanbox-session-id','download-dir','--daemon' or '--no-daemon','--download-fanbox' or '--no-download-fanbox'
        
        To download only specific posts/creators ,the following parameters must be passed to this program via command line.Only one type of jobs could be executed at a time.
            'fanbox-session-id','download-dir'
        Options:
            --help                                          Show this help message.
            --config                CONFIG_FILE             Read configuration from a file.Would be override by command line arguments.
            --fanbox-session-id     SESSIONID               Set fanbox sessionId.
            --daemon                                        Run as a daemon.
            --no-daemon                                     Don't run as a daemon.
            --download-fanbox                               Download all fanbox content that could be accessed before start daemon.
            --no-download-fanbox                            Don't download fanbox before start daemon.
            --interval              TIME(in sec)            Interval between two update queries.Only work when running daemon.
            --download-dir          DOWNLOAD_DIR            Specific where to store downloaded contents.
            --proxy                 PROXY_SERVER            Specific a proxy server
        Extra download options(Only download specific content and would exit after that):
            --download-post         <post(s)>               Download specific post(s).Exit after jobs done.
            --download-creator      <creatorId(s)>          Download specific posts from specific creator(s).Exit after jobs done.
    """.trimIndent()
    )
}
