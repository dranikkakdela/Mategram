package com.xxcactussell.presentation.profile.screen

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xxcactussell.domain.BasicGroupFullInfo
import com.xxcactussell.domain.SupergroupFullInfo
import com.xxcactussell.domain.UserFullInfo
import com.xxcactussell.mategram.presentation.R
import com.xxcactussell.presentation.profile.model.ProfileModel
import com.xxcactussell.presentation.profile.model.ProfileUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileContent(state: State<ProfileUiState>, onBackHandle: () -> Unit) {
    val profile = state.value.profileModel

    // Достаём данные в зависимости от типа профиля
    val name = when (profile) {
        is ProfileModel.UserProfileModel -> {
            val u = profile.userFullInfo.user
            "${u.firstName} ${u.lastName}".trim()
        }
        is ProfileModel.BasicGroupModel -> profile.basicGroupFullInfo.description
        is ProfileModel.SupergroupModel -> profile.supergroupFullInfo.description
        null -> ""
    }

    val bio = when (profile) {
        is ProfileModel.UserProfileModel -> profile.userFullInfo.bio?.text ?: ""
        is ProfileModel.BasicGroupModel -> profile.basicGroupFullInfo.description
        is ProfileModel.SupergroupModel -> profile.supergroupFullInfo.description
        null -> ""
    }

    val username = when (profile) {
        is ProfileModel.UserProfileModel -> profile.userFullInfo.user.usernames?.activeUsernames?.firstOrNull() ?: ""
        else -> ""
    }

    val phone = when (profile) {
        is ProfileModel.UserProfileModel -> profile.userFullInfo.user.phoneNumber
        else -> ""
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = name.ifBlank { "Profile" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackHandle) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert_24px),
                            contentDescription = "More"
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Аватар
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Заглушка аватара — большой круг с инициалами
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .then(
                                Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.firstOrNull()?.toString() ?: "?",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = name.ifBlank { "Unknown" },
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (username.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "@$username",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Кнопки действий
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = { },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_notification),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Message")
                    }
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.call_24px),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Call")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Информация
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                if (phone.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text(phone) },
                        supportingContent = { Text("Phone", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.call_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                if (bio.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text(bio) },
                        supportingContent = { Text("Bio", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.bookmarks_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                if (username.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text("@$username") },
                        supportingContent = { Text("Username", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.contacts_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}
