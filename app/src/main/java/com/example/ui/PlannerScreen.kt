package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.SavedPin
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: MapRouteViewModel,
    modifier: Modifier = Modifier
) {
    val pins by viewModel.allPins.collectAsState()
    val activeRoute by viewModel.optimizedRoute.collectAsState()
    val selectedWaypoints by viewModel.selectedWaypointIds.collectAsState()
    
    val startType by viewModel.startPointType.collectAsState()
    val startPinId by viewModel.selectedStartPinId.collectAsState()
    val customStartName by viewModel.customStartName.collectAsState()
    val customStartLat by viewModel.customStartLat.collectAsState()
    val customStartLng by viewModel.customStartLng.collectAsState()

    val endType by viewModel.endPointType.collectAsState()
    val endPinId by viewModel.selectedEndPinId.collectAsState()
    val customEndName by viewModel.customEndName.collectAsState()
    val customEndLat by viewModel.customEndLat.collectAsState()
    val customEndLng by viewModel.customEndLng.collectAsState()

    val isExtracting by viewModel.isExtracting.collectAsState()
    val extractionError by viewModel.extractionError.collectAsState()
    val isOfflineMode by viewModel.isOfflineMode.collectAsState()

    val context = LocalContext.current

    var bulkInputText by remember { mutableStateOf("") }
    var routeSaveName by remember { mutableStateOf("") }
    var routeSaveNotes by remember { mutableStateOf("") }
    var showSaveRouteDialog by remember { mutableStateOf(false) }

    var showStartSetupDialog by remember { mutableStateOf(false) }
    var showEndSetupDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App offline notice indicator
        if (isOfflineMode) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, shape = RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "دون اتصال بالإنترنت",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "أنت تعمل حالياً في وضع عدم الاتصال (أوفلاين) 📴. جميع مميزات التخطيط، التحسين الجغرافي وحفظ المسارات تعمل بالكامل محلياً وبسرعة فائقة دون الحاجة لشبكة الإنترنت!",
                            style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Section 1: Extraction & Input
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().shadow(3.dp, shape = RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "إضافة وتصنيف المواقع تلقائياً بـ Gemini",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = (-0.2).sp),
                        color = if (isOfflineMode) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isOfflineMode) 
                            "تتطلب ميزة استخراج وتصنيف المواقع بالذكاء الاصطناعي اتصالاً بالإنترنت. يمكنك دائماً إضافة دبابيس جديدة يدوياً والتحكم في المسارات بالكامل!"
                            else "ألصق قائمة دبابيس من خرائط جوجل أو اكتب أسماء الأماكن وسنقوم باستخراجها وتصنيفها على الخريطة فوراً:",
                        style = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = bulkInputText,
                        onValueChange = { bulkInputText = it },
                        placeholder = { 
                            Text(
                                if (isOfflineMode) "مغلق مؤقتاً في وضع أوفلاين..." 
                                else "مثال: مطعم البيك، كافيه وودز، https://maps.app.goo.gl/..."
                            ) 
                        },
                        enabled = !isOfflineMode,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isExtracting) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("جاري الاستخراج والتصنيف...", style = TextStyle(fontSize = 12.sp))
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (bulkInputText.isNotBlank()) {
                                        viewModel.extractPinsUsingAI(bulkInputText)
                                        bulkInputText = ""
                                    }
                                },
                                enabled = bulkInputText.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("استخراج وتصنيف", fontSize = 13.sp)
                            }
                        }

                        if (pins.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.clearAllPins() },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مسح كل الدبابيس", fontSize = 12.sp)
                            }
                        }
                    }

                    extractionError?.let { err ->
                        Text(
                            text = err,
                            style = TextStyle(color = MaterialTheme.colorScheme.error, fontSize = 12.sp),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Section 2: Start Point & Destination Configurations
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().shadow(3.dp, shape = RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "نقاط البداية والنهاية للمسار",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = (-0.2).sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Origin Setup Info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartSetupDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(getCategoryColor("انطلاق").copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = getCategoryColor("انطلاق"), modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("نقطة الانطلاق (البداية)", style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                            Text(
                                text = if (startType == "pin") pins.find { it.id == startPinId }?.name ?: "غير محدد" else customStartName,
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Destination Setup Info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEndSetupDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(getCategoryColor("نهاية").copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = null, tint = getCategoryColor("نهاية"), modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الوجهة النهائية (النهاية)", style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                            Text(
                                text = if (endType == "pin") pins.find { it.id == endPinId }?.name ?: "غير محدد" else customEndName,
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Section 3: Waypoint/Pin Selector Checklist
        if (pins.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "اختر المواقع لتضمينها في المسار (${selectedWaypoints.size}/${pins.size})",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { viewModel.selectAllWaypoints(pins.map { it.id }) }) {
                            Text("تحديد الكل", fontSize = 11.sp)
                        }
                        TextButton(onClick = { viewModel.selectAllWaypoints(emptyList()) }) {
                            Text("إلغاء التحديد", fontSize = 11.sp)
                        }
                    }
                }
            }

            items(pins) { pin ->
                val isChecked = selectedWaypoints.contains(pin.id)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleWaypoint(pin.id) },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { viewModel.toggleWaypoint(pin.id) }
                        )

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(getCategoryColor(pin.category), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(pin.category),
                                contentDescription = pin.category,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pin.name,
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${pin.category} • بقاء ${pin.timeSpentMinutes} دقيقة",
                                style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }

                        IconButton(onClick = { viewModel.deletePin(pin.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف الدبوس", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.optimizeRoute() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Route, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ترتيب وتحسين المسار للحد الأدنى من الوقت والجهد ⚡", fontSize = 14.sp)
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            "لا توجد دبابيس مضافة حالياً.",
                            style = TextStyle(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "الرجاء إضافة دبابيس يدوياً أو باستخدام مساعد الـ AI في الأعلى للبدء بتخطيط مسارك.",
                            style = TextStyle(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section 4: Optimized Route Results Display (if exists)
        activeRoute?.let { route ->
            item {
                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 2.dp)
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "المسار المحسّن والأوقات المقدرة لرحلتك 🗺️",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().shadow(4.dp, shape = RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header info: Origin -> Destination Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("نقطة الانطلاق", style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                                Text(route.startName, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp), maxLines = 1)
                            }
                            
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "مسار",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp).padding(horizontal = 4.dp)
                            )
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text("الوجهة النهائية", style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                                Text(route.endName, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp), maxLines = 1)
                            }
                        }

                        // Sleek 3-column quick stats grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Column 1: Time
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "الوقت المقدر",
                                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val hours = route.totalDurationMinutes / 60
                                    val mins = route.totalDurationMinutes % 60
                                    val timeStr = if (hours > 0) "${hours}س ${mins}د" else "${mins} دقيقة"
                                    Text(
                                        text = timeStr,
                                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }

                            // Column 2: Distance
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "المسافة الكلية",
                                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.1f كم", route.totalDistanceKm),
                                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    )
                                }
                            }

                            // Column 3: Stops
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "المحطات",
                                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${route.steps.size} نقاط",
                                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Step-by-Step sequence cards showing ETAs
            items(route.steps) { step ->
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, shape = RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Vertical accent bar matching getCategoryColor(step.pin.category)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(6.dp)
                                .background(getCategoryColor(step.pin.category))
                        )
                        
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Sequence numeric counter badge
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(getCategoryColor(step.pin.category).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${step.sequenceNumber}",
                                    style = TextStyle(color = getCategoryColor(step.pin.category), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.pin.name,
                                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (step.distanceFromPreviousKm > 0) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "تبعد %.2f كم (تنقل %d دقيقة)", step.distanceFromPreviousKm, step.durationFromPreviousMinutes),
                                        style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                } else {
                                    Text("نقطة الانطلاق والتحرك الأولى", style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.primary))
                                }
                            }

                            // Time Alarm/Notification block showing ETA & Departure times
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = "الوصول: ${step.arrivalTime}",
                                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    )
                                }
                                if (step.departureTime != "--") {
                                    Text(
                                        text = "المغادرة: ${step.departureTime}",
                                        style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Export or Save Action Trigger row buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showSaveRouteDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ المسار المفضل")
                    }

                    Button(
                        onClick = {
                            val url = viewModel.generateGoogleMapsDirectionUrl()
                            if (url.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "الرجاء تأكيد توليد المسار أولاً.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير لخرائط Google 🗺️")
                    }
                }
            }
        }
    }

    // Save Route dialog popup
    if (showSaveRouteDialog) {
        Dialog(onDismissRequest = { showSaveRouteDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "حفظ المسار المقترح كمسار مفضل",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = routeSaveName,
                        onValueChange = { routeSaveName = it },
                        label = { Text("اسم المسار (مثال: جولة كافيهات الرياض)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = routeSaveNotes,
                        onValueChange = { routeSaveNotes = it },
                        label = { Text("ملاحظات إضافية للمسار") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { showSaveRouteDialog = false }) {
                            Text("إلغاء")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (routeSaveName.isNotBlank()) {
                                    viewModel.saveCurrentOptimizedRoute(routeSaveName, routeSaveNotes.ifBlank { null })
                                    routeSaveName = ""
                                    routeSaveNotes = ""
                                    showSaveRouteDialog = false
                                    Toast.makeText(context, "تم حفظ المسار بنجاح في المفضلة!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = routeSaveName.isNotBlank()
                        ) {
                            Text("حفظ الآن")
                        }
                    }
                }
            }
        }
    }

    // Origin dialog configurations
    if (showStartSetupDialog) {
        LocationConfigDialog(
            title = "تعديل نقطة الانطلاق والبداية",
            pins = pins,
            currentType = startType,
            currentPinId = startPinId,
            currentCustomName = customStartName,
            currentLat = customStartLat,
            currentLng = customStartLng,
            onDismiss = { showStartSetupDialog = false },
            onConfirm = { type, pinId, name, lat, lng ->
                viewModel.setStartPoint(type, pinId, name, lat, lng)
                showStartSetupDialog = false
            }
        )
    }

    // Destination dialog configurations
    if (showEndSetupDialog) {
        LocationConfigDialog(
            title = "تعديل الوجهة والنهاية النهائية",
            pins = pins,
            currentType = endType,
            currentPinId = endPinId,
            currentCustomName = customEndName,
            currentLat = customEndLat,
            currentLng = customEndLng,
            onDismiss = { showEndSetupDialog = false },
            onConfirm = { type, pinId, name, lat, lng ->
                viewModel.setEndPoint(type, pinId, name, lat, lng)
                showEndSetupDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationConfigDialog(
    title: String,
    pins: List<SavedPin>,
    currentType: String,
    currentPinId: Int?,
    currentCustomName: String,
    currentLat: Double,
    currentLng: Double,
    onDismiss: () -> Unit,
    onConfirm: (String, Int?, String, Double, Double) -> Unit
) {
    var type by remember { mutableStateOf(currentType) } // "custom" or "pin"
    var pinIdSelected by remember { mutableStateOf(currentPinId ?: pins.firstOrNull()?.id) }
    var customName by remember { mutableStateOf(currentCustomName) }
    var latStr by remember { mutableStateOf(currentLat.toString()) }
    var lngStr by remember { mutableStateOf(currentLng.toString()) }

    var pinsExpanded by remember { mutableStateOf(false) }

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = title,
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = type == "custom",
                                onClick = { type = "custom" }
                            )
                            Text("إدخال موقع مخصص")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = type == "pin",
                                onClick = { type = "pin" },
                                enabled = pins.isNotEmpty()
                            )
                            Text("اختيار من الدبابيس")
                        }
                    }
                }

                if (type == "custom") {
                    item {
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("اسم الموقع المخصص") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                } else if (pins.isNotEmpty()) {
                    item {
                        val currentSelectedPinText = pins.find { it.id == pinIdSelected }?.name ?: "اختر دبابيس موقع"
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = currentSelectedPinText,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("الدبوس المحدد") },
                                trailingIcon = {
                                    IconButton(onClick = { pinsExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = pinsExpanded,
                                onDismissRequest = { pinsExpanded = false }
                            ) {
                                pins.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.name) },
                                        onClick = {
                                            pinIdSelected = p.id
                                            pinsExpanded = false
                                        }
                                    )
                                }
                            }
                        }
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
                                if (type == "custom" && customName.isNotBlank()) {
                                    val finalLat = latStr.toDoubleOrNull() ?: currentLat
                                    val finalLng = lngStr.toDoubleOrNull() ?: currentLng
                                    onConfirm("custom", null, customName, finalLat, finalLng)
                                } else if (type == "pin" && pinIdSelected != null) {
                                    val selPin = pins.find { it.id == pinIdSelected }
                                    if (selPin != null) {
                                        onConfirm("pin", pinIdSelected, selPin.name, selPin.latitude, selPin.longitude)
                                    }
                                }
                            }
                        ) {
                            Text("حفظ التغييرات")
                        }
                    }
                }
            }
        }
    }
}
