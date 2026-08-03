package com.wishtrace.app.ui.screens.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishtrace.app.R
import com.wishtrace.app.ui.WishTraceTestTags
import com.wishtrace.app.ui.components.DimensionalAsset
import com.wishtrace.app.ui.components.PrimaryAction
import com.wishtrace.app.ui.components.WishTraceWordmark
import com.wishtrace.app.ui.components.rememberWishTraceMotionEnabled
import com.wishtrace.app.ui.theme.BrandBlue
import com.wishtrace.app.ui.theme.BrandIndigo
import com.wishtrace.app.ui.theme.BrandIndigoPressed
import com.wishtrace.app.ui.theme.BlueSurface
import com.wishtrace.app.ui.theme.Ink
import com.wishtrace.app.ui.theme.LavenderSurface
import com.wishtrace.app.ui.theme.OutlineCool
import com.wishtrace.app.ui.theme.SurfaceWhite
import com.wishtrace.app.ui.theme.WishTraceTheme
import kotlinx.coroutines.launch

private enum class OnboardingMoment {
    PROMISE,
    DATE,
    CLUES,
    GIFT,
    CONTROL,
}

private data class OnboardingPage(
    val title: String,
    val supportingText: String,
    val moment: OnboardingMoment,
)

private val onboardingPages = listOf(
    OnboardingPage(
        title = "Thoughtful, right on time.",
        supportingText = "Remember the moment. Find the gift.",
        moment = OnboardingMoment.PROMISE,
    ),
    OnboardingPage(
        title = "Never miss their moment.",
        supportingText = "Keep important dates close.",
        moment = OnboardingMoment.DATE,
    ),
    OnboardingPage(
        title = "The little clues matter.",
        supportingText = "Save what lights them up—and what doesn't.",
        moment = OnboardingMoment.CLUES,
    ),
    OnboardingPage(
        title = "Watch the right gift emerge.",
        supportingText = "Weak matches fall away.",
        moment = OnboardingMoment.GIFT,
    ),
    OnboardingPage(
        title = "You make the final call.",
        supportingText = "Review every detail. Stay in control.",
        moment = OnboardingMoment.CONTROL,
    ),
)

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex

    Scaffold(
        modifier = modifier.testTag(WishTraceTestTags.OnboardingScreen),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PageIndicator(
                        pageCount = onboardingPages.size,
                        selectedPage = pagerState.currentPage,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    PrimaryAction(
                        text = when {
                            isLastPage -> "Set up WishTrace"
                            pagerState.currentPage == 0 -> "See how it works"
                            else -> "Continue"
                        },
                        onClick = {
                            if (isLastPage) {
                                onGetStarted()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(WishTraceTestTags.ContinueFromWelcome),
                        trailingContent = { Text("→") },
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WishTraceWordmark(
                    modifier = Modifier.weight(1f),
                    markSize = 30.dp,
                )
                if (!isLastPage) {
                    TextButton(
                        onClick = onGetStarted,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(
                            text = "Skip",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { pageIndex ->
                OnboardingPageContent(
                    page = onboardingPages[pageIndex],
                    active = pagerState.currentPage == pageIndex,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 10.dp),
    ) {
        // A short portrait handset is still a vertical canvas. The previous
        // height-only check treated common phones as landscape and squeezed
        // the scene and copy into two half-width columns.
        val useWideLayout = maxWidth >= 700.dp && maxWidth > maxHeight
        if (useWideLayout) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MomentScene(
                    moment = page.moment,
                    active = active,
                    modifier = Modifier
                        .weight(1.08f)
                        .fillMaxSize(),
                )
                PageCopy(
                    page = page,
                    modifier = Modifier.weight(0.92f),
                    textAlign = TextAlign.Start,
                    horizontalAlignment = Alignment.Start,
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MomentScene(
                    moment = page.moment,
                    active = active,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 300.dp, max = 440.dp),
                )
                PageCopy(
                    page = page,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 4.dp),
                    textAlign = TextAlign.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                )
            }
        }
    }
}

@Composable
private fun PageCopy(
    page: OnboardingPage,
    textAlign: TextAlign,
    horizontalAlignment: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = page.title,
            modifier = Modifier.semantics { heading() },
            color = Ink,
            textAlign = textAlign,
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = page.supportingText,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = textAlign,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun MomentScene(
    moment: OnboardingMoment,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val motionEnabled = rememberWishTraceMotionEnabled()
    val entrance by animateFloatAsState(
        targetValue = if (active || !motionEnabled) 1f else 0.82f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 280f),
        label = "onboarding scene entrance",
    )
    val idleTransition = rememberInfiniteTransition(label = "onboarding idle")
    val idleOffset by idleTransition.animateFloat(
        initialValue = -5f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1_900,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "onboarding scene float",
    )
    val floatOffset = if (motionEnabled && active) idleOffset else 0f

    val heroDrawable = when (moment) {
        OnboardingMoment.PROMISE, OnboardingMoment.GIFT, OnboardingMoment.CONTROL ->
            R.drawable.wishtrace_gift_3d
        OnboardingMoment.DATE -> R.drawable.wishtrace_calendar_3d
        OnboardingMoment.CLUES -> R.drawable.wishtrace_message_3d
    }
    val heroEyebrow = when (moment) {
        OnboardingMoment.PROMISE -> "THOUGHTFUL"
        OnboardingMoment.DATE -> "NEXT MOMENT"
        OnboardingMoment.CLUES -> "GIFT DNA"
        OnboardingMoment.GIFT -> "THE MATCH"
        OnboardingMoment.CONTROL -> "YOUR CALL"
    }
    val firstIcon = when (moment) {
        OnboardingMoment.PROMISE -> Icons.Rounded.Favorite
        OnboardingMoment.DATE -> Icons.Rounded.CalendarMonth
        OnboardingMoment.CLUES -> Icons.Rounded.SportsEsports
        OnboardingMoment.GIFT -> Icons.Rounded.Lightbulb
        OnboardingMoment.CONTROL -> Icons.Rounded.Shield
    }
    val firstValue = when (moment) {
        OnboardingMoment.PROMISE -> "For them"
        OnboardingMoment.DATE -> "Saved"
        OnboardingMoment.CLUES -> "Loves"
        OnboardingMoment.GIFT -> "Best fit"
        OnboardingMoment.CONTROL -> "Approve"
    }
    val secondIcon = when (moment) {
        OnboardingMoment.PROMISE -> Icons.Rounded.CalendarMonth
        OnboardingMoment.DATE -> Icons.Rounded.Favorite
        OnboardingMoment.CLUES -> Icons.Rounded.Shield
        OnboardingMoment.GIFT -> Icons.Rounded.Shield
        OnboardingMoment.CONTROL -> Icons.Rounded.CheckCircle
    }
    val secondValue = when (moment) {
        OnboardingMoment.PROMISE -> "On time"
        OnboardingMoment.DATE -> "Close"
        OnboardingMoment.CLUES -> "Avoids"
        OnboardingMoment.GIFT -> "Live facts"
        OnboardingMoment.CONTROL -> "Verified"
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                alpha = entrance
                scaleX = entrance
                scaleY = entrance
            }
            .clearAndSetSemantics {
                contentDescription = when (moment) {
                    OnboardingMoment.PROMISE ->
                        "A wrapped gift and heart meeting at the right moment"

                    OnboardingMoment.DATE ->
                        "A birthday date held close with a heart and a gift"

                    OnboardingMoment.CLUES ->
                        "A heart surrounded by small clues about what someone loves"

                    OnboardingMoment.GIFT ->
                        "Saved clues and a date coming together as a wrapped gift"

                    OnboardingMoment.CONTROL ->
                        "A reviewed gift with a clear approval check"
                }
            },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier
                    .weight(1.28f)
                    .fillMaxHeight(),
                color = LavenderSurface,
                contentColor = Ink,
                shape = RoundedCornerShape(30.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = BrandIndigo.copy(alpha = 0.07f),
                            radius = size.minDimension * 0.34f,
                            center = Offset(size.width * 0.5f, size.height * 0.53f),
                        )
                        drawCircle(
                            color = BrandBlue.copy(alpha = 0.12f),
                            radius = size.minDimension * 0.42f,
                            center = Offset(size.width * 0.5f, size.height * 0.53f),
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                    Text(
                        text = heroEyebrow,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(18.dp),
                        color = BrandIndigo,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    DimensionalAsset(
                        drawableRes = heroDrawable,
                        description = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(188.dp)
                            .graphicsLayer {
                                translationY = floatOffset
                            },
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(0.82f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OnboardingInfoTile(
                    icon = firstIcon,
                    value = firstValue,
                    containerColor = BrandIndigo,
                    contentColor = SurfaceWhite,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                OnboardingInfoTile(
                    icon = secondIcon,
                    value = secondValue,
                    containerColor = BlueSurface,
                    contentColor = Ink,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun OnboardingInfoTile(
    icon: ImageVector,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(24.dp),
    ) {
        Box(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.12f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(76.dp),
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(28.dp),
            )
            Text(
                text = value,
                modifier = Modifier.align(Alignment.BottomStart),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SceneBackdrop(
    moment: OnboardingMoment,
    active: Boolean,
) {
    val traceProgress by animateFloatAsState(
        targetValue = if (active) 1f else 0.35f,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "onboarding trace",
    )
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
    ) {
        val panelTop = size.height * 0.05f
        val panelHeight = size.height * 0.9f
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = when (moment) {
                    OnboardingMoment.PROMISE -> listOf(LavenderSurface, BlueSurface)
                    OnboardingMoment.DATE -> listOf(BlueSurface, LavenderSurface)
                    OnboardingMoment.CLUES -> listOf(LavenderSurface, SurfaceWhite)
                    OnboardingMoment.GIFT -> listOf(LavenderSurface, BlueSurface)
                    OnboardingMoment.CONTROL -> listOf(BlueSurface, SurfaceWhite)
                },
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
            topLeft = Offset(0f, panelTop),
            size = Size(size.width, panelHeight),
            cornerRadius = CornerRadius(42.dp.toPx()),
        )
        drawCircle(
            color = BrandBlue.copy(alpha = 0.09f),
            radius = size.minDimension * 0.28f,
            center = Offset(size.width * 0.12f, size.height * 0.20f),
        )
        drawCircle(
            color = BrandIndigo.copy(alpha = 0.08f),
            radius = size.minDimension * 0.32f,
            center = Offset(size.width * 0.92f, size.height * 0.83f),
        )
        val path = Path().apply {
            moveTo(size.width * 0.16f, size.height * 0.73f)
            cubicTo(
                size.width * 0.31f,
                size.height * 0.42f,
                size.width * 0.70f,
                size.height * 0.75f,
                size.width * (0.84f * traceProgress),
                size.height * 0.31f,
            )
        }
        drawPath(
            path = path,
            color = BrandIndigo.copy(alpha = 0.22f),
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
            ),
        )
    }
}

@Composable
private fun PromiseMoment(
    active: Boolean,
    floatOffset: Float,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = SurfaceWhite.copy(alpha = 0.82f),
                radius = size.minDimension * 0.31f,
                center = Offset(size.width * 0.5f, size.height * 0.48f),
            )
            drawCircle(
                color = BrandIndigo.copy(alpha = 0.08f),
                radius = size.minDimension * 0.38f,
                center = Offset(size.width * 0.5f, size.height * 0.48f),
                style = Stroke(width = 2.dp.toPx()),
            )
            listOf(
                Offset(size.width * 0.18f, size.height * 0.54f),
                Offset(size.width * 0.77f, size.height * 0.24f),
                Offset(size.width * 0.83f, size.height * 0.67f),
                Offset(size.width * 0.30f, size.height * 0.24f),
            ).forEachIndexed { index, point ->
                drawCircle(
                    color = if (index % 2 == 0) BrandIndigo else BrandBlue,
                    radius = (if (index % 2 == 0) 4.dp else 3.dp).toPx(),
                    center = point,
                )
            }
        }
        OrbitIconTile(
            icon = Icons.Rounded.CalendarMonth,
            active = active,
            delayMillis = 40,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 48.dp)
                .graphicsLayer { rotationZ = -6f },
        )
        OrbitIconTile(
            icon = Icons.Rounded.Person,
            active = active,
            delayMillis = 120,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 58.dp)
                .graphicsLayer { rotationZ = 5f },
        )
        DimensionalAsset(
            drawableRes = R.drawable.wishtrace_gift_3d,
            description = null,
            modifier = Modifier
                .size(220.dp)
                .offset(y = floatOffset.dp),
        )
        DimensionalAsset(
            drawableRes = R.drawable.wishtrace_message_3d,
            description = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 20.dp, top = 52.dp)
                .size(62.dp)
                .offset(y = (-floatOffset * 0.6f).dp)
                .graphicsLayer { rotationZ = 5f },
        )
        OrbitIconTile(
            icon = Icons.Rounded.Favorite,
            active = active,
            delayMillis = 200,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp)
                .graphicsLayer { rotationZ = 4f },
        )
        FloatingBadge(
            text = "Right on time",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 22.dp, bottom = 48.dp)
                .graphicsLayer { rotationZ = -3f },
        )
    }
}

@Composable
private fun DateMoment(
    active: Boolean,
    floatOffset: Float,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        FloatingBadge(
            text = "AUG",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 56.dp)
                .graphicsLayer { rotationZ = -4f },
        )
        DimensionalAsset(
            drawableRes = R.drawable.wishtrace_calendar_3d,
            description = null,
            modifier = Modifier
                .size(176.dp)
                .offset(y = (floatOffset - 18f).dp),
        )
        FloatingBadge(
            text = "12 days",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 58.dp, end = 22.dp)
                .graphicsLayer { rotationZ = 4f },
        )
        DimensionalAsset(
            drawableRes = R.drawable.wishtrace_message_3d,
            description = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 82.dp)
                .size(62.dp)
                .offset(y = (-floatOffset * 0.5f).dp),
        )
        DimensionalAsset(
            drawableRes = R.drawable.wishtrace_gift_3d,
            description = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 78.dp)
                .size(66.dp)
                .graphicsLayer { rotationZ = -5f },
        )
        DateRail(
            active = active,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp),
        )
    }
}

