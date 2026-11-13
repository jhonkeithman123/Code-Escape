package codeescape.android.app

import android.app.Activity
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import codeescape.android.ui.AssetEndingScene
import codeescape.android.ui.AssetRunningTransition
import codeescape.core.content.GameConfig
import codeescape.core.content.LevelMetadata
import codeescape.core.content.StoryContent
import codeescape.core.flow.FlowState
import codeescape.core.flow.FlowTransitions
import kotlinx.coroutines.delay

@Composable
fun GameApp(activity: Activity) {
    val context = LocalContext.current
    val app = remember { GameAppState(context) }
    val nav = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> app.onResume()
                Lifecycle.Event.ON_PAUSE -> app.onPause()
                Lifecycle.Event.ON_DESTROY -> { /* handled by activity dispose */ }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        lifecycleOwner.lifecycle.addObserver(app.scheduler)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            lifecycleOwner.lifecycle.removeObserver(app.scheduler)
            app.onDestroy()
        }
    }

    // Drive NavController from app.screen (single source of truth in app state)
    LaunchedEffect(app.screen) {
        val route = routeFor(app.screen)
        val current = nav.currentBackStackEntry?.destination?.route
        if (current != route) {
            nav.navigate(route) {
                popUpTo(nav.graph.startDestinationId) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    BackHandler {
        when (app.screen) {
            FlowState.WELCOME -> activity.finish()
            FlowState.LOGIN, FlowState.REGISTER -> app.screen = FlowState.WELCOME
            FlowState.CHARACTER -> app.screen = FlowState.WELCOME
            FlowState.DASHBOARD -> activity.finish()
            FlowState.STORY_INTRO, FlowState.STORY_LEVEL2, FlowState.STORY_LEVEL3, FlowState.STORY_LEVEL4, FlowState.STORY_LEVEL5 -> { /* avoid skipping story in parity flow */ }
            FlowState.QUESTION -> {
                if (app.awaitingAdvance) app.advanceFeedbackNow()
                else app.openExitDialog()
            }
            FlowState.RUNNING_TRANSITION -> { /* ignore */ }
            FlowState.PROFILE -> app.backToDashboard()
            FlowState.COMPLETE, FlowState.GAMEOVER, FlowState.ENDING -> app.returnToDashboardFromResult()
            FlowState.LOGOUT -> app.returnToDashboardFromResult()
            else -> {}
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFF5AF5E6),
        secondary = Color(0xFF8AB4FF),
        background = Color(0xFF071522),
        surface = Color(0xFF10263D)
    )) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            NavHost(navController = nav, startDestination = routeFor(FlowState.WELCOME)) {
                composable("welcome") { WelcomeScreen(app) }
                composable("loading") { LoadingScreen(app) }
                composable("login") { LoginScreen(app) }
                composable("register") { RegisterScreen(app) }
                composable("character") { CharacterScreen(app) }
                composable("story") { StoryScreen(app) }
                composable("dashboard") { DashboardScreen(app) }
                composable("question") { QuestionScreen(app) }
                composable("running") { RunningScreen(app) }
                composable("profile") { ProfileScreen(app) }
                composable("complete") { CompleteScreen(app) }
                composable("gameover") { GameOverScreen(app) }
                composable("ending") { EndingScreen(app) }
                composable("logout") { ResultScreen(app, title = "Logged out", message = "See you next time!") }
            }
        }
    }
}

private fun routeFor(state: FlowState): String = FlowTransitions.routePath(state)

@Composable
fun BackgroundImage(name: String, modifier: Modifier = Modifier, alpha: Float = 1f) {
    val ctx = LocalContext.current
    val bmp = remember(name) {
        try { ctx.assets.open(name).use { BitmapFactory.decodeStream(it) } } catch (_: Exception) { null }
    }
    if (bmp != null) {
        Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop, alpha = alpha)
    }
}

@Composable
fun ScreenScaffold(bg: String? = null, overlayAlpha: Float = 0.45f, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        if (bg != null) BackgroundImage(bg, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xAA071522), Color(0xCC071522).copy(alpha = overlayAlpha.coerceIn(0.2f, 0.95f))))
        ))
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            content = content
        )
    }
}

