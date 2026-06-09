package com.xxcactussell.presentation.auth.screen.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xxcactussell.mategram.presentation.R
import com.xxcactussell.presentation.auth.model.AuthEvent
import com.xxcactussell.presentation.auth.model.AuthUiState
import com.xxcactussell.presentation.localization.localizedString
import com.xxcactussell.presentation.tools.NumericKeyboard

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthInputCodeView(state: AuthUiState, onEvent: (AuthEvent) -> Unit) {
    val scrollState = rememberScrollState()
    var code by remember { mutableStateOf("") }
    val isReady = code.length >= 5

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            NumericKeyboard(
                onNumberClick = { digit ->
                    if (code.length < 16) {
                        code += digit.toString()
                    }
                },
                onBackspaceClick = {
                    if (code.isNotEmpty()) {
                        code = code.dropLast(1)
                    }
                },
                onSendClick = {
                    if (isReady) {
                        onEvent(AuthEvent.SubmitCode(code))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(paddingValues)
                    .padding(top = 52.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painterResource(R.drawable.lock_24px),
                    "Code icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    localizedString("login_Code"),
                    style = MaterialTheme.typography.displayMediumEmphasized
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    localizedString("SentAppCode"),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(32.dp))
                CodeDisplayField(
                    code = code,
                    isReady = isReady
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { onEvent(AuthEvent.ResendCode) }
                ) {
                    Text(
                        localizedString("DidNotGetTheCode"),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .clickable(enabled = false, onClick = {}, indication = null, interactionSource = null)
        ) {
            LoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CodeDisplayField(
    code: String,
    isReady: Boolean = false,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isReady)
            MaterialTheme.colorScheme.tertiaryContainer
        else
            MaterialTheme.colorScheme.primaryContainer,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "codeFieldColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isReady)
            MaterialTheme.colorScheme.onTertiaryContainer
        else
            MaterialTheme.colorScheme.onPrimaryContainer,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "codeTextColor"
    )

    TextField(
        value = code,
        onValueChange = {},
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(40.dp))
            .height(80.dp),
        readOnly = true,
        textStyle = MaterialTheme.typography.displaySmallEmphasized.copy(
            textAlign = TextAlign.Center
        ),
        colors = TextFieldDefaults.colors(
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent
        ),
        singleLine = true
    )
}
