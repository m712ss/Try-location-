package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class MoshiGeminiRequest(
    @Json(name = "contents") val contents: List<MoshiContent>,
    @Json(name = "generationConfig") val generationConfig: MoshiGenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: MoshiContent? = null,
    @Json(name = "tools") val tools: List<MoshiTool>? = null
)

@JsonClass(generateAdapter = true)
data class MoshiContent(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<MoshiPart>
)

@JsonClass(generateAdapter = true)
data class MoshiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class MoshiGenerationConfig(
    @Json(name = "temperature") val temperature: Double? = null,
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "thinkingConfig") val thinkingConfig: MoshiThinkingConfig? = null
)

@JsonClass(generateAdapter = true)
data class MoshiThinkingConfig(
    @Json(name = "thinkingLevel") val thinkingLevel: String
)

@JsonClass(generateAdapter = true)
data class MoshiTool(
    @Json(name = "googleSearch") val googleSearch: Map<String, String>? = null,
    @Json(name = "googleMaps") val googleMaps: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class MoshiGeminiResponse(
    @Json(name = "candidates") val candidates: List<MoshiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class MoshiCandidate(
    @Json(name = "content") val content: MoshiContent? = null,
    @Json(name = "groundingMetadata") val groundingMetadata: MoshiGroundingMetadata? = null
)

@JsonClass(generateAdapter = true)
data class MoshiGroundingMetadata(
    @Json(name = "groundingChunks") val groundingChunks: List<MoshiGroundingChunk>? = null,
    @Json(name = "webSearchQueries") val webSearchQueries: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class MoshiGroundingChunk(
    @Json(name = "web") val web: MoshiWebGrounding? = null
)

@JsonClass(generateAdapter = true)
data class MoshiWebGrounding(
    @Json(name = "uri") val uri: String? = null,
    @Json(name = "title") val title: String? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: MoshiGeminiRequest
    ): MoshiGeminiResponse
}

object RetrofitGeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}
