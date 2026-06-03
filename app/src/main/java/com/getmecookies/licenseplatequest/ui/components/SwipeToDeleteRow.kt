package com.getmecookies.licenseplatequest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.getmecookies.licenseplatequest.R
import kotlinx.coroutines.delay

/** How long the swiped row lingers as an undo bar before the delete actually commits. */
private const val UNDO_WINDOW_MILLIS = 3000L

/**
 * Wraps [content] in a swipe-to-delete row with an in-place undo window.
 *
 * Swiping the row fully (either direction) does **not** remove it. The swipe never actually
 * dismisses the box — the row's content stays put — and instead an opaque red
 * "[deletedMessage] · Undo" bar is laid over the row for [UNDO_WINDOW_MILLIS]. Tapping Undo
 * removes that overlay, revealing the unchanged row beneath (nothing is deleted). If the window
 * elapses, [onDelete] fires to commit the real deletion; the caller's data source then drops the
 * item and the list animates it out (pair with `Modifier.animateItem()`).
 *
 * Shared by the Trip List (playtest note #15) and Players roster (#16) so both behave the same.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteRow(
    onDelete: () -> Unit,
    deletedMessage: String,
    deleteContentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // True once the row has been swiped and is showing its undo bar, awaiting commit.
    var pending by remember { mutableStateOf(false) }

    // Hold the row under the undo bar for the window, then commit. Tapping Undo sets pending=false,
    // which cancels this effect before it fires.
    LaunchedEffect(pending) {
        if (pending) {
            delay(UNDO_WINDOW_MILLIS)
            onDelete()
        }
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                pending = true
            }
            // Never confirm the dismiss: the box snaps back so the content stays in place, and we
            // show our own undo overlay instead. This keeps Undo trivial (just drop the overlay).
            false
        },
    )

    Box(modifier = modifier) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                DeleteBackdrop(
                    contentDescription = deleteContentDescription,
                    swipingToStart = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart,
                )
            },
        ) {
            content()
        }

        // Opaque overlay sized to the row; covers the content during the undo window.
        if (pending) {
            UndoBar(
                message = deletedMessage,
                onUndo = { pending = false },
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/** The red backdrop with a trash icon shown while the row is being swiped. */
@Composable
private fun DeleteBackdrop(contentDescription: String, swipingToStart: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 20.dp),
        contentAlignment = if (swipingToStart) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

/** The in-place undo affordance laid over the row during the undo window. */
@Composable
private fun UndoBar(message: String, onUndo: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            // Swallow taps so they don't reach the row beneath while the undo bar is shown.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(start = 20.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onUndo) {
            Text(stringResource(R.string.action_undo))
        }
    }
}
