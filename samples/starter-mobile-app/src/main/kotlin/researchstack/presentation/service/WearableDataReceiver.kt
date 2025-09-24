package researchstack.presentation.service

import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.JsonParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import researchstack.HEALTH_DATA_FOLDER_NAME
import researchstack.data.datasource.local.pref.EnrollmentDatePref
import researchstack.data.datasource.local.pref.dataStore
import researchstack.domain.model.log.DataSyncLog
import researchstack.domain.model.priv.PrivDataType
import researchstack.domain.usecase.log.AppLogger
import researchstack.domain.usecase.wearable.PassiveDataStatusUseCase
import researchstack.domain.usecase.wearable.SaveWearableDataUseCase
import researchstack.domain.usecase.wearable.WearablePassiveDataStatusSenderUseCase
import researchstack.domain.repository.StudyRepository
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import org.apache.commons.io.input.ReaderInputStream
import javax.inject.Inject

@AndroidEntryPoint
class WearableDataReceiver : WearableListenerService() {
    @Inject
    lateinit var saveWearableDataUseCase: SaveWearableDataUseCase

    @Inject
    lateinit var wearablePassiveDataStatusSenderUseCase: WearablePassiveDataStatusSenderUseCase

    @Inject
    lateinit var passiveDataStatusUseCase: PassiveDataStatusUseCase

    @Inject
    lateinit var studyRepository: StudyRepository

    private val enrollmentDatePref by lazy { EnrollmentDatePref(applicationContext.dataStore) }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        if (capabilityInfo.nodes.size == 1) {
            Log.i(TAG, "node size: ${capabilityInfo.nodes.size}")
            CoroutineScope(Dispatchers.IO).launch {
                passiveDataStatusUseCase.invoke().first().forEach {
                    Log.i(TAG, it.toString())
                    wearablePassiveDataStatusSenderUseCase(PrivDataType.valueOf(it.dataType), it.enabled)
                }
            }
        }
        super.onCapabilityChanged(capabilityInfo)
    }

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        super.onChannelOpened(channel)
        Log.i(TAG, "channel opened ${channel.path}")

        val channelClient = Wearable.getChannelClient(baseContext)
        if (channel.path.contains(".csv")) {
            val outputDir = "${application.filesDir}" + HEALTH_DATA_FOLDER_NAME
            Files.createDirectories(Paths.get(outputDir))
            val file = File(outputDir, "inCompleted-${channel.path}")

            val channelCallback = object : ChannelClient.ChannelCallback() {
                override fun onInputClosed(
                    channel: ChannelClient.Channel,
                    closeReason: Int,
                    p2: Int,
                ) {
                    super.onInputClosed(channel, closeReason, p2)
                    Log.i(TAG, "onInputClosed channel ${channel.path}, closeReason: $closeReason")
                    if (closeReason != CLOSE_REASON_NORMAL) {
                        file.delete()
                        CoroutineScope(Dispatchers.IO).launch {
                            AppLogger.saveLog(DataSyncLog("wear->mobile ${channel.path} fail code: $closeReason"))
                        }
                    } else {
                        val completedFile = File(outputDir, channel.path)
                        file.renameTo(completedFile)
                        CoroutineScope(Dispatchers.IO).launch {
                            if(channel.path.lowercase().contains("bia")) {
//                                completedFile.inputStream().use { inputStream ->
//                                    val reader = BufferedReader(InputStreamReader(inputStream))
//                                    val dataType = PrivDataType.valueOf(reader.readLine())
//                                    val header = reader.readLine()
//                                    val csvBuilder = StringBuilder()
//                                    csvBuilder.appendLine("$header|weekNumber")
//                                    val studyId = studyRepository.getActiveStudies().first().firstOrNull()?.id
//                                    val enrollmentDate = studyId?.let { enrollmentDatePref.getEnrollmentDate(it) }?.let { LocalDate.parse(it) }
//                                    reader.lineSequence().forEach { line ->
//                                        if (line.isNotBlank()) {
//                                            val columns = line.split('|')
//                                            val timestamp = columns[11].toLongOrNull()
//                                            val week = if (timestamp != null && enrollmentDate != null) {
//                                                val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
//                                                ChronoUnit.WEEKS.between(enrollmentDate, date).toInt() + 1
//                                            } else 0
//                                            csvBuilder.appendLine("$line|$week")
//                                        }
//                                    }
//                                    saveWearableDataUseCase(dataType, csvBuilder.toString().byteInputStream())
//                                }
                            }
                            AppLogger.saveLog(DataSyncLog("wear->mobile ${channel.path} success"))
                        }
                    }

                    channelClient.close(channel)
                    channelClient.unregisterChannelCallback(this)
                }
            }
            kotlin.runCatching {
                Tasks.await(channelClient.registerChannelCallback(channel, channelCallback))
                Tasks.await(channelClient.receiveFile(channel, Uri.fromFile(file), false))
            }.onFailure {
                Log.e(TAG, "${it.message}")
                channelClient.unregisterChannelCallback(channel, channelCallback)
            }
        } else {
            kotlin.runCatching {
                Tasks.await(channelClient.getInputStream(channel))
            }.onSuccess {
                it.use { inputStream ->
//                    val jsonObject = JsonParser.parseString(String(inputStream.readBytes())).asJsonObject
//                    saveWearableDataUseCase(jsonObject)
                }
                channelClient.close(channel)
            }.onFailure { Log.e(TAG, "${it.message}") }
        }
    }

    companion object {
        private val TAG = WearableDataReceiver::class.simpleName
    }
}
