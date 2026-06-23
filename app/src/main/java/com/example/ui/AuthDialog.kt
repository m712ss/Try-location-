package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.MapRouteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProfileHeaderButton(viewModel: MapRouteViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userDisplayName by viewModel.userDisplayName.collectAsState()

    Row(
        modifier = Modifier
            .background(
                color = if (isLoggedIn) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { viewModel.setShowAuthScreen(true) }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isLoggedIn) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (userDisplayName ?: "م").take(1).uppercase(),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Text(
                text = userDisplayName ?: "المستخدم",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "تسجيل الدخول",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "دخول",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
fun AuthDialog(viewModel: MapRouteViewModel) {
    val showAuthScreen by viewModel.showAuthScreen.collectAsState()

    if (showAuthScreen) {
        Dialog(onDismissRequest = { viewModel.setShowAuthScreen(false) }) {
            AuthDialogContent(
                viewModel = viewModel,
                onDismiss = { viewModel.setShowAuthScreen(false) }
            )
        }
    }
}

@Composable
fun AuthDialogContent(
    viewModel: MapRouteViewModel,
    onDismiss: () -> Unit
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val userDisplayName by viewModel.userDisplayName.collectAsState()
    val authMethod by viewModel.authMethod.collectAsState()
    val pins by viewModel.allPins.collectAsState()
    val routes by viewModel.allRoutes.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isSignUpMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var loginTypeLoading by remember { mutableStateOf<String?>(null) } // "GOOGLE" or "EMAIL" or "SIGNUP"

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .shadow(16.dp, shape = RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (isLoggedIn) "حسابك الشخصي" else if (isSignUpMode) "إنشاء حساب جديد" else "تسجيل الدخول",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.size(40.dp)) // spacer to balance close button
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoggedIn) {
                // LOGGED IN STATE DISPLAY
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Icon Large
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (userDisplayName ?: "م").take(1).uppercase(),
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = userDisplayName ?: "مستخدم المسار الذكي",
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = userEmail ?: "لا يوجد بريد إلكتروني",
                            style = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        // Auth badge
                        val isGoogle = authMethod == "GOOGLE"
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = if (isGoogle) "مُتصل بحساب Google 🟢" else "مُتصل بالبريد الإلكتروني 📧",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                labelColor = if (isGoogle) MaterialTheme.colorScheme.primary else Color(0xFF0F766E)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isGoogle) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color(0xFF0F766E).copy(alpha = 0.3f)
                            ),
                            shape = CircleShape
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Sync Stats
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${pins.size}",
                                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black)
                                )
                                Text(
                                    text = "دبابيس مخزنة",
                                    style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Route,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "${routes.size}",
                                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black)
                                )
                                Text(
                                    text = "مسارات محفوظة",
                                    style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }

                    Text(
                        text = "تم تفعيل التزامن السحابي التلقائي مع حسابك بنجاح. دبابيسك ومخططاتك آمنة ومتاحة على أي جهاز آخر!",
                        style = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center),
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.logoutUser()
                            Toast.makeText(context, "تم تسجيل الخروج بنجاح 👋", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "تسجيل الخروج", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // AUTH LOGIN / REGISTER FORMS
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "احفظ مواقعك ومساراتك المفضلة وسجل دخولك الآن للتحكم وتنسيق رحلاتك بذكاء خارق وعبر الأجهزة المختلفة 🚀",
                        style = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Tab toggle
                    TabRow(
                        selectedTabIndex = if (isSignUpMode) 1 else 0,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        indicator = { TabRowDefaults.Indicator(color = MaterialTheme.colorScheme.primary) }
                    ) {
                        Tab(
                            selected = !isSignUpMode,
                            onClick = { isSignUpMode = false },
                            text = { Text("تسجيل الدخول", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        )
                        Tab(
                            selected = isSignUpMode,
                            onClick = { isSignUpMode = true },
                            text = { Text("إنشاء حساب", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Simulated Google Sign In Button (Google Colors & Vector layout)
                    if (loginTypeLoading == "GOOGLE") {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("جاري الاتصال بخوادم Google...", fontSize = 13.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                loginTypeLoading = "GOOGLE"
                                coroutineScope.launch {
                                    delay(1200) // Beautiful realistic latency
                                    viewModel.loginWithGoogle("saudi.explorer@gmail.com", "مستكشف السعودية 🇸🇦")
                                    loginTypeLoading = null
                                    Toast.makeText(context, "أهلاً بك! تم الدخول بحساب Google بنجاح 🌐", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1F2937)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "G  ",
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color(0xFF4285F4)
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تسجيل الدخول بـ Google",
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = "أو عبر البريد الإلكتروني",
                            style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    // Form Inputs
                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = {
                                nameInput = it
                                nameError = null
                            },
                            label = { Text("الاسم الكامل") },
                            isError = nameError != null,
                            supportingText = { nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            emailError = null
                        },
                        label = { Text("البريد الإلكتروني") },
                        isError = emailError != null,
                        supportingText = { emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            passwordError = null
                        },
                        label = { Text("كلمة المرور") },
                        isError = passwordError != null,
                        supportingText = { passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Action buttons
                    if (loginTypeLoading == "EMAIL" || loginTypeLoading == "SIGNUP") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(strokeWidth = 3.dp)
                        }
                    } else {
                        Button(
                            onClick = {
                                // Validation
                                var hasError = false
                                if (emailInput.isEmpty() || !emailInput.contains("@")) {
                                    emailError = "الرجاء إدخال بريد إلكتروني صالح دبابيسك"
                                    hasError = true
                                }
                                if (passwordInput.length < 6) {
                                    passwordError = "كلمة المرور يجب أن تكون 6 أحرف على الأقل"
                                    hasError = true
                                }
                                if (isSignUpMode && nameInput.isEmpty()) {
                                    nameError = "الرجاء إدخال الاسم الكامل"
                                    hasError = true
                                }

                                if (!hasError) {
                                    if (isSignUpMode) {
                                        loginTypeLoading = "SIGNUP"
                                        coroutineScope.launch {
                                            delay(1500)
                                            viewModel.loginWithEmail(emailInput, nameInput)
                                            loginTypeLoading = null
                                            Toast.makeText(context, "مرحباً بك! تم إنشاء حسابك وتفعيله بنجاح 🎉", Toast.LENGTH_LONG).show()
                                            onDismiss()
                                        }
                                    } else {
                                        loginTypeLoading = "EMAIL"
                                        coroutineScope.launch {
                                            delay(1200)
                                            val simulatedName = emailInput.substringBefore("@").replaceFirstChar { it.uppercase() }
                                            viewModel.loginWithEmail(emailInput, simulatedName)
                                            loginTypeLoading = null
                                            Toast.makeText(context, "أهلاً ومرحباً بك مجدداً! تم تسجيل دخولك الموثق بنجاح 🔒", Toast.LENGTH_LONG).show()
                                            onDismiss()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isSignUpMode) "تسجيل حساب جديد" else "تسجيل الدخول",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Guest navigation hint
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "الاستمرار كضيف مؤقتاً 🧭",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }
            }
        }
    }
}
