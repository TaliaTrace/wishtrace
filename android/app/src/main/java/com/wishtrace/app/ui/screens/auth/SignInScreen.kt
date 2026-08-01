package com.wishtrace.app.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wishtrace.app.R
import com.wishtrace.app.data.AuthRepository
import com.wishtrace.app.data.GoogleCredentialClient
import com.wishtrace.app.domain.AppSession
import com.wishtrace.app.ui.components.DimensionalAsset
import com.wishtrace.app.ui.components.ErrorBanner
import com.wishtrace.app.ui.components.ScreenTopBar
import com.wishtrace.app.ui.components.SecondaryAction
import com.wishtrace.app.ui.theme.BlueSurface
import com.wishtrace.app.ui.theme.Ink
import com.wishtrace.app.ui.theme.OutlineCool
import kotlinx.coroutines.launch

private sealed interface SignInState {
    data object Idle : SignInState
    data object Working : SignInState
    data class Error(val message: String) : SignInState
}

@Composable
fun SignInRoute(
    credentialClient: GoogleCredentialClient,
    authRepository: AuthRepository,
    webClientId: String,
    onSignedIn: (AppSession) -> Unit,
    onContinueLocal: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<SignInState>(SignInState.Idle) }
    var automaticAttempted by rememberSaveable { mutableStateOf(false) }

    fun requestGoogle(isAutomatic: Boolean) {
        if (state == SignInState.Working) return
        if (activity == null || webClientId.isBlank()) {
            if (!isAutomatic) {
                state = SignInState.Error(
                    "Google sign-in needs the WishTrace OAuth client configuration.",
                )
            }
            return
        }
        scope.launch {
            state = SignInState.Working
            try {
                val token = if (isAutomatic) {
                    credentialClient.requestAuthorizedAccount(activity, webClientId)
                } else {
                    credentialClient.requestWithGoogleButton(activity, webClientId)
                }
                authRepository.exchangeGoogleIdToken(token)
                    .onSuccess(onSignedIn)
                    .onFailure {
                        state = SignInState.Error(
                            it.message ?: "WishTrace could not verify this account.",
                        )
                    }
            } catch (_: Exception) {
                state = if (isAutomatic) {
                    SignInState.Idle
                } else {
                    SignInState.Error(
                        "Google sign-in was cancelled or unavailable. Try again or continue for now.",
                    )
                }
            }
        }
    }

    LaunchedEffect(webClientId) {
        if (webClientId.isNotBlank() && !automaticAttempted) {
            automaticAttempted = true
            requestGoogle(isAutomatic = true)
        }
    }

    SignInScreen(
        state = state,
        onGoogle = { requestGoogle(isAutomatic = false) },
        onContinueLocal = onContinueLocal,
        onBack = onBack,
    )
}

@Composable
private fun SignInScreen(
    state: SignInState,
    onGoogle: () -> Unit,
    onContinueLocal: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                title = "",
                onBack = onBack,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GoogleButton(
                        loading = state == SignInState.Working,
                        onClick = onGoogle,
                    )
                    SecondaryAction(
                        text = "Not now",
                        onClick = onContinueLocal,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state != SignInState.Working,
                    )
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BlueSurface,
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        DimensionalAsset(
                            drawableRes = R.drawable.wishtrace_message_3d,
                            description = "A heart inside a message",
                            modifier = Modifier.size(168.dp),
                        )
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Save the people you care about.",
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text = "Sign in to keep dates and clues in sync.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            if (state is SignInState.Error) {
                item { ErrorBanner(text = state.message) }
            }
        }
    }
}

@Composable
private fun GoogleButton(
    loading: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .size(height = 56.dp, width = 1.dp),
        enabled = !loading,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Ink,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = BorderStroke(1.dp, OutlineCool),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GoogleGlyph(modifier = Modifier.size(20.dp))
                Text(
                    text = "Sign in with Google",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun GoogleGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension * 0.18f, cap = StrokeCap.Butt)
        val inset = size.minDimension * 0.08f
        val arcSize = androidx.compose.ui.geometry.Size(
            size.width - inset * 2,
            size.height - inset * 2,
        )
        drawArc(
            color = androidx.compose.ui.graphics.Color(0xFF4285F4),
            startAngle = -45f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = stroke,
        )
        drawArc(
            color = androidx.compose.ui.graphics.Color(0xFF34A853),
            startAngle = 55f,
            sweepAngle = 85f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = stroke,
        )
        drawArc(
            color = androidx.compose.ui.graphics.Color(0xFFFBBC05),
            startAngle = 140f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = stroke,
        )
        drawArc(
            color = androidx.compose.ui.graphics.Color(0xFFEA4335),
            startAngle = 220f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = stroke,
        )
        drawLine(
            color = androidx.compose.ui.graphics.Color(0xFF4285F4),
            start = Offset(size.width * 0.52f, size.height * 0.50f),
            end = Offset(size.width * 0.94f, size.height * 0.50f),
            strokeWidth = size.minDimension * 0.18f,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
