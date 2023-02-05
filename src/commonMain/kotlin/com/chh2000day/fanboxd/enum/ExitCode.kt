package com.chh2000day.fanboxd.enum

/**
 * @Author CHH2000day
 * @Date 2023/2/5 20:48
 **/
enum class ExitCode(val value: Int) {
    NORMAL(0), WRONG_OPTION(1), RUNTIME_ERR(10)
}