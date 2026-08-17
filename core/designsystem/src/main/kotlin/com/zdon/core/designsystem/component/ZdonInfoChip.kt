package com.zdon.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Compact metadata label. Defaults to a quiet outlined style so lists stay
 * uncluttered; callers pass a filled [containerColor]/[contentColor] (with a
 * neutral border suppressed) for emphasis such as error states.
 */
@Composable
fun ZdonInfoChip(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val filled = containerColor != Color.Transparent
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor,
        border = if (filled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(end = 0.dp),
                )
            }
            Text(
                text = label,
                modifier = Modifier.padding(start = if (icon != null) 4.dp else 0.dp),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}