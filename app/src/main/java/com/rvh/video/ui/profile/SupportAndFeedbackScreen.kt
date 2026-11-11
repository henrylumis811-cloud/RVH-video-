package com.rvh.video.ui.profile

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rvh.video.BuildConfig
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.TextPrimary
import com.rvh.video.ui.theme.TextSecondary
import java.net.URLEncoder

private const val RVH_FEEDBACK_EMAIL = "henrylumis811@gmail.com"
private const val RVH_FEEDBACK_WHATSAPP = "256786997653"
private const val RVH_TELEGRAM_GROUP_URL = "https://t.me/+nII1BLKPzJw4MmU0"
private const val RVH_WHATSAPP_CHANNEL_URL = "https://whatsapp.com/channel/0029VbDuyWL3AzNJNWCkps3Q"

@Composable
fun SupportAndFeedbackScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var category by remember { mutableStateOf("Suggestion") }
    var message by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    val versionInfo = remember(context) {
        runCatching {
            val info = if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION") context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else {
                @Suppress("DEPRECATION") info.versionCode.toLong()
            }
            "${info.versionName ?: BuildConfig.VERSION_NAME} (build $code)"
        }.getOrDefault("${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 14.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = AccentTeal)
            }
            Text("Support & Feedback", style = RvhType.ScreenTitle, color = TextPrimary)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xE6122024)),
            shape = MaterialTheme.shapes.large
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("We're listening", style = RvhType.CardTitle, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(
                    "Share an idea, report a problem or tell us what you enjoy about RVH Video. Your message is only shared when you choose an option below and send it.",
                    style = RvhType.Body,
                    color = TextSecondary
                )
            }
        }

        SupportSectionTitle("JOIN THE RVH COMMUNITY")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xCC10191C)),
            shape = MaterialTheme.shapes.large
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Stay connected", style = RvhType.CardTitle, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    "Join the RVH Telegram group for community discussions and follow the WhatsApp channel for announcements and updates.",
                    style = RvhType.Body,
                    color = TextSecondary
                )
                Button(
                    onClick = {
                        status = if (openExternal(context, RVH_TELEGRAM_GROUP_URL))
                            "Telegram opened. Follow the link to join the RVH group."
                        else "Could not open Telegram. Please try again or open the invite link in your browser."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Groups, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Join RVH on Telegram")
                }
                OutlinedButton(
                    onClick = {
                        status = if (openExternal(context, RVH_WHATSAPP_CHANNEL_URL))
                            "WhatsApp opened. Follow the channel to receive RVH updates."
                        else "Could not open WhatsApp. Please try again or open the channel link in your browser."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Chat, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Follow RVH on WhatsApp")
                }
            }
        }

        SupportSectionTitle("SEND FEEDBACK")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Suggestion", "Problem", "Praise").forEach { option ->
                FilterChip(
                    selected = category == option,
                    onClick = { category = option },
                    label = { Text(option) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Your feedback") },
            placeholder = { Text("Describe your idea or issue…") },
            minLines = 4
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Choose where to send your feedback. RVH Video prepares the message in your chosen app; nothing is sent until you review and press Send.",
            style = RvhType.Meta,
            color = TextSecondary
        )
        Spacer(Modifier.height(12.dp))

        val feedbackBody = remember(category, message, versionInfo) {
            "RVH Video feedback\nType: $category\nApp: $versionInfo\nAndroid: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n\n${message.trim()}\n\nPlease do not include private media information or personal data."
        }

        Button(
            onClick = {
                if (message.isBlank()) {
                    status = "Please enter a message before sending feedback."
                } else {
                    status = if (sendFeedbackEmail(context, category, feedbackBody))
                        "Email opened. Review the message and press Send in your email app."
                    else "No email app could handle this request. You can send feedback using WhatsApp instead."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Email, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Send feedback by email")
        }
        OutlinedButton(
            onClick = {
                if (message.isBlank()) {
                    status = "Please enter a message before sending feedback."
                } else {
                    val url = "https://wa.me/$RVH_FEEDBACK_WHATSAPP?text=${URLEncoder.encode(feedbackBody, "UTF-8")}"
                    status = if (openExternal(context, url))
                        "WhatsApp opened. Review the message and press Send there."
                    else "Could not open WhatsApp or a browser. Please try email instead."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Chat, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Send feedback by WhatsApp")
        }
        if (status.isNotBlank()) {
            Text(status, modifier = Modifier.padding(top = 10.dp), style = RvhType.Meta, color = AccentTeal)
        }

        SupportSectionTitle("PRIVACY")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xCC10191C)),
            shape = MaterialTheme.shapes.large
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Feedback is not uploaded automatically. RVH Video does not attach your library, media filenames, storage paths or playback history. Only the app and device version plus the message you write are included in the prepared feedback.",
                    style = RvhType.Body,
                    color = TextSecondary
                )
                Text(
                    "Your email or WhatsApp app handles delivery. Review the message and press Send yourself.",
                    style = RvhType.Meta,
                    color = TextSecondary
                )
            }
        }

        Text(
            "Installed version: $versionInfo",
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp),
            style = RvhType.Meta,
            color = TextSecondary
        )
    }
}

@Composable
private fun SupportSectionTitle(title: String) {
    Text(
        title,
        modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 9.dp),
        style = RvhType.Meta,
        color = AccentTeal,
        fontWeight = FontWeight.Bold
    )
}

private fun openExternal(context: Context, url: String): Boolean = try {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
    )
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}

private fun sendFeedbackEmail(context: Context, category: String, body: String): Boolean = try {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$RVH_FEEDBACK_EMAIL")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(RVH_FEEDBACK_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "RVH Video — $category")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    try {
        val chooserIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(RVH_FEEDBACK_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, "RVH Video — $category")
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(chooserIntent, "Send RVH Video feedback by email"))
        true
    } catch (_: Exception) {
        false
    }
} catch (_: Exception) {
    false
}
