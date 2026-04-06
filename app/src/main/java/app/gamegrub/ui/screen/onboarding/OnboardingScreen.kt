package app.gamegrub.ui.screen.onboarding

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.gamegrub.R
import app.gamegrub.service.auth.PlatformOAuthHandlers
import app.gamegrub.ui.screen.auth.AmazonOAuthActivity
import app.gamegrub.ui.screen.auth.EpicOAuthActivity
import app.gamegrub.ui.screen.auth.GOGOAuthActivity
import app.gamegrub.ui.utils.SnackbarManager
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: String,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    onLoginWithSteam: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val primaryColor = MaterialTheme.colorScheme.primary

    val gogOAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val code = result.data?.getStringExtra(GOGOAuthActivity.EXTRA_AUTH_CODE)
            if (code != null) {
                lifecycleScope.launch {
                    PlatformOAuthHandlers.handleGogAuthentication(
                        context = context,
                        authCode = code,
                        coroutineScope = lifecycleScope,
                        onLoadingChange = { },
                        onError = { msg -> msg?.let { SnackbarManager.show(it) } },
                        onSuccess = { onComplete() },
                        onDialogClose = { },
                    )
                }
            }
        }
    }

    val epicOAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val code = result.data?.getStringExtra(EpicOAuthActivity.EXTRA_AUTH_CODE)
            if (code != null) {
                lifecycleScope.launch {
                    PlatformOAuthHandlers.handleEpicAuthentication(
                        context = context,
                        authCode = code,
                        coroutineScope = lifecycleScope,
                        onLoadingChange = { },
                        onError = { msg -> msg?.let { SnackbarManager.show(it) } },
                        onSuccess = { onComplete() },
                        onDialogClose = { },
                    )
                }
            }
        }
    }

    val amazonOAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val code = result.data?.getStringExtra(AmazonOAuthActivity.EXTRA_AUTH_CODE)
            if (code != null) {
                lifecycleScope.launch {
                    PlatformOAuthHandlers.handleAmazonAuthentication(
                        context = context,
                        authCode = code,
                        coroutineScope = lifecycleScope,
                        onLoadingChange = { },
                        onError = { msg -> msg?.let { SnackbarManager.show(it) } },
                        onSuccess = { onComplete() },
                        onDialogClose = { },
                    )
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onSkip) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                OnboardingPageContent(
                    page = page,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index) {
                                        primaryColor
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                ),
                        )
                    }
                }

                if (pagerState.currentPage == 3) {
                    Text(
                        text = stringResource(R.string.onboarding_connect_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onLoginWithSteam,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(stringResource(R.string.onboarding_connect_steam))
                        }
                        OutlinedButton(
                            onClick = {
                                gogOAuthLauncher.launch(Intent(context, GOGOAuthActivity::class.java))
                            },
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(stringResource(R.string.onboarding_connect_gog))
                        }
                        OutlinedButton(
                            onClick = {
                                epicOAuthLauncher.launch(Intent(context, EpicOAuthActivity::class.java))
                            },
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(stringResource(R.string.onboarding_connect_epic))
                        }
                        OutlinedButton(
                            onClick = {
                                amazonOAuthLauncher.launch(Intent(context, AmazonOAuthActivity::class.java))
                            },
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(stringResource(R.string.onboarding_connect_amazon))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onSkip) {
                        Text(
                            text = stringResource(R.string.onboarding_skip_account),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            lifecycleScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(stringResource(R.string.onboarding_next))
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: Int,
) {
    val pages = listOf(
        OnboardingPage(
            title = stringResource(R.string.onboarding_page1_title),
            description = stringResource(R.string.onboarding_page1_desc),
            icon = "🎮",
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_page2_title),
            description = stringResource(R.string.onboarding_page2_desc),
            icon = "📱",
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_page3_title),
            description = stringResource(R.string.onboarding_page3_desc),
            icon = "⚙️",
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_page4_title),
            description = stringResource(R.string.onboarding_page4_desc),
            icon = "🔐",
        ),
    )

    val currentPage = pages[page]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = currentPage.icon,
            style = MaterialTheme.typography.displayLarge,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = currentPage.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
//            brush = Brush.horizontalGradient(listOf(primaryColor, tertiaryColor)),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = currentPage.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