@Composable
private fun ClueMoment(
    active: Boolean,
    floatOffset: Float,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        SavedNoteCard(
            active = active,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-42).dp, y = 28.dp)
                .graphicsLayer { rotationZ = -4f },
        )
        ClueToken(
            text = "Cozy games",
            active = active,
            delayMillis = 40,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 68.dp)
                .graphicsLayer { rotationZ = -4f },
        )
        ClueToken(
            text = "Live music",
            active = active,
            delayMillis = 120,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 108.dp)
                .graphicsLayer { rotationZ = 4f },
        )
        ClueToken(
            text = "Books",
            active = active,
            delayMillis = 200,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 22.dp, bottom = 54.dp)
                .graphicsLayer { rotationZ = 3f },
        )
        ClueToken(
            text = "Quiet weekends",
            active = active,
            delayMillis = 260,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp)
                .graphicsLayer { rotationZ = -2f },
        )
        ClueToken(
            text = "No clutter",
            active = active,
            delayMillis = 320,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .offset(y = 80.dp)
                .graphicsLayer { rotationZ = 3f },
        )
        DimensionalAsset(
            drawableRes = R.drawable.wishtrace_message_3d,
            description = null,
            modifier = Modifier
                .size(152.dp)
                .offset(x = 62.dp, y = (floatOffset - 8f).dp),
        )
        OrbitIconTile(
            icon = Icons.Rounded.Lightbulb,
            active = active,
            delayMillis = 360,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp)
                .offset(y = 80.dp),
        )
    }
}

