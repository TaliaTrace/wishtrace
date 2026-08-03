package com.wishtrace.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
    onAdd: () -> Unit,
) {
    BottomAppBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = SurfaceWhite,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp),
    ) {
        ShellDestination.entries.take(2).forEach { destination ->
            DestinationItem(
                destination = destination,
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination) },
            )
        }
        AddPersonItem(onClick = onAdd)
        ShellDestination.entries.drop(2).forEach { destination ->
            DestinationItem(
                destination = destination,
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination) },
            )
        }
    }
}

@Composable
private fun RowScope.AddPersonItem(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Surface(
                onClick = onClick,
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = "Add a person"
                        role = Role.Button
                    },
                color = BrandIndigo,
                contentColor = SurfaceWhite,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(17.dp),
                shadowElevation = 5.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Text(
                text = "Add",
                color = BrandIndigo,
                style = MaterialTheme.typography.labelSmall,
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
