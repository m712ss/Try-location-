package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.*
import com.example.data.AppDatabase
import com.example.data.MapRouteDao
import com.example.data.MapRouteRepository
import com.example.data.SavedPin
import com.example.data.SavedRoute
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.*

// Message data class for chatbot
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// UI state for route optimization and maps
data class OptimizedPathStep(
    val pin: SavedPin,
    val sequenceNumber: Int,
    val arrivalTime: String,
    val departureTime: String,
    val distanceFromPreviousKm: Double,
    val durationFromPreviousMinutes: Int
)

data class RoutePlanResult(
    val steps: List<OptimizedPathStep>,
    val totalDistanceKm: Double,
    val totalDurationMinutes: Int,
    val travelTimeMinutes: Int,
    val startName: String,
    val endName: String
)

class MapRouteViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = MapRouteRepository(database.dao())

    // Simulated Offline Mode state (persisted computed routes accessibility)
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    fun toggleOfflineMode() {
        _isOfflineMode.value = !_isOfflineMode.value
    }

    // All pins and saved routes from SQLite
    val allPins: StateFlow<List<SavedPin>> = repository.allPins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRoutes: StateFlow<List<SavedRoute>> = repository.allRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active screen selection
    private val _currentTab = MutableStateFlow(0) // 0: Map, 1: Planner, 2: Chat, 3: Saved Routes
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Chatbot state
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "مرحباً بك! أنا مساعد الخرائط الذكي. يمكنك إرسال روابط أو قائمة بالمواقع التي تريد زيارتها، وسأقوم باستخراجها وتصنيفها وتنظيمها لك في مسار مثالي.",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // AI pin extraction state
    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    private val _extractionError = MutableStateFlow<String?>(null)
    val extractionError: StateFlow<String?> = _extractionError.asStateFlow()

    // Interactive map states (pan, zoom, selected pin)
    private val _mapZoom = MutableStateFlow(12f)
    val mapZoom: StateFlow<Float> = _mapZoom.asStateFlow()

    private val _mapCenter = MutableStateFlow(Pair(24.7136, 46.6753)) // Latitude, Longitude (Riyadh defaults)
    val mapCenter: StateFlow<Pair<Double, Double>> = _mapCenter.asStateFlow()

    private val _selectedPin = MutableStateFlow<SavedPin?>(null)
    val selectedPin: StateFlow<SavedPin?> = _selectedPin.asStateFlow()

    // Planning parameters
    private val _startPointType = MutableStateFlow("custom") // "custom" or "pin"
    val startPointType: StateFlow<String> = _startPointType.asStateFlow()

    private val _selectedStartPinId = MutableStateFlow<Int?>(null)
    val selectedStartPinId: StateFlow<Int?> = _selectedStartPinId.asStateFlow()

    private val _customStartName = MutableStateFlow("موقع الانطلاق")
    val customStartName: StateFlow<String> = _customStartName.asStateFlow()

    private val _customStartLat = MutableStateFlow(24.7136)
    val customStartLat: StateFlow<Double> = _customStartLat.asStateFlow()

    private val _customStartLng = MutableStateFlow(46.6753)
    val customStartLng: StateFlow<Double> = _customStartLng.asStateFlow()

    private val _endPointType = MutableStateFlow("custom") // "custom" or "pin"
    val endPointType: StateFlow<String> = _endPointType.asStateFlow()

    private val _selectedEndPinId = MutableStateFlow<Int?>(null)
    val selectedEndPinId: StateFlow<Int?> = _selectedEndPinId.asStateFlow()

    private val _customEndName = MutableStateFlow("الوجهة النهائية")
    val customEndName: StateFlow<String> = _customEndName.asStateFlow()

    private val _customEndLat = MutableStateFlow(24.7250)
    val customEndLat: StateFlow<Double> = _customEndLat.asStateFlow()

    private val _customEndLng = MutableStateFlow(46.6950)
    val customEndLng: StateFlow<Double> = _customEndLng.asStateFlow()

    // Selected pins to be optimized (by default all checkeds or we can allow marking)
    private val _selectedWaypointIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedWaypointIds: StateFlow<Set<Int>> = _selectedWaypointIds.asStateFlow()

    // Active optimized route results
    private val _optimizedRoute = MutableStateFlow<RoutePlanResult?>(null)
    val optimizedRoute: StateFlow<RoutePlanResult?> = _optimizedRoute.asStateFlow()

    private val _routeErrorMessage = MutableStateFlow<String?>(null)
    val routeErrorMessage: StateFlow<String?> = _routeErrorMessage.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("الكل")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    fun setZoom(zoom: Float) {
        _mapZoom.value = zoom.coerceIn(5f, 18f)
    }

    fun panMap(deltaX: Double, deltaY: Double) {
        // Simple conversion from screen delta to LatLng relative changes depending on zoom level
        val scale = 360.0 / (Math.pow(2.0, _mapZoom.value.toDouble()) * 256.0)
        val newLat = _mapCenter.value.first - deltaY * scale
        val newLng = _mapCenter.value.second + deltaX * scale
        _mapCenter.value = Pair(newLat, newLng)
    }

    fun selectPin(pin: SavedPin?) {
        _selectedPin.value = pin
        if (pin != null) {
            _mapCenter.value = Pair(pin.latitude, pin.longitude)
        }
    }

    fun setStartPoint(type: String, pinId: Int?, name: String, lat: Double, lng: Double) {
        _startPointType.value = type
        _selectedStartPinId.value = pinId
        _customStartName.value = name
        _customStartLat.value = lat
        _customStartLng.value = lng
    }

    fun setEndPoint(type: String, pinId: Int?, name: String, lat: Double, lng: Double) {
        _endPointType.value = type
        _selectedEndPinId.value = pinId
        _customEndName.value = name
        _customEndLat.value = lat
        _customEndLng.value = lng
    }

    fun toggleWaypoint(pinId: Int) {
        val currentSet = _selectedWaypointIds.value.toMutableSet()
        if (currentSet.contains(pinId)) {
            currentSet.remove(pinId)
        } else {
            currentSet.add(pinId)
        }
        _selectedWaypointIds.value = currentSet
    }

    fun selectAllWaypoints(pinIds: List<Int>) {
        _selectedWaypointIds.value = pinIds.toSet()
    }

    // SQLite operations wrapper
    fun addNewManualPin(name: String, category: String, description: String?, lat: Double, lng: Double, originalUrl: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val pin = SavedPin(
                name = name,
                category = category,
                description = description,
                latitude = lat,
                longitude = lng,
                originalUrl = originalUrl
            )
            repository.insertPin(pin)
            withContext(Dispatchers.Main) {
                _mapCenter.value = Pair(lat, lng)
            }
        }
    }

    fun deletePin(pinId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePinById(pinId)
            if (_selectedPin.value?.id == pinId) {
                _selectedPin.value = null
            }
            val currentSet = _selectedWaypointIds.value.toMutableSet()
            if (currentSet.remove(pinId)) {
                _selectedWaypointIds.value = currentSet
            }
        }
    }

    fun clearAllPins() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearPins()
            _selectedPin.value = null
            _selectedWaypointIds.value = emptySet()
            _optimizedRoute.value = null
        }
    }

    // AI parsing of unstructured user text containing links or place names
    fun extractPinsUsingAI(rawInput: String) {
        if (rawInput.isBlank()) return
        _isExtracting.value = true
        _extractionError.value = null

        viewModelScope.launch {
            try {
                if (_isOfflineMode.value) {
                    throw IllegalStateException("أنت تعمل حالياً في وضع عدم الاتصال بالإنترنت (أوفلاين) 📴. لاستخراج المواقع وتصنيفها بالذكاء الاصطناعي، يرجى العودة للوضع المتصل من أعلى الشاشة.")
                }

                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw IllegalStateException("مفتاح واجهة برمجة التطبيقات لـ Gemini غير متوفر. الرجاء إعداده عبر لوحة الأسرار في AI Studio.")
                }

                val systemPrompt = """
                    أنت خبير خرائط ومواقع جغرافي ذكي جداً. مهمتك هي استخراج وتصنيف وتحليل المواقع التي يرسلها المستخدم.
                    المستخدم قد يرسل أسماء مواقع، روابط خرائط جوجل (مثل goo.gl أو maps.app.goo.gl)، أو نصوصاً مختلطة.
                    قم بتحليل النص واستخراج قائمة بالمواقع. لكل موقع، حدد المعطيات التالية بدقة:
                    - الاسم (name): اسم الموقع باللغة العربية.
                    - التصنيف (category): يجب أن يكون أحد التصنيفات التالية بدقة: "عمل", "طعام وشراب", "تسوق", "ترفيه", "سياحة", "إقامة", "أخرى".
                    - الوصف (description): وصف موجز ومفيد باللغة العربية (ما هو المكان وسبب زيارته في سطر واحد).
                    - خط العرض (latitude) وخط الطول (longitude): إحداثيات الموقع الحقيقية في السعودية. إذا كان المكان معروفاً جداً مثل (برج المملكة، حديقة السلام، مطعم البيك)، قم بتوفير إحداثياته الحقيقية في الرياض أو جدة أو مكة إلخ بدقة. إذا لم يكن المكان معروفاً، خمن موقعه في الرياض (مثلاً حول خط العرض 24.7136 وخط الطول 46.6753) مع تغيير بسيط لتوزيعه على خريطة تفاعلية (مثلاً إضافة أو طرح 0.01 إلى 0.05 عشوائياً للانتشار الجيد).
                    
                    يجب أن تعيد النتيجة بصيغة JSON نظيفة جداً كقائمة من الكائنات (JSON Array) دون أي هوامش، ودون وضع كتل النص للـ markdown وإلا سيفشل البرنامج في معالجتها.
                    مثال على الرد المطلوب:
                    [
                      {
                        "name": "برج المملكة الرياض",
                        "category": "سياحة",
                        "description": "أحد أشهر معالم الرياض يحتوي على جسر معلق وإطلالة بانورامية رائعة.",
                        "latitude": 24.7114,
                        "longitude": 46.6744,
                        "timeSpentMinutes": 60
                      }
                    ]
                """.trimIndent()

                val prompt = "قم باستخراج المواقع من النص التالي وتصنيفها:\n$rawInput"

                val request = MoshiGeminiRequest(
                    contents = listOf(
                        MoshiContent(parts = listOf(MoshiPart(text = prompt)))
                    ),
                    generationConfig = MoshiGenerationConfig(
                        temperature = 0.3,
                        responseMimeType = "application/json"
                    ),
                    systemInstruction = MoshiContent(parts = listOf(MoshiPart(text = systemPrompt))),
                    tools = listOf(MoshiTool(googleMaps = emptyMap())) // Use Google Maps grounding capability!
                )

                val response = RetrofitGeminiClient.service.generateContent(
                    model = "gemini-3.5-flash", // Use general flash with tools grounding!
                    apiKey = apiKey,
                    request = request
                )

                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (responseText.isNullOrBlank()) {
                    throw IllegalStateException("لم أستطع الحصول على استجابة صالحة من المساعد.")
                }

                // Clean the response text from markdown block wrappers if present
                val cleanedText = responseText
                    .replace(Regex("(?i)```json\\s*"), "")
                    .replace("```", "")
                    .trim()

                withContext(Dispatchers.IO) {
                    val jsonArray = JSONArray(cleanedText)
                    val extractedCount = jsonArray.length()

                    for (i in 0 until extractedCount) {
                        val obj = jsonArray.getJSONObject(i)
                        val name = obj.getString("name")
                        val category = obj.getString("category")
                        val description = obj.optString("description", "")
                        val lat = obj.getDouble("latitude")
                        val lng = obj.getDouble("longitude")
                        val timeSpent = obj.optInt("timeSpentMinutes", 45)

                        val pin = SavedPin(
                            name = name,
                            category = category,
                            description = description,
                            latitude = lat,
                            longitude = lng,
                            originalUrl = null,
                            timeSpentMinutes = timeSpent
                        )
                        repository.insertPin(pin)
                    }
                }

                _extractionError.value = null
            } catch (e: Exception) {
                Log.e("MapRouteViewModel", "Error extracting pins", e)
                _extractionError.value = "حدث خطأ أثناء استخراج الدبابيس: ${e.localizedMessage ?: "تحقق من اتصال الإنترنت أو مفتاح الـ API"}"
            } finally {
                _isExtracting.value = false
            }
        }
    }

    // Execute route optimization from the designated start point to the end point
    fun optimizeRoute() {
        val pins = allPins.value.filter { _selectedWaypointIds.value.contains(it.id) }
        
        // Resolve start point coordinates
        val startLat: Double
        val startLng: Double
        val startName: String
        if (_startPointType.value == "pin") {
            val startPin = allPins.value.find { it.id == _selectedStartPinId.value }
            if (startPin == null) {
                _routeErrorMessage.value = "الرجاء تحديد نقطة انطلاق صالحة من الدبابيس."
                return
            }
            startLat = startPin.latitude
            startLng = startPin.longitude
            startName = startPin.name
        } else {
            startLat = _customStartLat.value
            startLng = _customStartLng.value
            startName = _customStartName.value
        }

        // Resolve end point coordinates
        val endLat: Double
        val endLng: Double
        val endName: String
        if (_endPointType.value == "pin") {
            val endPin = allPins.value.find { it.id == _selectedEndPinId.value }
            if (endPin == null) {
                _routeErrorMessage.value = "الرجاء تحديد نقطة نهاية صالحة من الدبابيس."
                return
            }
            endLat = endPin.latitude
            endLng = endPin.longitude
            endName = endPin.name
        } else {
            endLat = _customEndLat.value
            endLng = _customEndLng.value
            endName = _customEndName.value
        }

        _routeErrorMessage.value = null

        viewModelScope.launch(Dispatchers.Default) {
            try {
                if (pins.isEmpty()) {
                    // Just direct route from start to end
                    val directDist = calculateHaversineDistance(startLat, startLng, endLat, endLng)
                    val directTravelTime = (directDist / 40.0 * 60.0).toInt().coerceAtLeast(3)
                    
                    val steps = listOf(
                        OptimizedPathStep(
                            pin = SavedPin(name = startName, latitude = startLat, longitude = startLng, category = "انطلاق"),
                            sequenceNumber = 1,
                            arrivalTime = "09:00 ص",
                            departureTime = "09:05 ص",
                            distanceFromPreviousKm = 0.0,
                            durationFromPreviousMinutes = 0
                        ),
                        OptimizedPathStep(
                            pin = SavedPin(name = endName, latitude = endLat, longitude = endLng, category = "نهاية"),
                            sequenceNumber = 2,
                            arrivalTime = formatArrivalTime(9, 5, directTravelTime),
                            departureTime = "--",
                            distanceFromPreviousKm = directDist,
                            durationFromPreviousMinutes = directTravelTime
                        )
                    )

                    withContext(Dispatchers.Main) {
                        _optimizedRoute.value = RoutePlanResult(
                            steps = steps,
                            totalDistanceKm = directDist,
                            totalDurationMinutes = directTravelTime + 5,
                            travelTimeMinutes = directTravelTime,
                            startName = startName,
                            endName = endName
                        )
                    }
                    return@launch
                }

                // Compute Nearest Neighbor Route Optimization
                val uncalculated = pins.toMutableList()
                val orderedWaypoints = mutableListOf<SavedPin>()
                
                var currentLat = startLat
                var currentLng = startLng

                while (uncalculated.isNotEmpty()) {
                    var nearestIndex = 0
                    var minDistance = Double.MAX_VALUE

                    for (i in uncalculated.indices) {
                        val d = calculateHaversineDistance(currentLat, currentLng, uncalculated[i].latitude, uncalculated[i].longitude)
                        if (d < minDistance) {
                            minDistance = d
                            nearestIndex = i
                        }
                    }

                    val nextStop = uncalculated.removeAt(nearestIndex)
                    orderedWaypoints.add(nextStop)
                    currentLat = nextStop.latitude
                    currentLng = nextStop.longitude
                }

                // Construct full path steps
                val fullSteps = mutableListOf<OptimizedPathStep>()
                var totalDist = 0.0
                var currHour = 9
                var currMin = 0

                // 1. Add Start point
                fullSteps.add(
                    OptimizedPathStep(
                        pin = SavedPin(name = startName, latitude = startLat, longitude = startLng, category = "انطلاق"),
                        sequenceNumber = 1,
                        arrivalTime = formatTime(currHour, currMin),
                        departureTime = formatTime(currHour, currMin + 10),
                        distanceFromPreviousKm = 0.0,
                        durationFromPreviousMinutes = 0
                    )
                )
                currMin += 10
                if (currMin >= 60) {
                    currHour += currMin / 60
                    currMin %= 60
                }

                var prevLat = startLat
                var prevLng = startLng

                // 2. Add intermediate optimized waypoints
                var seq = 2
                var travelMinSum = 0

                for (wp in orderedWaypoints) {
                    val dist = calculateHaversineDistance(prevLat, prevLng, wp.latitude, wp.longitude)
                    totalDist += dist
                    val travelMin = (dist / 40.0 * 60.0).toInt().coerceAtLeast(3) // 40 km/h avg speed
                    travelMinSum += travelMin

                    // Arrival at waypoint
                    currMin += travelMin
                    if (currMin >= 60) {
                        currHour += currMin / 60
                        currMin %= 60
                    }
                    val arrivalStr = formatTime(currHour, currMin)

                    // Departure from waypoint after visit duration
                    val durationAtStop = wp.timeSpentMinutes
                    val depHour = currHour
                    val depMin = currMin + durationAtStop
                    val departureStr = formatTime(depHour + (depMin / 60), depMin % 60)

                    fullSteps.add(
                        OptimizedPathStep(
                            pin = wp,
                            sequenceNumber = seq++,
                            arrivalTime = arrivalStr,
                            departureTime = departureStr,
                            distanceFromPreviousKm = dist,
                            durationFromPreviousMinutes = travelMin
                        )
                    )

                    currMin += durationAtStop
                    if (currMin >= 60) {
                        currHour += currMin / 60
                        currMin %= 60
                    }

                    prevLat = wp.latitude
                    prevLng = wp.longitude
                }

                // 3. Add End point
                val lastDist = calculateHaversineDistance(prevLat, prevLng, endLat, endLng)
                totalDist += lastDist
                val lastTravelTime = (lastDist / 40.0 * 60.0).toInt().coerceAtLeast(3)
                travelMinSum += lastTravelTime

                currMin += lastTravelTime
                if (currMin >= 60) {
                    currHour += currMin / 60
                    currMin %= 60
                }

                fullSteps.add(
                    OptimizedPathStep(
                        pin = SavedPin(name = endName, latitude = endLat, longitude = endLng, category = "نهاية"),
                        sequenceNumber = seq,
                        arrivalTime = formatTime(currHour, currMin),
                        departureTime = "--",
                        distanceFromPreviousKm = lastDist,
                        durationFromPreviousMinutes = lastTravelTime
                    )
                )

                val planResult = RoutePlanResult(
                    steps = fullSteps,
                    totalDistanceKm = totalDist,
                    totalDurationMinutes = travelMinSum + (pins.size * 45) + 10,
                    travelTimeMinutes = travelMinSum,
                    startName = startName,
                    endName = endName
                )

                withContext(Dispatchers.Main) {
                    _optimizedRoute.value = planResult
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _routeErrorMessage.value = "حدث خطأ أثناء تعديل وتحسين المسار: ${e.localizedMessage}"
                }
            }
        }
    }

    // Save current optimized route to historic SQLite Database
    fun saveCurrentOptimizedRoute(nameOfRoute: String, notes: String?) {
        val activeRoute = _optimizedRoute.value ?: return
        
        // Serialize steps into JSON array string
        val stepsList = activeRoute.steps
        val jsonArray = JSONArray()
        for (step in stepsList) {
            val obj = JSONObject()
            obj.put("pinId", step.pin.id)
            obj.put("name", step.pin.name)
            obj.put("category", step.pin.category)
            obj.put("lat", step.pin.latitude)
            obj.put("lng", step.pin.longitude)
            obj.put("seq", step.sequenceNumber)
            obj.put("arr", step.arrivalTime)
            obj.put("dep", step.departureTime)
            obj.put("dist", step.distanceFromPreviousKm)
            obj.put("time", step.durationFromPreviousMinutes)
            jsonArray.put(obj)
        }

        val firstStep = stepsList.first()
        val lastStep = stepsList.last()

        viewModelScope.launch(Dispatchers.IO) {
            val dbRoute = SavedRoute(
                name = nameOfRoute,
                originName = firstStep.pin.name,
                originLat = firstStep.pin.latitude,
                originLng = firstStep.pin.longitude,
                destName = lastStep.pin.name,
                destLat = lastStep.pin.latitude,
                destLng = lastStep.pin.longitude,
                optimizedOrderJson = jsonArray.toString(),
                totalDistanceKm = activeRoute.totalDistanceKm,
                totalDurationMinutes = activeRoute.totalDurationMinutes,
                notes = notes
            )
            repository.insertRoute(dbRoute)
        }
    }

    fun deleteSavedRoute(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRouteById(id)
        }
    }

    fun toggleRouteFavorite(id: Int, isFav: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRouteFavorite(id, isFav)
        }
    }

    fun loadSavedRouteToMap(saved: SavedRoute) {
        // Parse the optimization order JSON mapping and restore route result
        try {
            val jsonArray = JSONArray(saved.optimizedOrderJson)
            val restoredSteps = mutableListOf<OptimizedPathStep>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val pinId = obj.optInt("pinId", 0)
                val name = obj.getString("name")
                val category = obj.getString("category")
                val lat = obj.getDouble("lat")
                val lng = obj.getDouble("lng")
                val seq = obj.getInt("seq")
                val arr = obj.getString("arr")
                val dep = obj.getString("dep")
                val dist = obj.getDouble("dist")
                val time = obj.getInt("time")

                restoredSteps.add(
                    OptimizedPathStep(
                        pin = SavedPin(id = pinId, name = name, category = category, latitude = lat, longitude = lng),
                        sequenceNumber = seq,
                        arrivalTime = arr,
                        departureTime = dep,
                        distanceFromPreviousKm = dist,
                        durationFromPreviousMinutes = time
                    )
                )
            }

            _optimizedRoute.value = RoutePlanResult(
                steps = restoredSteps,
                totalDistanceKm = saved.totalDistanceKm,
                totalDurationMinutes = saved.totalDurationMinutes,
                travelTimeMinutes = (saved.totalDistanceKm / 40.0 * 60.0).toInt(),
                startName = saved.originName,
                endName = saved.destName
            )
            
            // Adjust map view around start point
            _mapCenter.value = Pair(saved.originLat, saved.originLng)
            _currentTab.value = 0 // Navigate to map

        } catch (e: Exception) {
            Log.e("MapRouteViewModel", "Error loading saved route", e)
        }
    }

    // Google Maps multi-stop direction URL exported format maker
    fun generateGoogleMapsDirectionUrl(): String {
        val activeRoute = _optimizedRoute.value ?: return ""
        val steps = activeRoute.steps
        
        val origin = "${steps.first().pin.latitude},${steps.first().pin.longitude}"
        val destination = "${steps.last().pin.latitude},${steps.last().pin.longitude}"
        
        val waypoints = if (steps.size > 2) {
            val wpList = steps.subList(1, steps.size - 1).map { "${it.pin.latitude},${it.pin.longitude}" }
            wpList.joinToString("|")
        } else {
            ""
        }

        return if (waypoints.isNotEmpty()) {
            "https://www.google.com/maps/dir/?api=1&origin=$origin&destination=$destination&waypoints=${java.net.URLEncoder.encode(waypoints, "UTF-8")}&travelmode=driving"
        } else {
            "https://www.google.com/maps/dir/?api=1&origin=$origin&destination=$destination&travelmode=driving"
        }
    }

    // Send chat prompt to Gemini with thinking configuration option enabled
    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        
        val userMsg = ChatMessage(text = userText, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true

        viewModelScope.launch {
            if (_isOfflineMode.value) {
                kotlinx.coroutines.delay(600) // Small simulation delay
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    text = "أنا غير قادر على الاتصال بخادم المساعد الذكي بالذكاء الاصطناعي حالياً لأنك قمت بتشغيل 'وضع عدم الاتصال بالإنترنت' (أوفلاين) 📴.\n\nلكن الخبر السار! 💫 يمكنك الاستمرار في تخطيط مساراتك، تحسينها وموازنة الأوقات محلياً بنسبة 100٪، بالإضافة إلى تصفح وحفظ دبابيسك ومساراتك المفضلة، حيث يعمل التطبيق بالكامل بالاعتماد على التخزين المحلي والذكاء المحلي المدمج للتحسين الجغرافي!",
                    isUser = false
                )
                _isChatLoading.value = false
                return@launch
            }

            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw IllegalStateException("مفتاح واجهة برمجة التطبيقات لـ Gemini غير متوفر. الرجاء إعداده عبر لوحة الأسرار في AI Studio.")
                }

                // Construct conversation history
                val history = _chatMessages.value.map { msg ->
                    MoshiContent(
                        role = if (msg.isUser) "user" else "model",
                        parts = listOf(MoshiPart(text = msg.text))
                    )
                }

                val systemPrompt = """
                    أنت خبير كلي ومعلم خرائط ومسارات ذكي وتتحدث العربية الفصحى الجميلة والودودة.
                    تساعد المستخدمين في إيجاد أفضل الطرق، وتصنيف الأماكن (عمل، طعام وشراب، تسوق، ترفيه، سياحة، إقامة، أخرى)، وتخطيط جداولهم اليومية لزيارة المواقع بكفاءة وتوفير الوقت والجهد وعرض الأوقات المقدرة للوصول.
                    عند تصنيف أماكن أو تحسين مسارات، قم بذكر أسماء الأماكن بوضوح واشرح سبب الترتيب (الترتيب قائم على اختصار المسافة الجغرافية).
                    يمكنك استخدام الرموز التعبيرية لإعطاء جمالية لحديثك.
                """.trimIndent()

                val request = MoshiGeminiRequest(
                    contents = history,
                    generationConfig = MoshiGenerationConfig(
                        temperature = 0.5,
                        thinkingConfig = MoshiThinkingConfig(thinkingLevel = "high") // Enable High Thinking Level!
                    ),
                    systemInstruction = MoshiContent(parts = listOf(MoshiPart(text = systemPrompt))),
                    tools = listOf(MoshiTool(googleMaps = emptyMap())) // Enable Google Maps Grouding tool!
                )

                // High Thinking uses gemini-3.1-pro-preview!
                val response = RetrofitGeminiClient.service.generateContent(
                    model = "gemini-3.1-pro-preview", // Complex task uses 3.1-pro-preview indeed!
                    apiKey = apiKey,
                    request = request
                )

                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "عذراً، لم أستطع فهم ذلك، يرجى المحاولة مرة أخرى."

                _chatMessages.value = _chatMessages.value + ChatMessage(text = replyText, isUser = false)

            } catch (e: Exception) {
                Log.e("MapRouteViewModel", "Error in Gemini Chat", e)
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    text = "حدث خطأ أثناء معالجة رسالتك بالذكاء الاصطناعي: ${e.localizedMessage ?: "تأكد من إعداد مفتاح الـ API واتصالك بالإنترنت"}",
                    isUser = false
                )
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    // Helper functions
    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * asin(sqrt(a))
        return r * c
    }

    private fun formatTime(hour: Int, min: Int): String {
        var h = hour
        val amPm = if (h >= 12) {
            if (h > 12) h -= 12
            "م"
        } else {
            if (h == 0) h = 12
            "ص"
        }
        return String.format(Locale.getDefault(), "%02d:%02d %s", h, min, amPm)
    }

    private fun formatArrivalTime(startHour: Int, startMin: Int, travelMinutes: Int): String {
        var tMin = startMin + travelMinutes
        var tHour = startHour + (tMin / 60)
        tMin %= 60
        tHour %= 24
        return formatTime(tHour, tMin)
    }
}