@Composable
private fun GiftMoment(
    active: Boolean,
    floatOffset: Float,
) {
    val pop by animateFloatAsState(
        targetValue = if (active) 1f else 0.72f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 250f),
        label = "gift pop",
    )
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        SparkleField(active = active)
        DimensionalAsset(
            drawableRes = R.drawable.wishtrace_calendar_3d,
            description = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 44.dp)
                .size(54.dp)
                .graphicsLayer { rotationZ = -7f },
        )
        DimensionalAsset(
            drawableRes = R.drawable.wishtrace_message_3d,
            description = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 72.dp)
                .size(56.dp)
                .graphicsLayer { rotationZ = 7f },
        )
        CandidateTile(
            icon = Icons.Rounded.SportsEsports,
            label = "Gaming",
            selected = true,
            active = active,
            delayMillis = 80,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .offset(y = (-26).dp)
                .graphicsLayer { rotationZ = -3f },
        )
        CandidateTile(
            icon = Icons.Rounded.MusicNote,
            label = "Music",
            selected = false,
            active = active,
            delayMillis = 150,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .offset(y = (-46).dp)
                .graphicsLayer { rotationZ = 3f },
        )
        CandidateTile(
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            label = "Books",
            selected = false,
            active = active,
            delayMillis = 220,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 34.dp, bottom = 74.dp)
                .graphicsLayer { rotationZ = 4f },
        )
        DimensionalAsset(
            drawableRes = R.drawable.wishtrace_gift_3d,
            description = null,
            modifier = Modifier
                .size(198.dp)
                .offset(y = floatOffset.dp)
                .graphicsLayer {
                    scaleX = pop
                    scaleY = pop
                },
        )
        FloatingBadge(
            text = "Comes together",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 42.dp),
        )
    }
}

