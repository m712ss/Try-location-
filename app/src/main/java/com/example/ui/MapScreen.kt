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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
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
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
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
        "عمل" -> Color(0xFF6366F1) // Indigo/Work
        "انطلاق" -> Color(0xFF8B5CF6) // Violet
        "نهاية" -> Color(0xFF06B6D4) // Cyan
        else -> Color(0xFF94A3B8) // Slate Gray
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "طعام وشراب" -> Icons.Default.Restaurant
        "تسوق" -> Icons.Default.ShoppingBag
        "ترفيه" -> Icons.Default.Celebration
        "سياحة" -> Icons.Default.CameraAlt
        "إقامة" -> Icons.Default.Hotel
        "عمل" -> Icons.Default.Work
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
    val categoryFilter by viewModel.selectedCategoryFilter.collectAsState()

    val filteredPins = remember(pins, categoryFilter) {
        if (categoryFilter == "الكل") pins else pins.filter { it.category == categoryFilter }
    }

    var showAddPinDialog by remember { mutableStateOf(false) }
    var mapStyle by remember { mutableStateOf("VECTOR") } // "VECTOR" or "SATELLITE" or "TERRAIN"
    var showMapIntegrationsMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Map Canvas Renderer
        InteractiveMapCanvas(
            pins = filteredPins,
            activeRoute = activeRoute,
            center = mapCenter,
            zoom = mapZoom,
            selectedPin = selectedPin,
            onPinSelected = { viewModel.selectPin(it) },
            onMapDrag = { dx, dy -> viewModel.panMap(dx, dy) },
            mapStyle = mapStyle,
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

            FloatingActionButton(
                onClick = { showMapIntegrationsMenu = true },
                containerColor = Color(0xFF4285F4), 
                contentColor = Color.White,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = "طبقات وربط خرائط Google")
            }
        }

        // Interactive Category Filter & Legend panel
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .width(175.dp)
                .shadow(6.dp, shape = RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "تصفية",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "فلترة وتصنيف المواقع",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 2.dp))

                val filterCategories = listOf("الكل") + listOf("عمل", "تسوق", "ترفيه", "طعام وشراب", "سياحة", "إقامة", "أخرى")
                filterCategories.forEach { cat ->
                    val isSelectedFilter = categoryFilter == cat
                    val count = if (cat == "الكل") pins.size else pins.count { it.category == cat }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelectedFilter) MaterialTheme.colorScheme.primaryContainer 
                                else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setCategoryFilter(cat) }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (cat == "الكل") {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(getCategoryColor(cat), CircleShape)
                                )
                            }
                            Text(
                                text = cat, 
                                style = TextStyle(
                                    fontSize = 11.sp, 
                                    fontWeight = if (isSelectedFilter) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelectedFilter) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        
                        Text(
                            text = "$count",
                            style = TextStyle(
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.Bold,
                                color = if (isSelectedFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .background(
                                    if (isSelectedFilter) MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
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

    if (showMapIntegrationsMenu) {
        GoogleMapIntegrationsDialog(
            mapCenter = mapCenter,
            mapZoom = mapZoom,
            activeStyle = mapStyle,
            onStyleChanged = { mapStyle = it },
            onDismiss = { showMapIntegrationsMenu = false },
            viewModel = viewModel
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
    mapStyle: String,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current.density

    val categoriesList = listOf("طعام وشراب", "تسوق", "ترفيه", "سياحة", "إقامة", "عمل", "انطلاق", "نهاية", "أخرى")
    val categoryPainters = categoriesList.associateWith { cat ->
        androidx.compose.ui.graphics.vector.rememberVectorPainter(image = getCategoryIcon(cat))
    }
    val defaultPainter = androidx.compose.ui.graphics.vector.rememberVectorPainter(image = Icons.Default.Place)

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

    val mapBgColor = when (mapStyle) {
        "SATELLITE" -> Color(0xFF030E14) // Digital Deep Space Space black-blue
        "TERRAIN" -> Color(0xFF1C1917) // Earthy Stone Gray-Brown
        else -> Color(0xFF0F172A) // Modern Cartographic Slate
    }

    Canvas(
        modifier = modifier
            .background(mapBgColor)
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

        // 1. Draw grid / topography styles
        if (mapStyle == "SATELLITE") {
            // Draw glowing concentric telemetry radar rings to simulate orbital sensor scans
            drawCircle(
                color = Color(0xFF0EA5E9).copy(alpha = 0.15f),
                radius = 350f,
                center = Offset(w / 2f, h / 2f),
                style = Stroke(width = 1f * density)
            )
            drawCircle(
                color = Color(0xFF0EA5E9).copy(alpha = 0.08f),
                radius = 600f,
                center = Offset(w / 2f, h / 2f),
                style = Stroke(width = 1f * density)
            )
            // Cybernetic grid intersections
            val gridDist = 80f
            val startX = (w / 2f) % gridDist
            val startY = (h / 2) % gridDist
            var xLoc = startX
            while (xLoc < w) {
                var yLoc = startY
                while (yLoc < h) {
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                        radius = 2f * density,
                        center = Offset(xLoc, yLoc)
                    )
                    yLoc += gridDist
                }
                xLoc += gridDist
            }
        } else if (mapStyle == "TERRAIN") {
            // Draw altitude contour waves matching standard terrain designs
            drawCircle(
                color = Color(0xFFF59E0B).copy(alpha = 0.06f),
                radius = 180f,
                center = Offset(w / 3f, h / 2.5f),
                style = Stroke(width = 2f * density)
            )
            drawCircle(
                color = Color(0xFFF59E0B).copy(alpha = 0.04f),
                radius = 320f,
                center = Offset(w / 3f, h / 2.5f),
                style = Stroke(width = 1.5f * density)
            )
            drawCircle(
                color = Color(0xFF10B981).copy(alpha = 0.05f),
                radius = 250f,
                center = Offset(w * 0.7f, h * 0.6f),
                style = Stroke(width = 2f * density)
            )
            // Draw subtle topographic lines
            var gridX = (w / 2f) % 120f
            while (gridX < w) {
                drawLine(
                    color = Color(0xFF78350F).copy(alpha = 0.12f),
                    start = Offset(gridX, 0f),
                    end = Offset(gridX, h),
                    strokeWidth = 1f
                )
                gridX += 120f
            }
        } else {
            // Standard Vector dark gridlines
            val gridDist = 100f
            val startX = (w / 2f) % gridDist
            val startY = (h / 2) % gridDist
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
            val badgeRadius = if (isSelected) 14f * density else 11f * density

            // Outer pulse circle if selected to create locator dynamic alert effect
            if (isSelected) {
                drawCircle(
                    color = pinColor.copy(alpha = 0.25f),
                    radius = 28f * density,
                    center = pos
                )
                drawCircle(
                    color = pinColor.copy(alpha = 0.5f),
                    radius = 19f * density,
                    center = pos,
                    style = Stroke(width = 2f * density)
                )
            }

            // Draw shadow/glow effect of the pin
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = badgeRadius + 2f * density,
                center = pos + Offset(0f, 1f * density)
            )

            // Pin background circle matching category color
            drawCircle(
                color = pinColor,
                radius = badgeRadius,
                center = pos
            )

            // Dynamic white border for high readability/contrast
            drawCircle(
                color = Color.White,
                radius = badgeRadius,
                center = pos,
                style = Stroke(width = 1.5f * density)
            )

            // Draw the actual category icon vector inside the pin center!
            val iconSize = if (isSelected) 15f * density else 12f * density
            translate(left = pos.x - iconSize / 2f, top = pos.y - iconSize / 2f) {
                val painter = categoryPainters[pin.category] ?: defaultPainter
                with(painter) {
                    draw(
                        size = androidx.compose.ui.geometry.Size(iconSize, iconSize),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                    )
                }
            }

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
                topLeft = Offset(pos.x - textLayoutResult.size.width / 2f, pos.y - 28f * density)
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
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { onDelete(selectedPin.id) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف")
                }

                val context = LocalContext.current
                Button(
                    onClick = {
                        val uri = "geo:${selectedPin.latitude},${selectedPin.longitude}?q=${selectedPin.latitude},${selectedPin.longitude}(${Uri.encode(selectedPin.name)})"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val webUri = "https://www.google.com/maps/search/?api=1&query=${selectedPin.latitude},${selectedPin.longitude}"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUri)))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981), 
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فتح في Google Maps", fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
    var category by remember { mutableStateOf("عمل") }
    var desc by remember { mutableStateOf("") }
    var latStr by remember { mutableStateOf<String>(String.format(Locale.US, "%.5f", mapCenter.first)) }
    var lngStr by remember { mutableStateOf<String>(String.format(Locale.US, "%.5f", mapCenter.second)) }

    val categories = listOf("عمل", "تسوق", "ترفيه", "طعام وشراب", "سياحة", "إقامة", "أخرى")
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

@Composable
fun GoogleMapIntegrationsDialog(
    mapCenter: Pair<Double, Double>,
    mapZoom: Float,
    activeStyle: String,
    onStyleChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: MapRouteViewModel
) {
    val context = LocalContext.current
    val pins by viewModel.allPins.collectAsState()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(12.dp, shape = RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with colorful indicator strips representing Google Colors
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "خرائط Google والطبقات التفاعلية",
                            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }
                    
                    // Cute customized Google color strip representing official Google integration
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFEA4335)))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF4285F4)))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFFBBC05)))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF34A853)))
                    }
                }

                // 1. Layer styling selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "تصميم وخلفية المظهر الجغرافي:",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("VECTOR", "مخطط حديث 🗺️", MaterialTheme.colorScheme.primaryContainer),
                            Triple("SATELLITE", "قمر صناعي 🛰️", Color(0xFF0EA5E9).copy(alpha = 0.15f)),
                            Triple("TERRAIN", "تضاريس 🏔️", Color(0xFFF59E0B).copy(alpha = 0.15f))
                        ).forEach { (styleKey, styleName, colorToken) ->
                            val isSelected = activeStyle == styleKey
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { onStyleChanged(styleKey) },
                                label = { Text(styleName, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.elevatedFilterChipColors(
                                    selectedContainerColor = if (styleKey == "VECTOR") MaterialTheme.colorScheme.primary 
                                                             else if (styleKey == "SATELLITE") Color(0xFF0284C7) 
                                                             else Color(0xFFD97706),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 2. Google Maps Application Integrations
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "خيارات الربط والتوجيه النفاث:",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    )

                    // Button 1: Open center coordinate in Google Maps app
                    OutlinedButton(
                        onClick = {
                            val webUri = "https://www.google.com/maps/@${mapCenter.first},${mapCenter.second},${mapZoom}z"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUri)).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUri)))
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF4285F4))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "فتح موقع الكاميرا الحالي في خرائط Google", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    // Button 2: Export active route or pins
                    Button(
                        onClick = {
                            val activeSteps = viewModel.generateGoogleMapsDirectionUrl()
                            val urlMsg = if (activeSteps.isNotEmpty()) {
                                activeSteps
                            } else {
                                if (pins.isNotEmpty()) {
                                    val origin = pins.first()
                                    val dest = pins.last()
                                    "https://www.google.com/maps/dir/?api=1&origin=${origin.latitude},${origin.longitude}&destination=${dest.latitude},${dest.longitude}"
                                } else {
                                    "https://www.google.com/maps/dir/?api=1&origin=24.7136,46.6753&destination=24.7180,46.6850"
                                }
                            }
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlMsg))
                            context.startActivity(intent)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "تشغيل ملاحة مسار رحلتك في Google Maps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Copy center coordinates button
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Coordinates", "${mapCenter.first}, ${mapCenter.second}")
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "تم نسخ إحداثيات مركز الخريطة 📋", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "نسخ إحداثيات المركز الحالي: ${String.format(Locale.US, "%.4f, %.4f", mapCenter.first, mapCenter.second)}",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
