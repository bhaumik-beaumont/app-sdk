package researchstack.util

import kotlinx.coroutines.runBlocking
import researchstack.domain.model.log.DataSyncLog
import researchstack.domain.usecase.log.AppLogger

private const val DEFAULT_DATA_SYNC_TAG = "DataSync"

fun logDataSync(message: String, throwable: Throwable? = null, tag: String? = null) {
    val resolvedTag = tag ?: DEFAULT_DATA_SYNC_TAG
    val fullMessage = buildString {
        append("[")
        append(resolvedTag)
        append("] ")
        append(message)
        if (throwable != null) {
            append(' ')
            append(throwable.stackTraceToString())
        }
    }
    runBlocking {
        AppLogger.saveLog(DataSyncLog(fullMessage))
    }
}