@Composable
fun WelcomeScreen(app: GameAppState) {
    ScreenScaffold(bg = "castle.png", overlayAlpha = 0.35f) {
        Text("CODE ESCAPE", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF5AF5E6))
        Text("Shared core • Android (Compose + Canvas)", color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { app.screen = FlowState.LOGIN }, modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp)) { Text("Login") }
        OutlinedButton(onClick = { app.screen = FlowState.REGISTER }, modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp)) { Text("Register") }
    }
}

@Composable
fun LoadingScreen(app: GameAppState) {
    var progress by remember { mutableStateOf(0f) }
    val profile = app.currentProfile
    val isGirl = (profile?.character ?: "Boy").equals("Girl", ignoreCase = true)
    val variant = (profile?.outfitVariant ?: 0).coerceIn(0, 3)
    val context = LocalContext.current
    val frames by produceState<List<android.graphics.Bitmap>?>(initialValue = null, isGirl, variant) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val runFile = codeescape.android.render.SpriteAssets.runnerOutfitFile(isGirl, variant)
            codeescape.android.render.SpriteAssets.loadIsolatedWalkFrames(context, runFile)
                ?: codeescape.android.render.SpriteAssets.loadIsolatedWalkFrames(
                    context,
                    if (isGirl) "modern_girl_walk.png" else "modern_boy_walk.png"
                )
        }
    }
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (progress < 1f) {
            val elapsed = System.currentTimeMillis() - start
            progress = (elapsed / GameConfig.LOADING_DELAY_MS.toFloat()).coerceIn(0f, 1f)
            delay(16)
        }
        progress = 1f
        app.onLoadingFinished()
    }
    ScreenScaffold(bg = "loading.png", overlayAlpha = 0.5f) {
        Text("Loading assets...", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(140.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            val frameList = frames
            val frameIdx = if (!frameList.isNullOrEmpty()) {
                ((progress * frameList.size * 8).toInt()).coerceAtLeast(0) % frameList.size
            } else 0
            // Runner character synced to the same progress value as the loading bar
            androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val trackWidth = maxWidth
                val runnerHeight = 84.dp
                val runnerWidth = 46.dp
                val xOffset = (trackWidth - runnerWidth) * progress.coerceIn(0f, 1f)
                val bmp = frameList?.getOrNull(frameIdx)
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Loading character",
                        modifier = Modifier
                            .offset(x = xOffset, y = 0.dp)
                            .size(width = runnerWidth, height = runnerHeight)
                            .align(Alignment.BottomStart),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .offset(x = xOffset, y = 0.dp)
                            .size(width = runnerWidth, height = runnerHeight)
                            .align(Alignment.BottomStart)
                            .background(Color(0xFF69E5F5), RoundedCornerShape(10.dp))
                    )
                }
                // subtle track line
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color(0x33FFFFFF))
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        // Custom progress bar drawn from the same progress value as the runner (avoids indicator animation desync)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(8.dp)
                .background(Color(0x33FFFFFF), RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(Color(0xFF5AF5E6), RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
fun LoginScreen(app: GameAppState) {
    var user by rememberSaveable { mutableStateOf(app.currentUsername ?: "") }
    var pass by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    ScreenScaffold(bg = "castle.png", overlayAlpha = 0.5f) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC10263D)), shape = RoundedCornerShape(16.dp), modifier = Modifier.widthIn(max = 460.dp)) {
            Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Login", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                if (error != null) { Spacer(Modifier.height(8.dp)); Text(error!!, color = Color(0xFFFF8080)) }
                Spacer(Modifier.height(14.dp))
                Button(onClick = { error = app.login(user, pass) }, modifier = Modifier.fillMaxWidth()) { Text("Enter") }
                TextButton(onClick = { app.screen = FlowState.REGISTER }) { Text("Create account") }
                TextButton(onClick = { app.screen = FlowState.WELCOME }) { Text("Back") }
            }
        }
    }
}

