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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.xxcactussell.presentation.tools.PhoneNumberFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthInputPhoneView(state: AuthUiState, onEvent: (AuthEvent) -> Unit) {
    val scrollState = rememberScrollState()
    var phoneNumber by remember { mutableStateOf("+") }
    val isValid = PhoneNumberFormatter.isValid(phoneNumber)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            NumericKeyboard(
                onNumberClick = { digit ->
                    if (phoneNumber.length < 16) {
                        phoneNumber += digit.toString()
                    }
                },
                onBackspaceClick = {
                    if (phoneNumber.length > 1) {
                        phoneNumber = phoneNumber.dropLast(1)
                    }
                },
                onSendClick = {
                    if (isValid) {
                        onEvent(AuthEvent.SubmitPhone(phoneNumber))
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
                    painterResource(R.drawable.sim_card_24px),
                    "SIM-card icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    localizedString("YourPhone"),
                    style = MaterialTheme.typography.displayMediumEmphasized
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    localizedString("PassportPhoneInfo"),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(32.dp))
                // Цвет поля меняется когда номер валидный
                PhoneDisplayField(
                    phoneNumber = phoneNumber,
                    isValid = isValid
                )
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
fun PhoneDisplayField(
    phoneNumber: String,
    isValid: Boolean = false,
    modifier: Modifier = Modifier
) {
    val formattedNumber = PhoneNumberFormatter.format(phoneNumber) ?: ""

    // Плавная анимация цвета — зелёный когда валидный номер
    val containerColor by animateColorAsState(
        targetValue = if (isValid)
            MaterialTheme.colorScheme.tertiaryContainer
        else
            MaterialTheme.colorScheme.primaryContainer,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "phoneFieldColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isValid)
            MaterialTheme.colorScheme.onTertiaryContainer
        else
            MaterialTheme.colorScheme.onPrimaryContainer,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "phoneTextColor"
    )

    TextField(
        value = formattedNumber,
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
