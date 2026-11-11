package com.lyricsflow.player.ui.pages.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.lyricsflow.player.R
import com.lyricsflow.player.code.MediaController
import com.lyricsflow.player.code.auth.LyricsFlowAuthManager
import com.lyricsflow.player.code.rpc.DiscordRpcManager
import com.lyricsflow.player.code.updater.AppUpdateChecker
import com.lyricsflow.player.code.utils.downloader.SongDownloaderManager
import com.lyricsflow.player.code.utils.others.Vibrator
import com.lyricsflow.player.data.libraries.MusicLibrary
import com.lyricsflow.player.data.libraries.SettingsLibrary
import com.lyricsflow.player.ui.UI
import com.lyricsflow.player.ui.theme.SFProDisplay
import com.lyricsflow.player.ui.theme.withNight
import com.lyricsflow.player.ui.toUI
import com.lyricsflow.player.ui.widgets.basic.RoundColumn
import com.lyricsflow.player.ui.widgets.basic.Title
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(navController: NavController) =
    SettingBackground {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val userProfile by LyricsFlowAuthManager.currentUser
        val discordEnabled by DiscordRpcManager.isEnabled
        val discordUsername by DiscordRpcManager.discordUsername
        val discordAvatar by DiscordRpcManager.discordAvatarUrl
        val discordConnected by DiscordRpcManager.isConnected
        val availableUpdate by AppUpdateChecker.availableUpdate
        val isCheckingUpdate by AppUpdateChecker.isChecking

        var showAuthSheet by remember { mutableStateOf(false) }
        var showDiscordSheet by remember { mutableStateOf(false) }

        val textColor = Color.Black withNight Color.White
        val secondaryColor = (Color.Black withNight Color.White).copy(alpha = 0.58f)
        val appleRed = Color(0xFFFA233B)
        val discordBlurple = Color(0xFF5865F2)

        Title(
            title = stringResource(id = R.string.page_settings_title),
            onBack = {
                navController.popBackStack()
            },
            content = {
                item("settings") {
                    Column(Modifier.fillMaxSize()) {
                        // 1. iOS 27 LyricsFlow Account Card
                        ListHeader("LYRICSFLOW ACCOUNT")
                        RoundColumn {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        Vibrator.click(context)
                                        showAuthSheet = true
                                    }
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(appleRed.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (userProfile.avatarUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = userProfile.avatarUrl,
                                            contentDescription = userProfile.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.AccountCircle,
                                            contentDescription = null,
                                            tint = appleRed,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 14.dp)
                                ) {
                                    Text(
                                        text = if (userProfile.isLoggedIn) userProfile.name else "Sign in to LyricsFlow",
                                        fontFamily = SFProDisplay,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (userProfile.isLoggedIn) {
                                            userProfile.email.ifBlank { "Cloud Profile Synced" }
                                        } else {
                                            "Sync listening history, community TTML & cloud profile"
                                        },
                                        fontFamily = SFProDisplay,
                                        fontSize = 13.sp,
                                        color = secondaryColor,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = secondaryColor
                                )
                            }
                        }

                        // 2. Discord Rich Presence ("LyricsFlow Mobile") Card
                        GroupSpacer()
                        ListHeader("DISCORD RICH PRESENCE")
                        RoundColumn {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        Vibrator.click(context)
                                        showDiscordSheet = true
                                    }
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(discordBlurple.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (discordAvatar.isNotBlank()) {
                                        AsyncImage(
                                            model = discordAvatar,
                                            contentDescription = discordUsername,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Headphones,
                                            contentDescription = null,
                                            tint = discordBlurple,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 14.dp)
                                ) {
                                    Text(
                                        text = if (DiscordRpcManager.hasToken()) {
                                            "Connected: ${discordUsername.ifBlank { "Discord Account" }}"
                                        } else {
                                            "Link Discord Account"
                                        },
                                        fontFamily = SFProDisplay,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = textColor
                                    )
                                    Text(
                                        text = if (discordConnected) {
                                            "Broadcasting \"Listening to LyricsFlow Mobile\" with animated art"
                                        } else {
                                            "Show \"LyricsFlow Mobile\" status, animated cover & Apple Music buttons"
                                        },
                                        fontFamily = SFProDisplay,
                                        fontSize = 12.5.sp,
                                        color = secondaryColor
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = secondaryColor
                                )
                            }
                            if (DiscordRpcManager.hasToken()) {
                                Divider()
                                SwitchItem(
                                    title = "Enable \"LyricsFlow Mobile\" Rich Presence",
                                    onClick = {
                                        DiscordRpcManager.setRpcEnabled(!discordEnabled)
                                    },
                                    checkedLambda = { discordEnabled }
                                )
                            }
                        }

                        // 3. Online Streaming & Download Audio Quality (Lossless ALAC / Dolby Atmos)
                        GroupSpacer()
                        ListHeader("STREAMING & DOWNLOAD QUALITY")
                        RoundColumn {
                            SwitchItem(
                                title = "Lossless Audio (ALAC 24-bit/192kHz)",
                                onClick = {
                                    SettingsLibrary.EnableLosslessStreaming = !SettingsLibrary.EnableLosslessStreaming
                                },
                                checkedLambda = { SettingsLibrary.EnableLosslessStreaming }
                            )
                            Divider()
                            SwitchItem(
                                title = "Dolby Atmos Spatial Audio",
                                onClick = {
                                    SettingsLibrary.EnableDolbyAtmos = !SettingsLibrary.EnableDolbyAtmos
                                },
                                checkedLambda = { SettingsLibrary.EnableDolbyAtmos }
                            )
                            Divider()
                            val downloadPath = remember {
                                SongDownloaderManager.getRootDownloadDir(context).absolutePath
                            }
                            LabelItem(
                                title = "Downloads Folder",
                                desc = downloadPath
                            ) {
                                Toast.makeText(context, "Saved to: $downloadPath", Toast.LENGTH_LONG).show()
                            }
                        }
                        ListHeader(content = "Streams and downloads tracks in Apple Lossless (ALAC) or Dolby Atmos with automatic AAC fallback.")

                        // 4. Library Settings
                        GroupSpacer()
                        ListHeader(stringResource(id = R.string.page_library_title))
                        RoundColumn {
                            SwitchItem(
                                title = stringResource(id = R.string.settings_library_refresh_everytime),
                                onClick = {
                                    SettingsLibrary.RefreshEveryTime =
                                        !SettingsLibrary.RefreshEveryTime
                                },
                                checkedLambda = { SettingsLibrary.RefreshEveryTime }
                            )

                            Divider()
                            LabelItem(title = stringResource(id = R.string.settings_library_overview)) {
                                navController.toUI(UI.Settings.LibraryOverview)
                            }
                            Divider()
                            LabelItem(
                                title = stringResource(id = R.string.settings_library_artist_split_title),
                                desc = stringResource(id = R.string.settings_library_artist_split_desc),
                            ) {
                                navController.toUI(UI.Settings.ArtistSplit)
                            }
                        }

                        GroupSpacerMedium()
                        RoundColumn {
                            LabelItem(
                                title = stringResource(id = R.string.settings_library_refresh_now),
                                superLink = true
                            ) {
                                scope.launch(Dispatchers.Main) {
                                    var toast = Toast.makeText(
                                        context,
                                        R.string.tip_scanning,
                                        Toast.LENGTH_SHORT
                                    )
                                    toast.show()
                                    if (!MusicLibrary.hasAudioPermission(context)) {
                                        toast.cancel()
                                        Toast.makeText(context, R.string.permission_grant_subtitle, Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    withContext(Dispatchers.IO) {
                                        MusicLibrary.scanMedia(context)
                                    }
                                    toast.cancel()
                                    val size = MediaController.mainMusicList.size
                                    if (size == 0) {
                                        toast = Toast.makeText(
                                            context,
                                            R.string.tip_no_song,
                                            Toast.LENGTH_SHORT
                                        )
                                    } else {
                                        val msg =
                                            context.getString(R.string.tip_scan_finished, size)
                                        toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT)
                                    }
                                    toast.show()
                                }
                            }
                        }
                        ListHeader(content = stringResource(id = R.string.settings_library_refresh_now_desc))

                        // 5. Typography, Lyrics & Interface
                        GroupSpacer()
                        ListHeader(stringResource(id = R.string.settings_performance))
                        RoundColumn {
                            LabelItem(
                                title = stringResource(id = R.string.settings_performance_lyric_title),
                                desc = "Font Weight (${SettingsLibrary.LyricFontWeightNumeric}), Width (${SettingsLibrary.LyricFontWidth}%), Translation & Pronunciation"
                            ) {
                                navController.toUI(UI.Settings.LyricSetting)
                            }
                            Divider()
                            LabelItem(title = stringResource(id = R.string.settings_performance_ui_title)) {
                                navController.toUI(UI.Settings.UserInterfaceSetting)
                            }
                            Divider()
                            LabelItem(title = stringResource(id = R.string.settings_performance_notification_title)) {
                                navController.toUI(UI.Settings.NotificationSetting)
                            }
                        }

                        GroupSpacer()
                        ListHeader(stringResource(id = R.string.settings_audio))
                        RoundColumn {
                            LabelItem(title = stringResource(id = R.string.settings_audio_exoplayer)) {
                                navController.toUI(UI.Settings.ExoplayerSetting)
                            }
                            Divider()
                            SwitchItem(
                                title = stringResource(id = R.string.settings_audio_fade_in_out),
                                onClick = { },
                                checkedLambda = { SettingsLibrary.FadePlay }
                            )
                        }
                        ListHeader(content = stringResource(id = R.string.settings_audio_fade_in_out_desc))

                        GroupSpacer()
                        ListHeader(stringResource(id = R.string.settings_play))
                        RoundColumn {
                            SwitchItem(
                                title = stringResource(id = R.string.settings_play_history),
                                onClick = { },
                                checkedLambda = { SettingsLibrary.ListenHistory }
                            )
                        }
                        ListHeader(content = stringResource(id = R.string.settings_play_history_desc))

                        GroupSpacer()
                        ListHeader(stringResource(id = R.string.settings_stats))
                        StatsDataSection()
                        ListHeader(content = stringResource(id = R.string.settings_stats_desc))

                        GroupSpacer()
                        ListHeader(stringResource(id = R.string.settings_extend))
                        RoundColumn {
                            LabelItem(
                                title = stringResource(id = R.string.settings_extend_statusbarlyric),
                            ) {
                                navController.toUI(UI.Settings.LyricGetter)
                            }
                        }

                        // 6. App Updates & About
                        GroupSpacer()
                        ListHeader(stringResource(id = R.string.settings_others))
                        RoundColumn {
                            LabelItem(
                                title = if (isCheckingUpdate) "Checking for Updates..." else "Check for Updates",
                                desc = "Current version: v${AppUpdateChecker.CURRENT_VERSION}"
                            ) {
                                scope.launch {
                                    val release = AppUpdateChecker.checkForUpdates(forceShowEvenIfSkipped = true)
                                    if (release == null) {
                                        Toast.makeText(context, "LyricsFlow is up to date (v${AppUpdateChecker.CURRENT_VERSION})", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            Divider()
                            LabelItem(
                                title = stringResource(id = R.string.settings_others_about),
                            ) {
                                navController.toUI(UI.Settings.About)
                            }
                        }
                    }
                }
            }
        )

        // LyricsFlow Account Login / Signup / Profile Sheet
        if (showAuthSheet) {
            LyricsFlowAuthBottomSheet(
                onDismiss = { showAuthSheet = false }
            )
        }

        // Discord Account Linking Sheet (WebView + Token Input)
        if (showDiscordSheet) {
            DiscordLoginBottomSheet(
                onDismiss = { showDiscordSheet = false }
            )
        }

        // In-App Update Available Dialog (Update Now / Update Later / Don't Update)
        availableUpdate?.let { release ->
            UpdateAvailableBottomSheet(
                release = release,
                onUpdateNow = { AppUpdateChecker.openUpdateDownload(context, release) },
                onUpdateLater = { AppUpdateChecker.dismissForLater() },
                onSkipVersion = { AppUpdateChecker.skipVersion(release.tagName) }
            )
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsFlowAuthBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user by LyricsFlowAuthManager.currentUser

    var isSignupMode by remember { mutableStateOf(false) }
    var isVerifyStep by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF16161A),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (user.isLoggedIn) {
                Text(
                    text = "LyricsFlow Account",
                    fontFamily = SFProDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White
                )
                Text(
                    text = "Signed in as ${user.name} (${user.email})",
                    fontFamily = SFProDisplay,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFA233B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable {
                            scope.launch {
                                LyricsFlowAuthManager.logout()
                                Toast.makeText(context, "Signed out of LyricsFlow", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Sign Out",
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                Text(
                    text = when {
                        isVerifyStep -> "Verify Email Code"
                        isSignupMode -> "Create LyricsFlow Account"
                        else -> "Sign in to LyricsFlow"
                    },
                    fontFamily = SFProDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White
                )
                Text(
                    text = "Connect your trylyricsflow.com account to sync community TTML lyrics and cloud history.",
                    fontFamily = SFProDisplay,
                    fontSize = 13.5.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )

                if (isSignupMode && !isVerifyStep) {
                    AuthInputField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = "Display Name"
                    )
                }

                AuthInputField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    placeholder = "Email Address"
                )

                if (!isVerifyStep) {
                    AuthInputField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        placeholder = "Password",
                        isPassword = true
                    )
                } else {
                    AuthInputField(
                        value = codeInput,
                        onValueChange = { codeInput = it },
                        placeholder = "6-Digit Verification Code"
                    )
                }

                if (statusMessage.isNotBlank()) {
                    Text(
                        text = statusMessage,
                        fontFamily = SFProDisplay,
                        fontSize = 13.sp,
                        color = Color(0xFFFA233B)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFA233B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable(enabled = !isBusy) {
                            isBusy = true
                            statusMessage = ""
                            scope.launch {
                                when {
                                    isVerifyStep -> {
                                        val res = LyricsFlowAuthManager.verifyCode(emailInput, codeInput)
                                        isBusy = false
                                        res.onSuccess {
                                            Toast.makeText(context, "Welcome, ${it.name}!", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        }.onFailure { statusMessage = it.message ?: "Verification failed" }
                                    }
                                    isSignupMode -> {
                                        val res = LyricsFlowAuthManager.signup(nameInput, emailInput, passwordInput)
                                        isBusy = false
                                        res.onSuccess { state ->
                                            if (state == "signed_in") {
                                                onDismiss()
                                            } else {
                                                isVerifyStep = true
                                                statusMessage = "Enter the verification code sent to $emailInput"
                                            }
                                        }.onFailure { statusMessage = it.message ?: "Signup failed" }
                                    }
                                    else -> {
                                        val res = LyricsFlowAuthManager.login(emailInput, passwordInput)
                                        isBusy = false
                                        res.onSuccess {
                                            Toast.makeText(context, "Signed in as ${it.name}", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        }.onFailure { statusMessage = it.message ?: "Login failed" }
                                    }
                                }
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isBusy) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = when {
                                    isVerifyStep -> "Verify & Sign In"
                                    isSignupMode -> "Sign Up"
                                    else -> "Sign In"
                                },
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isSignupMode) "Already have an account? Sign In" else "Create an Account",
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFFFA233B),
                        modifier = Modifier.clickable {
                            isSignupMode = !isSignupMode
                            isVerifyStep = false
                            statusMessage = ""
                        }
                    )
                    Text(
                        text = "Continue with Discord",
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF5865F2),
                        modifier = Modifier.clickable {
                            scope.launch {
                                val url = LyricsFlowAuthManager.getOAuthUrl("discord")
                                    .ifBlank { "${com.lyricsflow.player.code.utils.lrc.LyricsFlowApi.API_BASE}/v1/auth/oauth/discord" }
                                startWeb(url, context)
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscordLoginBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var manualToken by remember { mutableStateOf("") }
    var showWebView by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF16161A),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Discord Rich Presence",
                fontFamily = SFProDisplay,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White
            )
            Text(
                text = "Broadcasts \"Listening to LyricsFlow Mobile\" with animated cover art and Apple Music / LyricsFlow buttons.",
                fontFamily = SFProDisplay,
                fontSize = 13.5.sp,
                color = Color.White.copy(alpha = 0.68f)
            )

            if (showWebView) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        view?.evaluateJavascript(
                                            "(function() { try { const i = document.createElement('iframe'); document.body.appendChild(i); return i.contentWindow.localStorage.token; } catch(e) { return ''; } })()"
                                        ) { rawToken ->
                                            val cleaned = rawToken?.trim()?.removeSurrounding("\"")?.removeSurrounding("\\\"").orEmpty()
                                            if (cleaned.length > 20 && cleaned != "null") {
                                                DiscordRpcManager.saveDiscordToken(cleaned)
                                                Toast.makeText(ctx, "Discord linked to LyricsFlow Mobile!", Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            }
                                        }
                                    }
                                }
                                loadUrl("https://discord.com/login")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF5865F2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { showWebView = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Sign in with Discord WebView",
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = Color.White
                        )
                    }
                }

                AuthInputField(
                    value = manualToken,
                    onValueChange = { manualToken = it },
                    placeholder = "Or paste Discord Token directly"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable(enabled = manualToken.isNotBlank()) {
                                DiscordRpcManager.saveDiscordToken(manualToken)
                                Toast.makeText(context, "Discord token saved!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Save Token", fontFamily = SFProDisplay, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = Color.White)
                        }
                    }

                    if (DiscordRpcManager.hasToken()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFA233B).copy(alpha = 0.22f),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable {
                                    DiscordRpcManager.unlinkDiscord()
                                    Toast.makeText(context, "Unlinked Discord", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("Unlink", fontFamily = SFProDisplay, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = Color(0xFFFA233B))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateAvailableBottomSheet(
    release: com.lyricsflow.player.code.updater.AppReleaseInfo,
    onUpdateNow: () -> Unit,
    onUpdateLater: () -> Unit,
    onSkipVersion: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onUpdateLater,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF16161A),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.SystemUpdate, null, tint = Color(0xFFFA233B), modifier = Modifier.size(28.dp))
                Column {
                    Text(
                        text = "Update Available",
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White
                    )
                    Text(
                        text = "${release.title} (v${release.tagName})",
                        fontFamily = SFProDisplay,
                        fontSize = 14.sp,
                        color = Color(0xFFFA233B)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = release.changelog,
                    fontFamily = SFProDisplay,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFA233B),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable(onClick = onUpdateNow)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Update Now", fontFamily = SFProDisplay, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable(onClick = onUpdateLater)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Update Later", fontFamily = SFProDisplay, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable(onClick = onSkipVersion)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Don't Update", fontFamily = SFProDisplay, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White.copy(alpha = 0.65f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AuthInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                fontFamily = SFProDisplay,
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.45f)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            textStyle = TextStyle(
                fontFamily = SFProDisplay,
                fontSize = 15.sp,
                color = Color.White
            ),
            cursorBrush = SolidColor(Color(0xFFFA233B)),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

fun safeStartActivity(context: Context, intent: Intent, options: Bundle?) {
    if (intent.resolveActivity(context.packageManager) != null) {
        ContextCompat.startActivity(context, intent, options)
    } else {
        Toast.makeText(
            context,
            context.getString(R.string.tip_intent_resolve_failed),
            Toast.LENGTH_SHORT
        ).show()
    }
}

fun startWeb(url: String, context: Context) {
    try {
        val uri: Uri =
            Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        safeStartActivity(context, intent, null)
    } catch (_: Exception) {
    }
}
