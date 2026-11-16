package com.nijatva.photos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** One action row in the sheet. */
class SheetAction(
    val label: String,
    val iconRes: Int,
    val action: () -> Unit
)

/** A titled group of actions. */
class SheetSection(val title: String, val actions: List<SheetAction>)

/**
 * Material 3 modal bottom sheet with categorized action groups — the
 * modern replacement for the old dropdown overflow menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSheet(
    sections: List<SheetSection>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            sections.forEach { section ->
                Text(
                    section.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 28.dp, top = 14.dp, bottom = 4.dp)
                )
                section.actions.forEach { act ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                act.action()
                            }
                            .padding(horizontal = 16.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(act.iconRes),
                                contentDescription = act.label,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(act.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
