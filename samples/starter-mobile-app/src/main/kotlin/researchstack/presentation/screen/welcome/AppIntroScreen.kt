package researchstack.presentation.screen.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import researchstack.R
import researchstack.presentation.LocalNavController
import researchstack.presentation.component.AppTextButton
import researchstack.presentation.initiate.route.Route

@Composable
fun AppIntroScreen() {
    val navController = LocalNavController.current
    val getStartedText = stringResource(id = R.string.get_started)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF222222))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp)
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier
                        .size(192.dp)
                        .padding(bottom = 0.dp)
                ) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
                                    center = Offset(96f, 96f),
                                    radius = 96f
                                ),
                                shape = CircleShape
                            )
                            .blur(28.dp)
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_map_marker),
                        contentDescription = stringResource(id = R.string.map_marker),
                        tint = Color.White,
                        modifier = Modifier
                            .size(150.dp)
                            .align(Alignment.Center)
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.welcome_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 34.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(id = R.string.welcome_desc),
                    fontSize = 16.sp,
                    color = Color(0xFFB0B0B0),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(28.dp))
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                AppTextButton (
                    text = getStartedText,
                    onClick = {
                        navController.navigate(Route.Login.name) {
                            popUpTo(0)
                        }
                    },
                    icon = Icons.Filled.ArrowForward
                )
            }
        }
    }
}