@Composable
private fun ControlMoment(
    active: Boolean,
    floatOffset: Float,
) {
    val cardScale by animateFloatAsState(
        targetValue = if (active) 1f else 0.86f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 260f),
        label = "review card entrance",
    )
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.80f)
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    rotationZ = -1.5f
                },
            color = SurfaceWhite,
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = OutlineCool,
            ),
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(88.dp),
                        color = LavenderSurface,
                        shape = RoundedCornerShape(22.dp),
                    ) {
                        DimensionalAsset(
                            drawableRes = R.drawable.wishtrace_gift_3d,
                            description = null,
                            modifier = Modifier
                                .padding(7.dp)
                                .offset(y = floatOffset.dp),
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Text(
                            text = "Your review",
                            color = Ink,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(OutlineCool),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.58f)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(BlueSurface),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ReviewFact(
                        icon = Icons.Rounded.CalendarMonth,
                        label = "Timing",
                        modifier = Modifier.weight(1f),
                    )
                    ReviewFact(
                        icon = Icons.Rounded.CheckCircle,
                        label = "Budget",
                        modifier = Modifier.weight(1f),
                    )
                    ReviewFact(
                        icon = Icons.Rounded.Favorite,
                        label = "Note",
                        modifier = Modifier.weight(1f),
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = LavenderSurface,
                    contentColor = BrandIndigoPressed,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "✓",
                            color = BrandIndigo,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Nothing moves without you",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        OrbitIconTile(
            icon = Icons.Rounded.Shield,
            active = active,
            delayMillis = 180,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 20.dp, top = 48.dp)
                .graphicsLayer { rotationZ = 7f },
        )
        OrbitIconTile(
            icon = Icons.Rounded.Favorite,
            active = active,
            delayMillis = 260,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 22.dp, bottom = 58.dp)
                .graphicsLayer { rotationZ = -5f },
        )
    }
}

