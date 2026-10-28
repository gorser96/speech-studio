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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
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
import androidx.compose.ui.unit.dp

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
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (logo != null) {
                Image(
                    painter = logo,
                    contentDescription = "Кабинет речи",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SS",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Кабинет речи",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Помогаем говорить красиво",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(8.dp))

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
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .semantics { if (!enabled) disabled() },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall
            )
        },
        shape = MaterialTheme.shapes.small,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
            unselectedContainerColor = MaterialTheme.colorScheme.surface,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        selected = currentScreen == screen,
        onClick = {
            if (enabled) onScreenSelected(screen)
        }
    )
}
