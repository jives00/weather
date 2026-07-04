package com.weather.app.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.ui.main.UpdateState

private val BannerBackground = Color(0xE6192028)
private val AccentBlue = Color(0xFF81D4FA)
private val ErrorRed = Color(0xFFEF9A9A)

@Composable
fun UpdateBanner(
    state: UpdateState,
    context: Context,
    onUpdate: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = state is UpdateState.Available || state is UpdateState.Downloading ||
        state is UpdateState.ReadyToInstall || state is UpdateState.Error

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BannerBackground)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            when (state) {
                is UpdateState.Available -> AvailableContent(state.tag, onUpdate, onDismiss)
                is UpdateState.Downloading -> DownloadingContent(state.progress)
                is UpdateState.ReadyToInstall -> ReadyContent(onInstall, onDismiss)
                is UpdateState.Error -> ErrorContent(state.message, onRetry, onDismiss)
                else -> {}
            }
        }
    }
}

@Composable
private fun AvailableContent(tag: String, onUpdate: () -> Unit, onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Update available",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = tag,
                color = AccentBlue,
                fontSize = 12.sp
            )
        }
        TextButton(
            onClick = onUpdate,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text("Download", color = AccentBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun DownloadingContent(progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Downloading update…", color = Color.White, fontSize = 13.sp)
            Text("${(progress * 100).toInt()}%", color = AccentBlue, fontSize = 13.sp)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = AccentBlue,
            trackColor = Color.White.copy(alpha = 0.15f)
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Update check failed",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                color = ErrorRed,
                fontSize = 12.sp
            )
        }
        TextButton(
            onClick = onRetry,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text("Retry", color = AccentBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ReadyContent(onInstall: () -> Unit, onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Ready to install",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = onInstall,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text("Install", color = AccentBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        }
    }
}
