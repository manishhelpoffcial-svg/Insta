package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.InstagramCircularSpinner
import com.example.ui.theme.GrandHotelFontFamily
import com.example.ui.theme.IgBlue
import com.example.ui.theme.IgDarkElevated
import com.example.ui.theme.IgDarkHairline
import com.example.ui.theme.IgDarkSecondaryText
import com.example.ui.theme.IgNotifRed
import com.example.ui.theme.IgTypography
import com.example.ui.theme.InstagramTheme

@Composable
fun LoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onLoginClick: (identifier: String, pass: String) -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigateForgotPassword: () -> Unit,
    onContinueGuestMode: (() -> Unit)? = null,
) {
    val colors = InstagramTheme.colors
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val canSubmit = identifier.isNotBlank() && password.length >= 4 && !isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height(30.dp))

        // Main Lock Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // App Logo
            Image(
                painter = painterResource(R.drawable.ic_instagram_brand_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp)),
            )

            Spacer(Modifier.height(20.dp))

            // Lock Icon Badge & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Locked",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Application Locked",
                    style = IgTypography.screenHeading,
                    color = colors.textPrimary,
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Authorized Database Access Only",
                style = IgTypography.secondaryMeta.copy(fontSize = 13.sp),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Log in with your database account to unlock and access.",
                style = IgTypography.secondaryMeta,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(28.dp))

            // Error banner if any
            if (!errorMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF331414))
                        .border(1.dp, Color(0xFFED4956).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = "Error",
                            tint = Color(0xFFED4956),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFF6B6B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // Identifier Input (Username / Email)
            OutlinedTextField(
                value = identifier,
                onValueChange = { identifier = it },
                label = { Text("Database Username or Email", style = IgTypography.secondaryMeta.copy(fontSize = 13.sp)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IgBlue,
                    unfocusedBorderColor = colors.hairline,
                    focusedContainerColor = colors.surfaceVariant,
                    unfocusedContainerColor = colors.surfaceVariant,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedLabelColor = IgBlue,
                    unfocusedLabelColor = colors.textSecondary,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Database Password", style = IgTypography.secondaryMeta.copy(fontSize = 13.sp)) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canSubmit) onLoginClick(identifier, password) }),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IgBlue,
                    unfocusedBorderColor = colors.hairline,
                    focusedContainerColor = colors.surfaceVariant,
                    unfocusedContainerColor = colors.surfaceVariant,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedLabelColor = IgBlue,
                    unfocusedLabelColor = colors.textSecondary,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            // Unlock Application Button
            Button(
                onClick = { onLoginClick(identifier, password) },
                enabled = canSubmit,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0095F6),
                    disabledContainerColor = Color(0xFF0095F6).copy(alpha = 0.4f),
                    contentColor = Color.White,
                    disabledContentColor = Color.White.copy(alpha = 0.6f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (isLoading) {
                    InstagramCircularSpinner(
                        size = 20.dp,
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Unlock Application", style = IgTypography.button)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Bottom Admin Notice
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalDivider(thickness = 0.5.dp, color = colors.hairline)
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Accounts are managed via Discord Bot / Admin Panel",
                color = colors.textSecondary,
                style = IgTypography.chartLabel,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun RegisterScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onRegisterClick: (email: String, username: String, pass: String) -> Unit,
    onNavigateLogin: () -> Unit,
) {
    val colors = InstagramTheme.colors
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val cleanUser = username.trim()
    val canSubmit = cleanUser.length >= 3 &&
        password.length >= 6 &&
        password == confirmPassword &&
        !isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height(30.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Instagram",
                fontFamily = GrandHotelFontFamily,
                fontSize = 48.sp,
                color = colors.textPrimary,
            )

            Text(
                text = "Sign up to access exclusive creator insights and feeds.",
                fontSize = 14.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )

            if (!errorMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(IgNotifRed.copy(alpha = 0.15f))
                        .border(1.dp, IgNotifRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                ) {
                    Text(
                        text = errorMessage,
                        color = IgNotifRed,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Desired Username", fontSize = 13.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IgDarkHairline,
                    unfocusedBorderColor = IgDarkHairline.copy(alpha = 0.6f),
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (min 6 characters)", fontSize = 13.sp) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IgDarkHairline,
                    unfocusedBorderColor = IgDarkHairline.copy(alpha = 0.6f),
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password", fontSize = 13.sp) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (canSubmit) {
                        val internalEmail = "${cleanUser.lowercase()}@insta.app"
                        onRegisterClick(internalEmail, cleanUser, password)
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IgDarkHairline,
                    unfocusedBorderColor = IgDarkHairline.copy(alpha = 0.6f),
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    val internalEmail = "${cleanUser.lowercase()}@insta.app"
                    onRegisterClick(internalEmail, cleanUser, password)
                },
                enabled = canSubmit,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IgBlue,
                    disabledContainerColor = IgBlue.copy(alpha = 0.5f),
                    contentColor = Color.White,
                    disabledContentColor = Color.White.copy(alpha = 0.7f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text("Sign Up", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalDivider(thickness = 0.5.dp, color = colors.hairline)
            Row(
                modifier = Modifier.padding(vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Have an account? ",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                )
                Text(
                    text = "Log in.",
                    color = IgBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onNavigateLogin() },
                )
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    isLoading: Boolean,
    message: String?,
    onSendReset: (email: String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val colors = InstagramTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(64.dp),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Need Account Help?",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = colors.textPrimary,
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Accounts and passwords are administered directly through Discord without requiring email.\n\nTo reset your password or view your credentials, use the Discord Bot or contact an administrator.",
            fontSize = 13.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { onNavigateBack() },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IgBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
        ) {
            Text("Back to Login", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

/**
 * Screen displayed when the account is suspended, banned, or subscription has expired.
 */
@Composable
fun AccessGatedScreen(
    accountStatus: String,
    subscriptionStatus: String?,
    expiryDate: String?,
    message: String,
    onLogoutClick: () -> Unit,
    onRefreshClick: () -> Unit,
) {
    val colors = InstagramTheme.colors
    val context = LocalContext.current

    val isExpired = subscriptionStatus == "expired" || (expiryDate != null && message.contains("expired", ignoreCase = true))
    val isSuspendedOrBanned = accountStatus == "suspended" || accountStatus == "banned"

    val title = when {
        accountStatus == "banned" -> "Account Banned"
        accountStatus == "suspended" -> "Account Suspended"
        isExpired -> "Subscription Expired"
        else -> "Subscription Required"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (isSuspendedOrBanned) IgNotifRed.copy(alpha = 0.2f) else colors.pillBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = if (isSuspendedOrBanned) IgNotifRed else colors.textPrimary,
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = message,
            fontSize = 14.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )

        if (expiryDate != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Expiry: $expiryDate",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )
        }

        Spacer(Modifier.height(32.dp))

        // Discord renew button
        Button(
            onClick = {
                // Open Discord instructions or server link
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg"))
                context.startActivity(intent)
            },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IgBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
        ) {
            Text("Renew via Discord (/purchase)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onRefreshClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.pillBackground),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
        ) {
            Text("Check Status Again", color = colors.textPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Log Out",
            color = IgNotifRed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(8.dp)
                .clickable { onLogoutClick() },
        )
    }
}
