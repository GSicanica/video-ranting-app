package com.youtube.rating.android.ui.screens

import java.io.Serializable

internal object FavoritesContract {
    internal enum class DeleteKind {
        SINGLE,
        GROUP,
        CLEAR_ALL,
    }

    internal data class DeleteConfirmationState(
        val open: Boolean = false,
        val kind: DeleteKind = DeleteKind.SINGLE,
        val key: String = "",
        val step: Int = 1,
        val total: Int = 1,
        val needsWord: Boolean = false,
        val word: String = "",
    ) : Serializable {
        val isFinalStep: Boolean
            get() = step == total
    }

    internal data class State(
        val query: String = "",
        val sortBy: SortMode = SortMode.NEWEST,
        val groupMode: GroupMode = GroupMode.GROUPED,
        val collapsedGroups: List<String> = emptyList(),
        val confirmation: DeleteConfirmationState = DeleteConfirmationState(),
    ) : Serializable {
        val collapsedSet: Set<String>
            get() = collapsedGroups.toHashSet()
    }

    internal sealed interface Intent {
        data class QueryChanged(val query: String) : Intent
        data class SortChanged(val sortMode: SortMode) : Intent
        data class GroupModeChanged(val groupMode: GroupMode) : Intent
        data class ToggleGroupCollapsed(val groupKey: String) : Intent
        data class OpenDeleteConfirmation(
            val kind: DeleteKind,
            val key: String,
            val protected: Boolean,
        ) : Intent
        data object CloseDeleteConfirmation : Intent
        data object AdvanceDeleteConfirmation : Intent
        data object BackDeleteConfirmation : Intent
        data class DeleteConfirmationWordChanged(val word: String) : Intent
    }

    internal sealed interface Effect {
        data class ShowUndoSnackbar(val message: String) : Effect
    }

    internal fun reduce(state: State, intent: Intent): State {
        return when (intent) {
            is Intent.QueryChanged -> state.copy(query = intent.query)
            is Intent.SortChanged -> state.copy(sortBy = intent.sortMode)
            is Intent.GroupModeChanged -> state.copy(groupMode = intent.groupMode)
            is Intent.ToggleGroupCollapsed -> {
                val collapsedGroups = if (state.collapsedSet.contains(intent.groupKey)) {
                    state.collapsedGroups.filterNot { it == intent.groupKey }
                } else {
                    state.collapsedGroups + intent.groupKey
                }
                state.copy(collapsedGroups = collapsedGroups)
            }
            is Intent.OpenDeleteConfirmation -> state.copy(
                confirmation = DeleteConfirmationState(
                    open = true,
                    kind = intent.kind,
                    key = intent.key,
                    step = 1,
                    total = if (intent.protected) 3 else 1,
                    needsWord = intent.protected,
                    word = "",
                )
            )
            Intent.CloseDeleteConfirmation -> state.copy(confirmation = DeleteConfirmationState())
            Intent.AdvanceDeleteConfirmation -> state.copy(
                confirmation = state.confirmation.copy(
                    step = (state.confirmation.step + 1).coerceAtMost(state.confirmation.total),
                    word = "",
                )
            )
            Intent.BackDeleteConfirmation -> state.copy(
                confirmation = state.confirmation.copy(
                    step = (state.confirmation.step - 1).coerceAtLeast(1),
                    word = "",
                )
            )
            is Intent.DeleteConfirmationWordChanged -> state.copy(
                confirmation = state.confirmation.copy(word = intent.word)
            )
        }
    }
}