@Composable
fun RegisterScreen(app: GameAppState) {
    var user by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    ScreenScaffold(bg = "castle.png", overlayAlpha = 0.5f) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC10263D)), shape = RoundedCornerShape(16.dp), modifier = Modifier.widthIn(max = 460.dp)) {
            Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Register", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                if (error != null) { Spacer(Modifier.height(8.dp)); Text(error!!, color = Color(0xFFFF8080)) }
                Spacer(Modifier.height(14.dp))
                Button(onClick = { error = app.register(user, email, pass) }, modifier = Modifier.fillMaxWidth()) { Text("Create & Continue") }
                TextButton(onClick = { app.screen = FlowState.LOGIN }) { Text("Back to login") }
            }
        }
    }
}

@Composable
fun CharacterScreen(app: GameAppState) {
    val p = app.currentProfile
    var isGirl by remember(p?.username) { mutableStateOf((p?.character ?: "Boy").equals("Girl", ignoreCase = true)) }
    var selectedVariant by remember(p?.username) { mutableStateOf((p?.outfitVariant ?: 0).coerceIn(0, 3)) }
    // Highlight follows selection for sensible preview/confirm behavior (preview shown only when a variant is highlighted/selected).
    var highlightedVariant by remember(p?.username) { mutableStateOf<Int?>((p?.outfitVariant ?: 0).coerceIn(0, 3)) }

    ScreenScaffold(bg = "level1.png", overlayAlpha = 0.5f) {
        Text("Choose Your Character", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // First section: vertical gender tabs/buttons at very left, then variant buttons.
            Column(
                modifier = Modifier.widthIn(min = 120.dp, max = 136.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Gender", color = Color(0xFF9FD8FF), fontSize = 13.sp)
                CharacterSelectButton(
                    text = "Girls",
                    selected = isGirl,
                    onClick = { isGirl = true },
                    modifier = Modifier.fillMaxWidth(),
                    compact = true
                )
                CharacterSelectButton(
                    text = "Boys",
                    selected = !isGirl,
                    onClick = { isGirl = false },
                    modifier = Modifier.fillMaxWidth(),
                    compact = true
                )
            }

            Column(
                modifier = Modifier
                    .widthIn(min = 132.dp, max = 150.dp)
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Variants", color = Color(0xFF9FD8FF), fontSize = 13.sp)
                (0..3).forEach { i ->
                    CharacterSelectButton(
                        text = "Variant ${i + 1}",
                        selected = selectedVariant == i,
                        highlighted = highlightedVariant == i,
                        onClick = {
                            selectedVariant = i
                            highlightedVariant = i
                        },
                        modifier = Modifier.fillMaxWidth(),
                        compact = true
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            // Second section: large rectangular preview area for the highlighted/selected variant model.
            Card(
                modifier = Modifier.weight(1f).heightIn(min = 320.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC10263D))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val previewVariant = highlightedVariant
                    if (previewVariant != null) {
                        val previewName = codeescape.android.render.SpriteAssets.standingOrVariantFile(isGirl, previewVariant)
                        val ctx = LocalContext.current
                        val bmp = remember(previewName) {
                            codeescape.android.render.SpriteAssets.loadBitmap(ctx, previewName)
                        }
                        if (bmp != null) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "${if (isGirl) "Girl" else "Boy"} • Variant ${previewVariant + 1}",
                                    color = Color(0xFFFFE08A),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(12.dp))
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Selected character preview",
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        } else {
                            Text(
                                "Preview unavailable for ${if (isGirl) "Girl" else "Boy"} Variant ${previewVariant + 1}",
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            "Highlight a variant to preview the character model",
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { app.saveCharacter(if (isGirl) "Girl" else "Boy", selectedVariant) },
            modifier = Modifier.fillMaxWidth(0.6f)
        ) { Text("Confirm") }
    }
}

@Composable
private fun CharacterSelectButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = selected,
    compact: Boolean = false
) {
    val container = when {
        selected -> Color(0xFF1D6F73)
        highlighted -> Color(0xFF1D4E6E)
        else -> Color(0xFF14324D)
    }
    val content = if (selected || highlighted) Color.White else Color.White.copy(alpha = 0.85f)
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        contentPadding = if (compact) PaddingValues(horizontal = 8.dp, vertical = 8.dp) else ButtonDefaults.ContentPadding
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
            fontSize = if (compact) 13.sp else 14.sp
        )
    }
}

@Composable
fun StoryScreen(app: GameAppState) {
    val page = when (app.screen) {
        FlowState.STORY_LEVEL2 -> StoryContent.LEVEL2
        FlowState.STORY_LEVEL3 -> StoryContent.LEVEL3
        FlowState.STORY_LEVEL4 -> StoryContent.LEVEL4
        FlowState.STORY_LEVEL5 -> StoryContent.LEVEL5
        else -> StoryContent.INTRO
    }
    DisposableEffect(page.screenId) {
        app.speak(page.body)
        onDispose { app.stopNarration() }
    }
    Box(Modifier.fillMaxSize()) {
        BackgroundImage(page.backgroundImage, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Color(0x88071522)))
        Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            Text(page.title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFE08A), textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC10263D)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    val range = app.storyHighlightRange
                    val bodyText = if (range != null) {
                        val s = range.first.coerceIn(0, page.body.length)
                        val e = range.second.coerceIn(s, page.body.length)
                        buildAnnotatedString {
                            append(page.body.substring(0, s))
                            withStyle(SpanStyle(color = Color(0xFFFFE08A), fontWeight = FontWeight.Bold, background = Color(0x335AF5E6))) {
                                append(page.body.substring(s, e))
                            }
                            append(page.body.substring(e))
                        }
                    } else buildAnnotatedString { append(page.body) }
                    Text(
                        bodyText,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        color = Color.White,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center
                    )
                    if (app.narration.isSupported) {
                        Text(
                            if (app.storyHighlightRange != null) "Narration playing (best-effort highlight)…" else "Narration ready (best-effort).",
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                            color = Color(0xFF9FD8FF),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = { app.continueFromStory() }, modifier = Modifier.fillMaxWidth(0.7f)) { Text(page.buttonText) }
            TextButton(onClick = { app.stopNarration() }) { Text("Stop narration") }
        }
    }
}

