package com.example.ui

import java.util.Locale

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.SavedPin
import kotlin.math.sqrt

// Color configurations based on standard map categories
fun getCategoryColor(category: String): Color {
    return when (category) {
        "طعام وشراب" -> Color(0xFFEF4444) // Red
        "تسوق" -> Color(0xFFF59E0B) // Amber
        "ترفيه" -> Color(0xFFEC4899) // Pink
        "سياحة" -> Color(0xFF10B981) // Green-Emerald
        "إقامة" -> Color(0xFF3B82F6) // Blue
        "انطلاق" -> Color(0xFF8B5CF6) // Violet
        "نهاية" -> Color(0xFF06B6D4) // Cyan
        else -> Color(0xFF6B7280) // Gray
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "طعام وشراب" -> Icons.Default.Restaurant
        "تسوق" -> Icons.Default.ShoppingBag
        "ترفيه" -> Icons.Default.Celebration
        "سياحة" -> Icons.Default.CameraAlt
        "إقامة" -> Icons.Default.Hotel
        "انطلاق" -> Icons.Default.PlayArrow
        "نهاية" -> Icons.Default.Flag
        else -> Icons.Default.Place
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapRouteViewModel,
    modifier: Modifier = Modifier
) {
    val pins by viewModel.allPins.collectAsState()
    val activeRoute by viewModel.optimizedRoute.collectAsState()
    val selectedPin by viewModel.selectedPin.collectAsState()
    val mapCenter by viewModel.mapCenter.collectAsState()
    val mapZoom by viewModel.mapZoom.collectAsState()

    var showAddPinDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Map Canvas Renderer
        InteractiveMapCanvas(
            pins = pins,
            activeRoute = activeRoute,
            center = mapCenter,
            zoom = mapZoom,
            selectedPin = selectedPin,
            onPinSelected = { viewModel.selectPin(it) },
            onMapDrag = { dx, dy -> viewModel.panMap(dx, dy) },
            modifier = Modifier.fillMaxSize()
        )

        // 2. HUD Buttons overlays (Zoom In, Zoom Out, Add Pin, Clear)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FloatingActionButton(
                onClick = { viewModel.setZoom(mapZoom + 1f) },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "تكبير")
            }

            FloatingActionButton(
                onClick = { viewModel.setZoom(mapZoom - 1f) },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "تصغير")
            }

            FloatingActionButton(
                onClick = { showAddPinDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.AddLocation, contentDescription = "إضافة دبوس")
            }
        }

        // Quick Category Legend indicator overlay
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .shadow(4.dp, shape = RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "دليل التصنيفات",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                listOf("طعام وشراب", "تسوق", "ترفيه", "سياحة", "إقامة", "أخرى").forEach { cat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(getCategoryColor(cat), CircleShape)
                        )
                        Text(text = cat, style = TextStyle(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // 3. Selection Detail Card (bottom overlay)
        AnimatedSelectedPinDetail(
            selectedPin = selectedPin,
            onClose = { viewModel.selectPin(null) },
            onDelete = {
                viewModel.deletePin(it)
                viewModel.selectPin(null)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp) // Leave room for bottom navigation app bar
        )
    }

    if (showAddPinDialog) {
        AddManualPinDialog(
            mapCenter = mapCenter,
            onDismiss = { showAddPinDialog = false },
            onAdd = { name, category, desc, lat, lng ->
                viewModel.addNewManualPin(name, category, desc, lat, lng, null)
                showAddPinDialog = false
            }
        )
    }
}

@Composable
fun InteractiveMapCanvas(
    pins: List<SavedPin>,
    activeRoute: RoutePlanResult?,
    center: Pair<Double, Double>,
    zoom: Float,
    selectedPin: SavedPin?,
    onPinSelected: (SavedPin?) -> Unit,
    onMapDrag: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current.density

    // Map background coordinates mapping helpers
    fun getScreenPos(lat: Double, lng: Double, width: Float, height: Float): Offset {
        val centerX = width / 2f
        val centerY = height / 2f
        val lngDelta = lng - center.second
        val latDelta = lat - center.first
        
        val scale = 256.0 * Math.pow(2.0, zoom.toDouble() - 10.0)
        
        val x = centerX + (lngDelta * scale).toFloat()
        val y = centerY - (latDelta * scale).toFloat() // Latitude goes UP, Y screen coordinate goes DOWN
        return Offset(x, y)
    }

    Canvas(
        modifier = modifier
            .background(Color(0xFF0F172A)) // Cartographic night slate
            .pointerInput(pins, center, zoom) {
                detectTapGestures { offset ->
                    var found: SavedPin? = null
                    var minDistance = 24.dp.toPx() // 24dp selection radius

                    pins.forEach { p ->
                        val pos = getScreenPos(p.latitude, p.longitude, size.width.toFloat(), size.height.toFloat())
                        val dx = offset.x - pos.x
                        val dy = offset.y - pos.y
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist < minDistance) {
                            minDistance = dist
                            found = p
                        }
                    }
                    onPinSelected(found)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Reversing drag logic to pan map naturally
                    onMapDrag(-dragAmount.x.toDouble(), dragAmount.y.toDouble())
                }
            }
    ) {
        val w = size.width
        val h = size.height

        // 1. Draw grid slate lines
        val gridDist = 100f
        val startX = (w / 2f) % gridDist
        val startY = (h / 2) % gridDist

        // Draw longitudinal and latitudinal lines
        var gridX = startX
        while (gridX < w) {
            drawLine(
                color = Color(0xFF1E293B).copy(alpha = 0.5f),
                start = Offset(gridX, 0f),
                end = Offset(gridX, h),
                strokeWidth = 1f
            )
            gridX += gridDist
        }

        var gridY = startY
        while (gridY < h) {
            drawLine(
                color = Color(0xFF1E293B).copy(alpha = 0.5f),
                start = Offset(0f, gridY),
                end = Offset(w, gridY),
                strokeWidth = 1f
            )
            gridY += gridDist
        }

        // Draw beautiful coordinate origin concentric rings in center
        drawCircle(
            color = Color(0xFF1E293B),
            radius = 120f,
            center = Offset(w/2f, h/2f),
            style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
        )

        // 2. Draw Active Route lines connecting pins
        if (activeRoute != null && activeRoute.steps.isNotEmpty()) {
            val steps = activeRoute.steps
            for (i in 0 until steps.size - 1) {
                val p1 = getScreenPos(steps[i].pin.latitude, steps[i].pin.longitude, w, h)
                val p2 = getScreenPos(steps[i+1].pin.latitude, steps[i+1].pin.longitude, w, h)

                // Draw path line with glowing gradient
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(getCategoryColor(steps[i].pin.category), getCategoryColor(steps[i+1].pin.category))
                    ),
                    start = p1,
                    end = p2,
                    strokeWidth = 6f * density,
                )

                // Draw path border outline for contrast
                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = p1,
                    end = p2,
                    strokeWidth = 1.5f * density,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                )

                // Draw sequence sequence indicators between steps
                val midX = (p1.x + p2.x) / 2f
                val midY = (p1.y + p2.y) / 2f
                drawCircle(
                    color = Color(0xFF1E293B),
                    radius = 11f * density,
                    center = Offset(midX, midY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 9f * density,
                    center = Offset(midX, midY),
                    style = Stroke(width = 1f * density)
                )
                
                // Draw estimated travel time on route line segment
                val travelDurationStr = "${steps[i+1].durationFromPreviousMinutes} د"
                drawText(
                    textMeasurer = textMeasurer,
                    text = travelDurationStr,
                    topLeft = Offset(midX - 12f * density, midY - 6f * density),
                    style = TextStyle(color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.SemiBold)
                )
            }
        }

        // 3. Draw All Pins
        pins.forEach { pin ->
            val pos = getScreenPos(pin.latitude, pin.longitude, w, h)
            val isSelected = pin.id == selectedPin?.id
            val pinColor = getCategoryColor(pin.category)

            // Outer pulse circle if selected to create locator dynamic alert effect
            if (isSelected) {
                drawCircle(
                    color = pinColor.copy(alpha = 0.3f),
                    radius = 26f * density,
                    center = pos
                )
                drawCircle(
                    color = pinColor.copy(alpha = 0.6f),
                    radius = 18f * density,
                    center = pos,
                    style = Stroke(width = 2f * density)
                )
            }

            // Normal pin concentric circle outline
            drawCircle(
                color = Color.White,
                radius = 10f * density,
                center = pos
            )

            drawCircle(
                color = pinColor,
                radius = 8f * density,
                center = pos
            )

            // Inner center dot
            drawCircle(
                color = Color.White,
                radius = 3f * density,
                center = pos
            )

            // Draw pin names with text measurer above the pin
            val textLayoutResult = textMeasurer.measure(
                text = pin.name,
                style = TextStyle(
                    color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                    fontSize = if (isSelected) 11.sp else 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    background = Color(0xFF1E293B).copy(alpha = 0.85f)
                )
            )

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(pos.x - textLayoutResult.size.width / 2f, pos.y - 25f * density)
            )
        }
    }
}

