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
import com.chh2000day.fanboxd.fanbox.FanboxD
import kotlinx.serialization.SerializationException
import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toPath
import okio.buffer
import platform.posix.exit

fun main(args: Array<String>) {
    if (args.contains("--help")) {
        printHelpMessage()
        exit(ExitCode.NORMAL.value)
    }
    //Parse runtime configure
    val config = parseConfig(args)
    //Start up
    FanboxD(config)
}

private fun parseConfig(args: Array<String>): Config {
    //Handle args
    var currentState = ArgState.WAIT_FOR_ARG
    var configFilePath = "./config.json".toPath()
    var lastOption: Options? = null
    val cmdLineArgs = NullableConfig()
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
                        cmdLineArgs.asDamon = true
                    }

                    "--no-daemon " -> {
                        cmdLineArgs.asDamon = false
                    }

                    "--download-fanbox" -> {
                        cmdLineArgs.downloadFanbox = true
                    }

                    "no-download-fanbox" -> {
                        cmdLineArgs.downloadFanbox = false
                    }

                    else -> {
                        println("Unknown option :$arg")
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
                            logger.error { "Failed to parse configure: $arg" }
                            logger.error { it }
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
                            logger.error { "Invalid interval:$arg" }
                            exit(ExitCode.WRONG_OPTION.value)
                        }
                    }

                    Options.DOWNLOAD_DIR -> {
                        runCatching {
                            cmdLineArgs.downloadDir = arg
                            arg.toPath(true)
                        }.onFailure {
                            logger.error { "Failed to parse download dir: $arg" }
                            logger.error { it }
                            exit(ExitCode.WRONG_OPTION.value)
                        }
                    }

                    null -> {
                        logger.error { "Illegal state" }
                        exit(ExitCode.RUNTIME_ERR.value)
                    }
                }
                //Update state
                currentState = ArgState.WAIT_FOR_ARG
            }
        }
    }

    var config: Config? = null
    if (cmdLineArgs.asDamon != null && cmdLineArgs.downloadFanbox != null && cmdLineArgs.downloadDir != null && cmdLineArgs.fanboxSessionId != null) {
        with(cmdLineArgs) {
            config = Config(fanboxSessionId!!, asDamon!!, downloadFanbox!!, downloadDir = downloadDir!!)
        }
    } else {
        //Read conf if necessary
        runCatching {
            val configFileSource = FileSystem.SYSTEM.source(configFilePath).buffer()
            val confString = configFileSource.readUtf8()
            configFileSource.close()
            config = json.decodeFromString(Config.serializer(), confString)
        }.onFailure {
            when (it) {
                is IOException -> {
                    logger.error { "Failed to read config file" }
                    logger.error { it }
                }

                is SerializationException -> {
                    logger.error { "Failed to parse config file" }
                    logger.error { it }
                }

                else -> {
                    logger.error { it }
                }
            }
            exit(ExitCode.WRONG_OPTION.value)
        }
    }
    return config!!
}

private enum class ArgState {
    WAIT_FOR_ARG, WAIT_FOR_PARAMETER
}

private enum class Options {
    CONFIG, SESSION_ID, INTERVAL, DOWNLOAD_DIR
}

private fun printHelpMessage() {
    println("FanboxD version ${Version.versionName}")
    println(
        """
        Usage: fanboxd [OPTION]
        Options:
            --help                                          Show this help message.
            --conf                  CONFIG_FILE             Read configuration from a file.Would be override by command line arguments.
            --fanbox-session-id     SESSIONID               Set fanbox sessionId.
            --daemon                                        Run as a daemon.
            --no-daemon                                     Don't run as a daemon.
            --download-fanbox                               Download all fanbox content that could be accessed before start daemon.
            --no-download-fanbox                            Don't download fanbox before start daemon.
            --interval              TIME(in sec)            Interval between two update queries.Only work when running daemon.
            --download-dir          DOWNLOAD_DIR            Specific where to store downloaded contents.
    """.trimIndent()
    )
}
