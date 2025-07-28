package researchstack.presentation.screen.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Model representing a single notification. */
data class NotificationItem(
    val text: String,
    val read: Boolean = false
)

/** ViewModel managing notifications list. */
@HiltViewModel
class NotificationViewModel @Inject constructor() : ViewModel() {
    private val _notifications = MutableStateFlow(
        listOf(
            NotificationItem(text = "Welcome to Samsung Health App!"),
            NotificationItem(text = "Your weekly report is ready."),
            NotificationItem(text = "Don't forget your workout today.")
        )
    )
    val notifications: StateFlow<List<NotificationItem>> = _notifications

    val hasUnread: StateFlow<Boolean> = _notifications
        .map { list -> list.any { !it.read } }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, true)

    /** Toggle notification read state. */
    fun toggleRead(index: Int) {
        val list = _notifications.value.toMutableList()
        val item = list[index]
        list[index] = item.copy(read = !item.read)
        _notifications.value = list
    }
}
