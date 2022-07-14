package utils

import org.slf4j.LoggerFactory

class Logger {
    companion object {
        fun getLogger() = LoggerFactory.getLogger("ROOT_LOGGER")
    }
}
