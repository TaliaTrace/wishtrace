package com.wishtrace.app.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.wishtrace.app.ui.theme.BrandIndigo
import com.wishtrace.app.ui.theme.LavenderSurface
import com.wishtrace.app.ui.theme.SurfaceWhite

enum class ShellDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home("home", "Home", Icons.Rounded.Home),
    People("people", "People", Icons.Rounded.People),
    Occasions("occasions", "Occasions", Icons.Rounded.CalendarMonth),
    Profile("profile", "Profile", Icons.Rounded.Person),
}

@Composable
fun WishTraceBottomBar(
    currentRoute: String?,
    onNavigate: (ShellDestination) -> Unit,
) {
    BottomAppBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = SurfaceWhite,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp),
    ) {
        ShellDestination.entries.forEach { destination ->
            DestinationItem(
                destination = destination,
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination) },
            )
        }
    }
}

@Composable
private fun RowScope.DestinationItem(
    destination: ShellDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.weight(1f),
        icon = {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                modifier = Modifier.size(23.dp),
            )
        },
        label = {
            Text(
                text = destination.label,
                style = MaterialTheme.typography.labelSmall,
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = BrandIndigo,
            selectedTextColor = BrandIndigo,
            indicatorColor = LavenderSurface,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
