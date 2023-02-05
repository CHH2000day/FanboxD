package com.chh2000day.fanboxd

import kotlinx.serialization.Serializable

/**
 * @Author CHH2000day
 * @Date 2023/2/5 19:59
 **/

@Serializable
data class Config(
    val fanboxSessionId: String,
    val asDamon: Boolean = true,
    val downloadFanbox: Boolean = true,
    val interval: Int = 180,
    val downloadDir: String = "."
)

@Serializable
data class NullableConfig(
    var fanboxSessionId: String? = null,
    var asDamon: Boolean? = null,
    var downloadFanbox: Boolean? = null,
    var interval: Int? = null,
    var downloadDir: String? = null
)
