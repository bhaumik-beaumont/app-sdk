package researchstack.presentation.main.screen

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import researchstack.R
import researchstack.presentation.main.MedicalInfoActivity
import researchstack.presentation.main.component.AlwaysVisiblePositionIndicator
import researchstack.presentation.main.viewmodel.HomeViewModel
import researchstack.presentation.theme.HomeScreenItemBackground
import researchstack.presentation.theme.SubTextColor
import researchstack.presentation.theme.TextColor

enum class HomeScreenItem {
    BLOOD_OXYGEN,
    ECG,
    BODY_COMPOSITION,
    PPG_RED,
    PPG_IR
}

@Composable
fun HomeScreenItem.getItemTitle(): String {
    val context = LocalContext.current
    return when (this) {
        HomeScreenItem.BLOOD_OXYGEN -> context.getString(R.string.blood_oxygen)
        HomeScreenItem.ECG -> context.getString(R.string.ecg)
        HomeScreenItem.BODY_COMPOSITION -> context.getString(R.string.body_composition)
        HomeScreenItem.PPG_RED -> context.getString(R.string.ppg_red)
        HomeScreenItem.PPG_IR -> context.getString(R.string.ppg_ir)
    }
}

fun HomeScreenItem.getItemPrefKey(): Preferences.Key<Long> {
    return when (this) {
        HomeScreenItem.BLOOD_OXYGEN -> longPreferencesKey(HomeScreenItem.BLOOD_OXYGEN.name)
        HomeScreenItem.ECG -> longPreferencesKey(HomeScreenItem.ECG.name)
        HomeScreenItem.BODY_COMPOSITION -> longPreferencesKey(HomeScreenItem.BODY_COMPOSITION.name)
        HomeScreenItem.PPG_RED -> longPreferencesKey(HomeScreenItem.PPG_RED.name)
        HomeScreenItem.PPG_IR -> longPreferencesKey(HomeScreenItem.PPG_IR.name)
    }
}

fun HomeScreenItem.getItemIcon(): Int {
    return when (this) {
        HomeScreenItem.BLOOD_OXYGEN -> R.drawable.health_blood_oxygen
        HomeScreenItem.ECG -> R.drawable.health_ecg
        HomeScreenItem.BODY_COMPOSITION -> R.drawable.health_body_composition
        HomeScreenItem.PPG_RED -> R.drawable.ppg_red
        HomeScreenItem.PPG_IR -> R.drawable.ppg_ir
    }
}

fun HomeScreenItem.getItemActivityClass(): Class<*> {
    return MedicalInfoActivity::class.java
}

@Composable
fun HomeScreenItem.View(hashLastMeasure: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(57.dp)
            .padding(top = 8.dp)
            .background(HomeScreenItemBackground, RoundedCornerShape(26))
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(12.dp))
        Image(
            painter = painterResource(id = getItemIcon()),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = getItemTitle(),
                color = TextColor,
                fontSize = 16.sp
            )
            if (hashLastMeasure.isNotBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = hashLastMeasure,
                    color = SubTextColor,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun HomeScreen(context: Context, homeViewModel: HomeViewModel = hiltViewModel()) {
    val listState = rememberScalingLazyListState()
    val homeScreenItems = HomeScreenItem.values().filter {
        homeViewModel.ecgMeasurementEnabled.observeAsState().value == true ||
            (it != HomeScreenItem.ECG && it != HomeScreenItem.BODY_COMPOSITION)
    }

    Scaffold(
        positionIndicator = {
            AlwaysVisiblePositionIndicator(
                scalingLazyListState = listState
            )
        }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            state = listState,
        ) {
            item {
                dimensionResource(id = R.dimen.cardview_compat_inset_shadow)
                Spacer(modifier = Modifier.height(20.dp))
            }
            item {
                Text(
                    text = stringResource(R.string.app_name_wearable),
                    color = TextColor,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                Text(
                    text = stringResource(R.string.app_message),
                    textAlign = TextAlign.Center,
                    color = TextColor,
                    fontSize = 16.sp
                )
            }

            items(count = homeScreenItems.size) { index ->
                val homeItem = homeScreenItems[index]
                val lastMeasure = homeViewModel.getLiveDataByType(homeItem).observeAsState().value ?: ""
                homeItem.View(lastMeasure) {
                    val intent = Intent(context, homeItem.getItemActivityClass())
                        .putExtra(MedicalInfoActivity.EXTRA_HOME_ITEM, homeItem.name)
                    context.startActivity(intent)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

//            item {
//                AppButton(HomeScreenItemBackground, stringResource(id = R.string.note)) {
//                    context.startActivity(Intent(context, NoteActivity::class.java))
//                }
//            }
//
//            item { Spacer(modifier = Modifier.height(8.dp)) }
//
//            item {
//                AppButton(HomeScreenItemBackground, stringResource(id = R.string.settings)) {
//                    context.startActivity(Intent(context, SettingActivity::class.java))
//                }
//            }
//
//            if (BuildConfig.ENABLE_INSTANT_SYNC_BUTTON) {
//                item { Spacer(modifier = Modifier.height(8.dp)) }
//                item {
//                    AppButton(HomeScreenItemBackground, stringResource(id = R.string.sync)) {
//                        WorkManager.getInstance(context).enqueue(
//                            OneTimeWorkRequestBuilder<SyncPrivDataWorker>().build()
//                        )
//                        Toast.makeText(
//                            context,
//                            context.resources.getString(R.string.synchronizing),
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
//            }

            item { Spacer(modifier = Modifier.height(53.dp)) }
        }
    }
}
