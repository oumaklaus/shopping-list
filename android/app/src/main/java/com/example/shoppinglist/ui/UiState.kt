package com.example.shoppinglist.ui

import com.example.shoppinglist.data.Item

data class UiState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val loadError: Boolean = false
)

enum class UiMessage {
    REFRESH_FAILED,
    SAVE_FAILED
}
