package com.speech_studio.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The logo is injected by the platform shell so the drawer does not reference
 * Android resources. The iOS shell supplies the same image from Compose Resources.
 */
@Composable
fun AppDrawer(
    currentScreen: AppScreen,
    enabled: Boolean = true,
    logo: Painter? = null,
    onScreenSelected: (AppScreen) -> Unit
) {
    ModalDrawerSheet {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (logo != null) {
                Image(
                    painter = logo,
                    contentDescription = "Кабинет речи",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SS",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "Кабинет речи",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Помогаем говорить увереннее",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        DrawerItem("Расписание", AppScreen.Schedule, currentScreen, enabled, onScreenSelected)
        DrawerItem("Дети", AppScreen.Children, currentScreen, enabled, onScreenSelected)
        DrawerItem("Шаблоны смен", AppScreen.ShiftTemplates, currentScreen, enabled, onScreenSelected)
        DrawerItem("Тэги", AppScreen.Tags, currentScreen, enabled, onScreenSelected)
        DrawerItem("Настройки", AppScreen.Settings, currentScreen, enabled, onScreenSelected)
    }
}

@Composable
private fun DrawerItem(
    label: String,
    screen: AppScreen,
    currentScreen: AppScreen,
    enabled: Boolean,
    onScreenSelected: (AppScreen) -> Unit
) {
    NavigationDrawerItem(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.38f)
            .semantics { if (!enabled) disabled() },
        label = {
            Text(label)
        },
        shape = RoundedCornerShape(2.dp),
        selected = currentScreen == screen,
        onClick = {
            if (enabled) onScreenSelected(screen)
        }
    )
}
