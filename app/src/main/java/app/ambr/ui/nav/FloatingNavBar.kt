package app.ambr.ui.nav

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.ambr.R
import app.ambr.ui.theme.AmbrColors

enum class Tab(val route: String, val label: String, @DrawableRes val icon: Int, @DrawableRes val iconActive: Int) {
    Home("home", "Home", R.drawable.ic_home, R.drawable.ic_home_solid),
    Vault("vault", "Vault", R.drawable.ic_apps, R.drawable.ic_apps_solid),
    You("you", "You", R.drawable.ic_user, R.drawable.ic_user_solid),
}

// Glass pill with the three tabs, and the Add button floating beside it.
@Composable
fun FloatingNavBar(
    current: Tab,
    onSelect: (Tab) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(68.dp)
                .shadow(elevation = 14.dp, shape = RoundedCornerShape(34.dp), ambientColor = AmbrColors.Navy, spotColor = AmbrColors.Navy)
                .clip(RoundedCornerShape(34.dp))
                .background(AmbrColors.Card.copy(alpha = 0.94f))
                .border(1.dp, AmbrColors.Navy.copy(alpha = 0.06f), RoundedCornerShape(34.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Tab.entries.forEach { tab ->
                val active = tab == current
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(28.dp))
                        .background(if (active) AmbrColors.Navy else AmbrColors.Card.copy(alpha = 0f))
                        .selectable(selected = active, role = Role.Tab, onClick = { onSelect(tab) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val tint = if (active) AmbrColors.Card else AmbrColors.TextMuted
                    Icon(
                        painter = painterResource(if (active) tab.iconActive else tab.icon),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(19.dp),
                    )
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                        color = tint,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .size(68.dp)
                .shadow(elevation = 14.dp, shape = CircleShape, ambientColor = AmbrColors.Honey, spotColor = AmbrColors.Honey)
                .clip(CircleShape)
                .background(AmbrColors.Honey)
                .selectable(selected = false, role = Role.Button, onClick = onAdd)
                .semantics { contentDescription = "Add item" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = null,
                tint = AmbrColors.TextPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
