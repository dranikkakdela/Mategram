package com.xxcactussell.presentation.auth.screen.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xxcactussell.mategram.presentation.R
import com.xxcactussell.presentation.auth.model.AuthEvent
import com.xxcactussell.presentation.auth.model.AuthUiState
import com.xxcactussell.presentation.localization.localizedString
import com.xxcactussell.presentation.tools.SendButton

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthInputPasswordView(state: AuthUiState, onEvent: (AuthEvent) -> Unit) {
    val scrollState = rememberScrollState()
    var password by remember { mutableStateOf("") }
    val isReady = password.isNotEmpty()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painterResource(R.drawable.lock_24px),
                "Password icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                localizedString("YourPassword"),
                style = MaterialTheme.typography.displayMediumEmphasized
            )
            Spacer(Modifier.height(8.dp))
            Text(
                localizedString("LoginPasswordText"),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PasswordDisplayField(
                    password = password,
                    isReady = isReady,
                    onPasswordChange = { password = it },
                    onSend = { onEvent(AuthEvent.SubmitPassword(password)) },
                    modifier = Modifier.weight(1f)
                )
                SendButton(
                    onClick = { onEvent(AuthEvent.SubmitPassword(password)) },
                    content = {
                        Icon(
                            painterResource(R.drawable.start_24px),
                            modifier = Modifier.size(28.dp),
                            contentDescription = localizedString("Send")
                        )
                    },
                    modifier = Modifier.height(80.dp),
                    isLandscape = false,
                    interactionSource = null,
                    isPressed = false,
                    isOtherPressed = false,
                )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                // Забыл пароль — отправляем событие
                onClick = { onEvent(AuthEvent.ForgotPassword) }
            ) {
                Text(
                    localizedString("ForgotPassword"),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .clickable(
                    enabled = false,
                    onClick = {},
                    indication = null,
                    interactionSource = null
                )
        ) {
            LoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PasswordDisplayField(
    password: String,
    isReady: Boolean = false,
    onPasswordChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordVisibility by remember { mutableStateOf(false) }

    // Плавная анимация цвета когда пароль введён
    val containerColor by animateColorAsState(
        targetValue = if (isReady)
            MaterialTheme.colorScheme.tertiaryContainer
        else
            MaterialTheme.colorScheme.primaryContainer,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "passwordFieldColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isReady)
            MaterialTheme.colorScheme.onTertiaryContainer
        else
            MaterialTheme.colorScheme.onPrimaryContainer,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "passwordTextColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(containerColor)
    ) {
        TextField(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 24.dp),
            value = password,
            onValueChange = onPasswordChange,
            textStyle = MaterialTheme.typography.displaySmallEmphasized,
            colors = TextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent
            ),
            singleLine = true,
            visualTransformation = if (passwordVisibility)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSend() }
            ),
            trailingIcon = {
                val image = if (passwordVisibility)
                    R.drawable.visibility_24px
                else
                    R.drawable.visibility_off_24px
                IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                    Icon(
                        painter = painterResource(image),
                        contentDescription = "Password visibility",
                        tint = textColor
                    )
                }
            }
        )
    }
}