@Composable
fun AnimatedSelectedPinDetail(
    selectedPin: SavedPin?,
    onClose: () -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedPin == null) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(6.dp, shape = RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(getCategoryColor(selectedPin.category), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(selectedPin.category),
                            contentDescription = selectedPin.category,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = selectedPin.name,
                            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = selectedPin.category,
                            style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            if (!selectedPin.description.isNullOrBlank()) {
                Text(
                    text = selectedPin.description,
                    style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text("خط عرض: ${String.format(Locale.getDefault(), "%.4f", selectedPin.latitude)}") },
                    leadingIcon = { Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = {},
                    label = { Text("خط طول: ${String.format(Locale.getDefault(), "%.4f", selectedPin.longitude)}") },
                    leadingIcon = { Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { onDelete(selectedPin.id) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف الدبوس")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualPinDialog(
    mapCenter: Pair<Double, Double>,
    onDismiss: () -> Unit,
    onAdd: (String, String, String?, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("طعام وشراب") }
    var desc by remember { mutableStateOf("") }
    var latStr by remember { mutableStateOf<String>(String.format(Locale.US, "%.5f", mapCenter.first)) }
    var lngStr by remember { mutableStateOf<String>(String.format(Locale.US, "%.5f", mapCenter.second)) }

    val categories = listOf("طعام وشراب", "تسوق", "ترفيه", "سياحة", "إقامة", "أخرى")
    var catExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "إضافة دبوس موقع يدوياً",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الموقع") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("التصنيف") },
                            trailingIcon = {
                                IconButton(onClick = { catExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = catExpanded,
                            onDismissRequest = { catExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        catExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("الوصف / الملاحظات") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = latStr,
                            onValueChange = { latStr = it },
                            label = { Text("خط العرض") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lngStr,
                            onValueChange = { lngStr = it },
                            label = { Text("خط الطول") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("إلغاء")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val lat = latStr.toDoubleOrNull() ?: mapCenter.first
                                val lng = lngStr.toDoubleOrNull() ?: mapCenter.second
                                if (name.isNotBlank()) {
                                    onAdd(name, category, desc.ifBlank { null }, lat, lng)
                                }
                            },
                            enabled = name.isNotBlank()
                        ) {
                            Text("إضافة الدبوس")
                        }
                    }
                }
            }
        }
    }
}