@Composable
private fun OrbitIconTile(
    icon: ImageVector,
    active: Boolean,
    delayMillis: Int,
    modifier: Modifier = Modifier,
) {
    val visibility by animateFloatAsState(
        targetValue = if (active) 1f else 0.55f,
        animationSpec = tween(
            durationMillis = 420,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "onboarding icon tile",
    )
    Surface(
        modifier = modifier
            .size(52.dp)
            .graphicsLayer {
                alpha = visibility
                scaleX = 0.78f + (visibility * 0.22f)
                scaleY = 0.78f + (visibility * 0.22f)
            },
        color = SurfaceWhite,
        contentColor = BrandIndigo,
        shape = RoundedCornerShape(17.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = OutlineCool.copy(alpha = 0.75f),
        ),
        shadowElevation = 5.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = BrandIndigo,
            )
        }
    }
}

@Composable
private fun DateRail(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val entrance by animateFloatAsState(
        targetValue = if (active) 1f else 0.72f,
        animationSpec = tween(460, delayMillis = 140, easing = FastOutSlowInEasing),
        label = "date rail entrance",
    )
    Surface(
        modifier = modifier
            .graphicsLayer {
                alpha = entrance
                scaleX = entrance
                scaleY = entrance
            },
        color = SurfaceWhite,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineCool),
        shadowElevation = 5.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("7", "8", "9", "10", "11").forEachIndexed { index, day ->
                Surface(
                    modifier = Modifier.size(38.dp),
                    color = if (index == 4) BrandIndigo else BlueSurface,
                    contentColor = if (index == 4) SurfaceWhite else BrandIndigoPressed,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedNoteCard(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val entrance by animateFloatAsState(
        targetValue = if (active) 1f else 0.75f,
        animationSpec = tween(500, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "saved note entrance",
    )
    Surface(
        modifier = modifier
            .size(width = 214.dp, height = 142.dp)
            .graphicsLayer {
                alpha = entrance
                scaleX = entrance
                scaleY = entrance
            },
        color = SurfaceWhite,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineCool),
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = BrandBlue,
                )
                Text(
                    text = "Saved clue",
                    color = Ink,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            listOf(1f, 0.82f, 0.56f).forEach { fraction ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(if (fraction == 1f) OutlineCool else BlueSurface),
                )
            }
        }
    }
}

@Composable
private fun CandidateTile(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    active: Boolean,
    delayMillis: Int,
    modifier: Modifier = Modifier,
) {
    val emphasis by animateFloatAsState(
        targetValue = when {
            !active -> 0.72f
            selected -> 1f
            else -> 0.62f
        },
        animationSpec = tween(
            durationMillis = 520,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "candidate $label",
    )
    Surface(
        modifier = modifier
            .size(width = 94.dp, height = 74.dp)
            .graphicsLayer {
                alpha = if (selected) emphasis else 0.45f + (emphasis * 0.55f)
                scaleX = 0.78f + (emphasis * 0.22f)
                scaleY = 0.78f + (emphasis * 0.22f)
            },
        color = SurfaceWhite,
        contentColor = if (selected) BrandIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) BrandIndigo.copy(alpha = 0.45f) else OutlineCool,
        ),
        shadowElevation = if (selected) 7.dp else 3.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = if (selected) "✓" else "×",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 8.dp),
                color = if (selected) BrandIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ReviewFact(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = BlueSurface,
        contentColor = BrandIndigoPressed,
        shape = RoundedCornerShape(13.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = BrandBlue,
            )
            Text(
                text = label,
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SparkleField(active: Boolean) {
    val sparkleScale by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(480, delayMillis = 180, easing = FastOutSlowInEasing),
        label = "gift celebration",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val points = listOf(
            Offset(size.width * 0.17f, size.height * 0.37f),
            Offset(size.width * 0.80f, size.height * 0.31f),
            Offset(size.width * 0.14f, size.height * 0.65f),
            Offset(size.width * 0.86f, size.height * 0.64f),
            Offset(size.width * 0.62f, size.height * 0.19f),
        )
        points.forEachIndexed { index, point ->
            val radius = (if (index % 2 == 0) 5.dp else 3.dp).toPx() * sparkleScale
            drawCircle(
                color = if (index % 2 == 0) BrandIndigo else BrandBlue,
                radius = radius,
                center = point,
            )
        }
    }
}

@Composable
private fun ClueToken(
    text: String,
    active: Boolean,
    delayMillis: Int,
    modifier: Modifier = Modifier,
) {
    val visibility by animateFloatAsState(
        targetValue = if (active) 1f else 0.45f,
        animationSpec = tween(
            durationMillis = 420,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "clue token $text",
    )
    Surface(
        modifier = modifier.graphicsLayer {
            alpha = visibility
            scaleX = 0.82f + (visibility * 0.18f)
            scaleY = 0.82f + (visibility * 0.18f)
        },
        color = SurfaceWhite,
        contentColor = BrandIndigoPressed,
        shape = CircleShape,
        shadowElevation = 4.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun FloatingBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = SurfaceWhite,
        contentColor = BrandIndigoPressed,
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineCool.copy(alpha = 0.75f)),
        shadowElevation = 4.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    selectedPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.semantics {
            contentDescription = "Page ${selectedPage + 1} of $pageCount"
        },
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val width by animateFloatAsState(
                targetValue = if (index == selectedPage) 24f else 7f,
                animationSpec = tween(220),
                label = "onboarding page indicator",
            )
            Box(
                modifier = Modifier
                    .height(7.dp)
                    .size(width = width.dp, height = 7.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == selectedPage) BrandIndigo else OutlineCool,
                    ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun WelcomePreview() {
    WishTraceTheme {
        WelcomeScreen(onGetStarted = {})
    }
}
