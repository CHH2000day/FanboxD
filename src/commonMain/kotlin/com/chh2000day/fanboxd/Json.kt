package com.chh2000day.fanboxd

import kotlinx.serialization.json.Json

/**
 * @Author CHH2000day
 * @Date 2023/2/5 22:16
 **/
val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}