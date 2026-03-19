package com.dotmatrix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmptyStateCard(icon: ImageVector, title: String, subtitle: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(title, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        }
    }
}

@Composable
fun TabPill(title: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(6.dp))
            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable { onClick() }.padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 2.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(title, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
