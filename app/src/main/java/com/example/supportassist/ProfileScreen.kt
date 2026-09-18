package com.example.supportassist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen(
    user: User?,
    totalTickets: Int,
    resolvedTickets: Int,
    currentThemeMode: String,
    onThemeChange: (String) -> Unit,
    onLogout: () -> Unit
) {
    val scrollState = rememberScrollState()
    val bgColor = colorResource(id = R.color.bg_color)
    val cardBg = colorResource(id = R.color.profile_card)
    val primaryBlue = colorResource(id = R.color.primary_blue)
    val textMain = colorResource(id = R.color.text_main)
    val textGrey = colorResource(id = R.color.text_grey)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Profile",
            color = textMain,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Profile Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(primaryBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = primaryBlue
                    )
                }

                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = user?.name ?: "...",
                        color = textMain,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user?.email ?: "...",
                        color = textGrey,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = colorResource(id = R.color.status_open_bg),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Member",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = colorResource(id = R.color.status_open_text),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats Row
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCard(
                label = "Total Tickets",
                value = totalTickets.toString(),
                modifier = Modifier.weight(1f),
                cardBg = cardBg,
                textMain = textMain,
                textGrey = textGrey
            )
            Spacer(modifier = Modifier.width(16.dp))
            StatCard(
                label = "Resolved",
                value = resolvedTickets.toString(),
                modifier = Modifier.weight(1f),
                cardBg = cardBg,
                textMain = textMain,
                textGrey = textGrey
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                MenuItem(icon = Icons.Default.Person, label = "Edit Profile", textColor = textMain)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = colorResource(id = R.color.divider_color))
                MenuItem(icon = Icons.Default.Info, label = "Help Support", textColor = textMain)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Theme card — an explicit in-app override on top of the OS's own
        // dark/light setting (the app already follows that automatically via
        // values-night/ resources; this just lets someone pick regardless of
        // their phone's setting). See ThemeManager.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Appearance", color = textMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                ThemeSelector(
                    currentMode = currentThemeMode,
                    onModeSelected = onThemeChange,
                    primaryBlue = primaryBlue,
                    textMain = textMain,
                    bgColor = bgColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.priority_high_bg))
        ) {
            Text(
                text = "Logout",
                color = colorResource(id = R.color.priority_high_text),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier, cardBg: Color, textMain: Color, textGrey: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, color = textMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = label, color = textGrey, fontSize = 12.sp)
        }
    }
}

@Composable
fun ThemeSelector(
    currentMode: String,
    onModeSelected: (String) -> Unit,
    primaryBlue: Color,
    textMain: Color,
    bgColor: Color
) {
    val options = listOf(
        Triple(ThemeManagerModes.LIGHT, "Light", null),
        Triple(ThemeManagerModes.DARK, "Dark", null),
        Triple(ThemeManagerModes.SYSTEM, "System", null)
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, label, _) ->
            val selected = mode == currentMode
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .then(if (index > 0) Modifier.padding(start = 8.dp) else Modifier)
                    .clickable { onModeSelected(mode) },
                shape = RoundedCornerShape(12.dp),
                color = if (selected) primaryBlue else bgColor
            ) {
                Text(
                    text = label,
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .fillMaxWidth(),
                    color = if (selected) Color.White else textMain,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/** Mirrors ThemeManager's string constants — kept as plain strings there since that class is Java, read by Kotlin here. */
object ThemeManagerModes {
    const val LIGHT = "light"
    const val DARK = "dark"
    const val SYSTEM = "system"
}

@Composable
fun MenuItem(icon: ImageVector, label: String, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(24.dp))
        Text(
            text = label,
            modifier = Modifier.padding(start = 16.dp),
            color = textColor,
            fontSize = 16.sp
        )
    }
}
