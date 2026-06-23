package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SavedRoute
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedRoutesScreen(
    viewModel: MapRouteViewModel,
    modifier: Modifier = Modifier
) {
    val routes by viewModel.allRoutes.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(Icons.Default.Bookmarks, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column {
                Text(
                    text = "المسارات المفضلة والمحفوظة",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "مخزنة محلياً على جهازك ومتاحة بنسبة 100% دون اتصال بالإنترنت 💾",
                    style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        if (routes.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "لا توجد مسارات محفوظة بعد.",
                        style = TextStyle(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "قم بإضافة مواقع، رتب خطوات المسار المثالية، ثم اضغط على 'حفظ المسار المفضل' لتجده هنا في أي وقت.",
                        style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 60.dp), // Leave space for bottom navigation bar
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(routes) { route ->
                    SavedRouteItem(
                        route = route,
                        onLoad = { viewModel.loadSavedRouteToMap(it) },
                        onExport = {
                             val activeSteps = viewModel.generateGoogleMapsDirectionUrl()
                             val urlMsg = if (activeSteps.isNotEmpty()) {
                                 activeSteps
                             } else {
                                 // Draw simple direct link if activeSteps fails
                                 "https://www.google.com/maps/dir/?api=1&origin=${route.originLat},${route.originLng}&destination=${route.destLat},${route.destLng}"
                             }
                             val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlMsg))
                             context.startActivity(intent)
                        },
                        onDelete = { viewModel.deleteSavedRoute(route.id) },
                        onFavoriteToggle = { viewModel.toggleRouteFavorite(route.id, !route.isFavorite) }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedRouteItem(
    route: SavedRoute,
    onLoad: (SavedRoute) -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(3.dp, shape = RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Directions,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = route.name,
                            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = (-0.2).sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                            )
                            Text(
                                text = "متاح ومحفوظ بالكامل دون اتصال",
                                style = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            imageVector = if (route.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            tint = if (route.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                            contentDescription = "تفضيل"
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            tint = MaterialTheme.colorScheme.error,
                            contentDescription = "حذف"
                        )
                    }
                }
            }

            if (!route.notes.isNullOrBlank()) {
                Text(
                    text = route.notes,
                    style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.5f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp), tint = getCategoryColor("انطلاق"))
                        Text("البداية: ${route.originName}", style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant), maxLines = 1)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(12.dp), tint = getCategoryColor("نهاية"))
                        Text("النهاية: ${route.destName}", style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant), maxLines = 1)
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f كم", route.totalDistanceKm),
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                    val hours = route.totalDurationMinutes / 60
                    val mins = route.totalDurationMinutes % 60
                    val timeString = if (hours > 0) "$hours س و $mins د" else "$mins د"
                    Text(
                        text = "الزمن: $timeString",
                        style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onLoad(route) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("عرض الخريطة", fontSize = 11.sp)
                }

                Button(
                    onClick = onExport,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصدير للخرائط", fontSize = 11.sp)
                }
            }
        }
    }
}
