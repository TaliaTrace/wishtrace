package com.wishtrace.app.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishtrace.app.ui.theme.BrandBlue
import com.wishtrace.app.ui.theme.BrandIndigo
import com.wishtrace.app.ui.theme.BrandIndigoPressed
import com.wishtrace.app.ui.theme.Canvas
import com.wishtrace.app.ui.theme.ErrorRed
import com.wishtrace.app.ui.theme.ErrorSoft
import com.wishtrace.app.ui.theme.Ink
import com.wishtrace.app.ui.theme.InkMuted
import com.wishtrace.app.ui.theme.LavenderSurface
import com.wishtrace.app.ui.theme.OutlineCool
import com.wishtrace.app.ui.theme.SurfaceWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WishTraceWordmark(
    modifier: Modifier = Modifier,
    markSize: Dp = 34.dp,
) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "WishTrace"
        },
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WishTraceMark(
            modifier = Modifier
                .size(markSize)
                .clearAndSetSemantics { },
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Ink)) {
                    append("Wish")
                }
                withStyle(SpanStyle(color = BrandIndigo)) {
                    append("Trace")
                }
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.45).sp,
        )
    }
}

@Composable
fun WishTraceMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.075f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val boxTop = size.height * 0.30f
        val boxLeft = size.width * 0.14f
        val boxSize = Size(size.width * 0.64f, size.height * 0.48f)

        drawRoundRect(
            color = BrandIndigo,
            topLeft = Offset(boxLeft, boxTop),
            size = boxSize,
            cornerRadius = CornerRadius(size.width * 0.09f),
            style = stroke,
        )
        drawLine(
            color = BrandIndigo,
            start = Offset(size.width * 0.46f, boxTop),
            end = Offset(size.width * 0.46f, boxTop + boxSize.height),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = BrandIndigo,
            start = Offset(boxLeft, size.height * 0.49f),
            end = Offset(boxLeft + boxSize.width, size.height * 0.49f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawArc(
            color = BrandIndigo,
            startAngle = 186f,
            sweepAngle = 218f,
            useCenter = false,
            topLeft = Offset(size.width * 0.18f, size.height * 0.04f),
            size = Size(size.width * 0.29f, size.height * 0.32f),
            style = stroke,
        )
        drawArc(
            color = BrandIndigo,
            startAngle = 132f,
            sweepAngle = 218f,
            useCenter = false,
            topLeft = Offset(size.width * 0.44f, size.height * 0.04f),
            size = Size(size.width * 0.29f, size.height * 0.32f),
            style = stroke,
        )
        drawLine(
            color = BrandBlue,
            start = Offset(size.width * 0.77f, size.height * 0.78f),
            end = Offset(size.width * 0.92f, size.height * 0.90f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = BrandBlue,
            radius = strokeWidth * 0.72f,
            center = Offset(size.width * 0.93f, size.height * 0.91f),
        )
    }
}

@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandIndigo,
            contentColor = SurfaceWhite,
            disabledContainerColor = LavenderSurface,
            disabledContentColor = InkMuted,
        ),
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
        if (trailingContent != null) {
            Spacer(modifier = Modifier.size(10.dp))
            trailingContent()
        }
    }
}

@Composable
fun SecondaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = BrandIndigoPressed,
            disabledContentColor = InkMuted,
        ),
        border = BorderStroke(1.dp, OutlineCool),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ScreenTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = "Back" },
        ) {
            Text(
                text = "‹",
                modifier = Modifier.clearAndSetSemantics { },
                color = Ink,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        action?.invoke()
    }
}

@Composable
fun EditorialCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, OutlineCool.copy(alpha = 0.9f)),
        content = content,
    )
}

@Composable
fun InterestChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = LavenderSurface,
        contentColor = BrandIndigoPressed,
        shape = CircleShape,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun ErrorBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ErrorSoft,
        contentColor = ErrorRed,
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun RecipientAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    photoUri: String? = null,
) {
    val context = LocalContext.current
    val photo by produceState<ImageBitmap?>(initialValue = null, photoUri) {
        value = photoUri?.let { loadLocalPhoto(context, it) }
    }
    Surface(
        modifier = modifier.size(size),
        color = LavenderSurface,
        contentColor = BrandIndigoPressed,
        shape = CircleShape,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (photo != null) {
                Image(
                    bitmap = requireNotNull(photo),
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private suspend fun loadLocalPhoto(
    context: android.content.Context,
    rawUri: String,
): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val uri = Uri.parse(rawUri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > 512 ||
            bounds.outHeight / sampleSize > 512
        ) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)?.asImageBitmap()
        }
    }.getOrNull()
}

@Composable
fun DimensionalAsset(
    @DrawableRes drawableRes: Int,
    description: String?,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(drawableRes),
        contentDescription = description,
        modifier = modifier,
    )
}

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = InkMuted,
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 0.8.sp,
    )
}

@Composable
fun PageContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = Canvas,
        content = content,
    )
}
