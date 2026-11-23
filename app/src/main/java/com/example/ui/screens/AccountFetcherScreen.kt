package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.rememberProgressiveLoadingLabel
import com.example.ui.theme.IgBoostBlue
import com.example.ui.theme.IgNotifRed
import com.example.ui.theme.InstagramTheme
import com.example.ui.viewmodel.FetchAccountUiState

@Composable
fun AccountFetcherScreen(
    fetchState: FetchAccountUiState,
    onFetchAccount: (String) -> Unit,
    onInputChanged: () -> Unit = {},
) {
    val colors = InstagramTheme.colors
    val keyboardController = LocalSoftwareKeyboardController.current
    var usernameInput by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val isLoading = fetchState is FetchAccountUiState.Loading
    val loadingLabel = rememberProgressiveLoadingLabel(isLoading)

    fun submitFetch() {
        val cleaned = usernameInput.trim().removePrefix("@").trim()
        if (cleaned.isBlank()) {
            localError = "Please enter an Instagram username."
            return
        }
        localError = null
        keyboardController?.hide()
        onFetchAccount(cleaned)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            OutlinedTextField(
                value = usernameInput,
                onValueChange = {
                    usernameInput = it
                    if (localError != null) localError = null
                    onInputChanged()
                },
                placeholder = {
                    Text(
                        text = "Enter Instagram username...",
                        color = colors.textSecondary,
                        fontSize = 15.sp,
                    )
                },
                singleLine = true,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { submitFetch() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.pillBackground,
                    unfocusedContainerColor = colors.pillBackground,
                    disabledContainerColor = colors.pillBackground,
                    focusedBorderColor = IgBoostBlue,
                    unfocusedBorderColor = colors.hairline,
                    disabledBorderColor = colors.hairline,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = IgBoostBlue,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tab3_username_input"),
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { submitFetch() },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IgBoostBlue,
                    contentColor = Color.White,
                    disabledContainerColor = IgBoostBlue.copy(alpha = 0.65f),
                    disabledContentColor = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("tab3_fetch_button"),
            ) {
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(18.dp)
                                .testTag("tab3_loading_indicator"),
                        )
                        Text(
                            text = loadingLabel,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("tab3_loading_label"),
                        )
                    }
                } else {
                    Text(
                        text = "Enter",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            val errorText = localError ?: (fetchState as? FetchAccountUiState.Error)?.message
            if (!errorText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorText,
                    color = IgNotifRed,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tab3_error_text"),
                )
            }
        }
    }
}
