package org.nekomanga.presentation.screens.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.nekomanga.R
import org.nekomanga.presentation.theme.Size

internal class PermissionStep : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val context = LocalContext.current

        var notificationGranted by remember {
            mutableStateOf(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
            )
        }

        Column(modifier = Modifier.padding(vertical = Size.medium)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionRequester =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = { bool -> notificationGranted = bool },
                    )
                PermissionItem(
                    title = stringResource(R.string.onboarding_permission_notifications),
                    subtitle =
                        stringResource(R.string.onboarding_permission_notifications_description),
                    granted = notificationGranted,
                    onButtonClick = {
                        permissionRequester.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
            }
        }
    }

    @Composable
    private fun PermissionItem(
        title: String,
        subtitle: String,
        granted: Boolean,
        modifier: Modifier = Modifier,
        onButtonClick: () -> Unit,
    ) {
        ListItem(
            modifier = modifier,
            supportingContent = { Text(text = subtitle) },
            trailingContent = {
                OutlinedButton(
                    enabled = !granted,
                    onClick = onButtonClick,
                    shapes = ButtonDefaults.shapes(),
                ) {
                    if (granted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text(stringResource(R.string.onboarding_permission_action_grant))
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        ) {
            Text(text = title)
        }
    }
}