@Composable
fun DashboardScreen(app: GameAppState) {
    val p = app.currentProfile
    val session = app.engine.session
    val lifetimeXp = (p?.lifetimeXp ?: try { app.profileRepo.getLifetimeXp(app.currentUsername ?: "") } catch (_: Exception) { session.savedXpBeforeRun }).coerceAtLeast(0)
    val displayedXp = if (session.showZeroXpAfterCancelledRun) 0 else lifetimeXp
    ScreenScaffold(bg = "castle.png", overlayAlpha = 0.45f) {
        Text("CODE ESCAPE", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD54A))
        Text("WELCOME, ${(app.currentUsername ?: "PLAYER").uppercase()}", color = Color(0xFFF2EFE2), fontSize = 15.sp)
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xE6051C43)), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(p?.character ?: "BOY").uppercase()}", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = if ((p?.character ?: "Boy").equals("Girl", ignoreCase = true)) Color(0xFFFF8FD0) else Color(0xFF58DAFF))
                Spacer(Modifier.height(4.dp))
                Text("★  XP: $displayedXp", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54A))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("MAIN MENU", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54A))
        val menuWidth = Modifier.fillMaxWidth().widthIn(max = 420.dp)
        Button(onClick = { app.startLessonFromBeginning() }, modifier = menuWidth) { Text("▶  PLAY") }
        OutlinedButton(onClick = { app.openProfile() }, modifier = menuWidth) { Text("PROFILE") }
        OutlinedButton(
            onClick = { app.openLogoutDialog() },
            modifier = menuWidth,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9A9A))
        ) { Text("EXIT") }
    }

    // Desktop LOGOUT message screen parity (YES/NO confirm)
    if (app.logoutDialogShown) {
        AlertDialog(
            onDismissRequest = { app.dismissLogoutDialog() },
            title = { Text("LOGOUT", color = Color(0xFFFFD54A), fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to logout?\nYour XP will remain saved for this session.") },
            confirmButton = { Button(onClick = { app.confirmLogout() }) { Text("YES") } },
            dismissButton = { OutlinedButton(onClick = { app.dismissLogoutDialog() }) { Text("NO") } }
        )
    }
}

@Composable
fun ProfileScreen(app: GameAppState) {
    val p = app.currentProfile
    val session = app.engine.session
    ScreenScaffold(bg = "level2.png", overlayAlpha = 0.5f) {
        Text("Profile", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC10263D)), modifier = Modifier.fillMaxWidth(0.9f)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Player: ${p?.username ?: "-"}")
                Text("Email: ${p?.email ?: "-"}")
                Text("Character: ${p?.character ?: "-"} (variant ${(p?.outfitVariant ?: 0) + 1})")
                Text("Total XP: ${p?.lifetimeXp ?: 0}")
                Text("Correct Answers: ${session.correctAnswers}")
                Text("Current Level: ${session.level}")
                Text("Hearts: ${session.hearts}")
                Text("Run XP: ${session.xp}  •  Question ${session.questionIndex + 1}/${GameConfig.QUESTIONS_PER_LEVEL}", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { app.backToDashboard() }) { Text("Back to Dashboard") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionScreen(app: GameAppState) {
    val q = app.currentQuestion()
    val level = app.engine.level
    val meta = LevelMetadata.of(level)

    LaunchedEffect(app.screen, q.text, app.answered, app.awaitingAdvance, app.exitDialogShown, app.isResumed) {
        while (FlowTransitions.shouldPauseInteractiveQuestion(app.screen, app.answered, app.awaitingAdvance) && !app.exitDialogShown &&
            app.screen == FlowState.QUESTION && app.questionTimeLeft > 0
        ) {
            delay(1000)
            if (app.isResumed && !app.answered && !app.awaitingAdvance && !app.exitDialogShown && app.screen == FlowState.QUESTION) {
                app.questionTimeLeft -= 1
                if (app.questionTimeLeft <= 0) {
                    app.timeoutQuestion()
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        BackgroundImage(meta.backgroundImage, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Color(0x9910212F)))
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            @OptIn(ExperimentalFoundationApi::class)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { app.cheatCompleteCurrentStageForTesting() }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("LEVEL ${app.engine.level} • QUESTION ${app.engine.questionIndex + 1} / ${GameConfig.QUESTIONS_PER_LEVEL}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("❤ ${app.engine.hearts}   XP ${app.engine.xp}", color = Color(0xFFFFE08A), fontWeight = FontWeight.SemiBold)
                Text("${app.questionTimeLeft}s", color = if (app.questionTimeLeft <= 5) Color(0xFFFF8080) else Color(0xFF5AF5E6), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            val denom = app.questionTimeLimitSeconds(q).coerceAtLeast(1)
            LinearProgressIndicator(progress = { (app.questionTimeLeft.toFloat() / denom).coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xE610263D)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    Text(q.text ?: "", color = Color.White, fontSize = 18.sp, lineHeight = 24.sp)
                    Spacer(Modifier.height(14.dp))
                    val locked = app.answered || app.awaitingAdvance
                    if (q.syntax) {
                        OutlinedTextField(
                            value = app.syntaxInput,
                            onValueChange = { app.syntaxInput = it },
                            label = { Text("Type the exact answer/syntax") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !locked,
                            singleLine = false,
                            minLines = 2
                        )
                        Spacer(Modifier.height(8.dp))
                        val symbolKeys = listOf(
                            "<", ">", "=", "/", "{", "}", "(", ")", "[", "]",
                            "\"", "'", ";", ":", ".", ",", "!", "&", "|",
                            "+", "-", "*", "_", "#"
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            symbolKeys.forEach { sym ->
                                OutlinedButton(
                                    onClick = { app.syntaxInput += sym },
                                    enabled = !locked,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 34.dp)
                                ) {
                                    Text(sym, fontSize = 14.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { app.submitSyntax() }, enabled = !locked) { Text("Submit") }
                    } else {
                        val answers = q.answers ?: emptyArray()
                        val answerPairs = answers.mapIndexed { idx, ans -> idx to (ans ?: "") }.chunked(2)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            answerPairs.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { (idx, ans) ->
                                        val isCorrect = idx == q.correct
                                        val showResult = app.answered
                                        val color = when {
                                            !showResult -> Color(0xFF1D4E6E)
                                            isCorrect -> Color(0xFF2E7D32)
                                            else -> Color(0xFF5A3A3A)
                                        }
                                        Button(
                                            onClick = { app.submitChoice(idx) },
                                            enabled = !locked,
                                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = color,
                                                contentColor = Color.White,
                                                disabledContainerColor = color,
                                                disabledContentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                ans,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth(),
                                                color = Color.White
                                            )
                                        }
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    if (app.answered) {
                        Spacer(Modifier.height(8.dp))
                        Text(app.feedbackHeading, color = if (app.lastAnswerCorrect == true) Color(0xFF8CFF9B) else Color(0xFFFFA0A0), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = { app.openProfile() }) { Text("Profile") }
                OutlinedButton(onClick = { app.openExitDialog() }) { Text("Exit Run") }
            }
        }

        // Desktop showGamePopup parity: modal feedback with heading/big/detail + CONTINUE.
        if (app.awaitingAdvance && app.feedbackHeading.isNotBlank() && app.screen == FlowState.QUESTION) {
            val accent = if (app.lastAnswerCorrect == true) Color(0xFF8CFF9B) else Color(0xFFFF9A9A)
            AlertDialog(
                onDismissRequest = { /* modal parity: advance via CONTINUE or auto-fallback */ },
                title = { Text(app.feedbackHeading, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(app.feedbackBig, color = Color(0xFFFFD54A), fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(app.feedbackDetail, color = Color.White, textAlign = TextAlign.Center)
                    }
                },
                confirmButton = { Button(onClick = { app.advanceFeedbackNow() }) { Text("CONTINUE") } }
            )
        }

        // Desktop showGameExitDialog parity: confirm before abandoning the run.
        if (app.exitDialogShown && app.screen == FlowState.QUESTION) {
            AlertDialog(
                onDismissRequest = { app.dismissExitDialog() },
                title = { Text("EXIT GAME?", color = Color(0xFFFFD54A), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) },
                text = { Text("Your unfinished run will not be saved.", color = Color(0xFFF2EFE2)) },
                confirmButton = { Button(onClick = { app.dismissExitDialog() }) { Text("CONTINUE GAME") } },
                dismissButton = {
                    OutlinedButton(
                        onClick = { app.dismissExitDialog(); app.cancelRunAndDashboard() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9A9A))
                    ) { Text("EXIT GAME") }
                }
            )
        }
    }
}

@Composable
fun RunningScreen(app: GameAppState) {
    val p = app.currentProfile
    val isGirl = (p?.character ?: "Boy").equals("Girl", ignoreCase = true)
    val variant = (p?.outfitVariant ?: 0).coerceIn(0, 3)
    Box(Modifier.fillMaxSize()) {
        AssetRunningTransition(isGirl = isGirl, variantIndex = variant, onFinished = { app.onRunningFinished() })
        Column(Modifier.align(Alignment.TopCenter).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (app.runningReason == GameAppState.RunningReason.GAME_COMPLETE) "Final Run..." else "Running...", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Level ${app.engine.level} • ${p?.character ?: "Hero"}", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
        }
    }
}

@Composable
fun EndingScreen(app: GameAppState) {
    val p = app.currentProfile
    val isGirl = (p?.character ?: "Boy").equals("Girl", ignoreCase = true)
    val variant = (p?.outfitVariant ?: 0).coerceIn(0, 3)
    Box(Modifier.fillMaxSize()) {
        AssetEndingScene(
            isGirl = isGirl,
            variantIndex = variant,
            initialChestOpened = app.endingChestOpened || app.endingTreasureDialogShown || app.endingXpDialogShown,
            onOpened = { app.onEndingChestOpened() }
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                .background(Color(0x9908121E), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("VICTORY POSE READY!", color = Color(0xFFFFE08A), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, textAlign = TextAlign.Center)
            Text(
                if (app.endingChestOpened) "Treasure unlocked" else "Tap the key to open the vault",
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        if (app.endingTreasureDialogShown) {
            val ctx = LocalContext.current
            val noteBmp = remember("treasure_note.png") {
                try { ctx.assets.open("treasure_note.png").use { BitmapFactory.decodeStream(it) } } catch (_: Exception) { null }
            }
            AlertDialog(
                onDismissRequest = { /* modal parity */ },
                confirmButton = { Button(onClick = { app.onTreasureDialogContinue() }) { Text("CONTINUE") } },
                title = { Text("YOU FOUND THE FINAL TREASURE!", textAlign = TextAlign.Center) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "The vault opens and reveals the final note of your escape.",
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                        Spacer(Modifier.height(10.dp))
                        if (noteBmp != null) {
                            Image(
                                bitmap = noteBmp.asImageBitmap(),
                                contentDescription = "Treasure note",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp, max = 420.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            BackgroundImage("treasure_note.png", modifier = Modifier.fillMaxWidth().height(180.dp), alpha = 1f)
                        }
                    }
                }
            )
        }
        if (app.endingXpDialogShown) {
            val lifetimeXp = (app.currentProfile?.lifetimeXp
                ?: app.profileRepo.getLifetimeXp(app.currentUsername ?: "")).coerceAtLeast(0)
            val finalRunXp = app.engine.xp.coerceAtLeast(app.endingFinalXp).coerceAtLeast(0)
            AlertDialog(
                onDismissRequest = { /* modal parity */ },
                confirmButton = { Button(onClick = { app.onEndingXpMenu() }) { Text("MENU") } },
                dismissButton = { OutlinedButton(onClick = { app.onEndingXpRetry() }) { Text("TRY AGAIN") } },
                title = { Text("LEVELS COMPLETED!", color = Color(0xFFFFD54A), fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "XP collected this login: $lifetimeXp",
                            color = Color(0xFFF2EFE2),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Final run XP: $finalRunXp",
                            color = Color(0xFFFFE08A),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Your progress has been saved. You can return to the menu or try again.",
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun ResultScreen(app: GameAppState, title: String, message: String) {
    val bg = when (app.screen) {
        FlowState.GAMEOVER -> "level5.png"
        FlowState.COMPLETE, FlowState.ENDING -> "castle.png"
        else -> "castle.png"
    }
    ScreenScaffold(bg = bg, overlayAlpha = 0.55f) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = if (app.screen == FlowState.GAMEOVER) Color(0xFFFF9A9A) else Color(0xFF8CFF9B))
        Spacer(Modifier.height(10.dp))
        Text(message, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Final XP: ${app.engine.xp}", color = Color(0xFFFFD54A), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Button(onClick = { app.returnToDashboardFromResult() }, modifier = Modifier.fillMaxWidth(0.6f)) { Text("Dashboard") }
    }
}

/** Desktop MessageScreen parity: "LESSON COMPLETE!" / "You finished all 5 levels!" / Final XP. */
@Composable
fun CompleteScreen(app: GameAppState) {
    ScreenScaffold(bg = "castle.png", overlayAlpha = 0.55f) {
        Text("LESSON COMPLETE!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF8CFF9B), textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text("You finished all 5 levels!", color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Final XP: ${app.engine.xp}", color = Color(0xFFFFD54A), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Button(onClick = { app.startLessonFromBeginning() }, modifier = Modifier.fillMaxWidth(0.7f)) { Text("TAKE ANOTHER LESSON") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { app.returnToDashboardFromResult() }, modifier = Modifier.fillMaxWidth(0.7f)) { Text("HOME") }
    }
}

/** Desktop MessageScreen parity: "GAME OVER" / "You ran out of hearts." / Final XP. */
@Composable
fun GameOverScreen(app: GameAppState) {
    ScreenScaffold(bg = "level5.png", overlayAlpha = 0.55f) {
        Text("GAME OVER", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF9A9A))
        Spacer(Modifier.height(10.dp))
        Text(app.resultMessage.ifBlank { "You ran out of hearts." }, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Final XP: ${app.engine.xp}", color = Color(0xFFFFD54A), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Button(onClick = { app.startLessonFromBeginning() }, modifier = Modifier.fillMaxWidth(0.7f)) { Text("TRY AGAIN") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { app.returnToDashboardFromResult() }, modifier = Modifier.fillMaxWidth(0.7f)) { Text("HOME") }
    }
}
