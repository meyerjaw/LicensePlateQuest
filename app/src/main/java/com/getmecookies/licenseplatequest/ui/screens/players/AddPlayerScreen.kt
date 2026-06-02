package com.getmecookies.licenseplatequest.ui.screens.players

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.ui.AppViewModelProvider
import java.util.UUID

/**
 * Full-screen Add Player (replaces the former dialog so the flow can grow in future
 * phases). MVP: a single name field, a top-bar back button, and a Save action.
 *
 * [onDone] reports the result: the new player's id on a successful save, or null if the user
 * backed out. This lets callers (e.g. the New Trip flow) auto-select the player that was just
 * created. Either way the screen is finished and should be popped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayerScreen(
    onDone: (UUID?) -> Unit,
    viewModel: AddPlayerViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedPlayerId by viewModel.savedPlayerId.collectAsStateWithLifecycle()

    val errorText = when (uiState.error) {
        PlayerNameError.BLANK -> stringResource(R.string.player_name_blank)
        PlayerNameError.DUPLICATE ->
            stringResource(R.string.player_name_duplicate, uiState.name.trim())
        null -> null
    }

    // Navigate back (reporting the new id) once the save completes.
    LaunchedEffect(savedPlayerId) {
        savedPlayerId?.let { onDone(it) }
    }

    // Focus the name field as soon as the screen opens.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_player_title)) },
                navigationIcon = {
                    IconButton(onClick = { onDone(null) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.add_player_name_label)) },
                singleLine = true,
                isError = errorText != null,
                supportingText = errorText?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.onSave() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )

            Button(
                onClick = viewModel::onSave,
                enabled = !uiState.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}
