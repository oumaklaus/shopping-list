package com.example.shoppinglist.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoppinglist.data.Item
import com.example.shoppinglist.data.ItemRepository
import com.example.shoppinglist.data.RemoteItemRepository
import kotlinx.coroutines.launch

class ShoppingListViewModel(
    private val repository: ItemRepository
) : ViewModel() {

    constructor() : this(RemoteItemRepository())

    private val _uiState = MutableLiveData(UiState(isLoading = true))
    val uiState: LiveData<UiState> = _uiState

    private val _message = MutableLiveData<UiMessage?>()
    val message: LiveData<UiMessage?> = _message

    init {
        loadItems()
    }

    fun loadItems() {
        val current = _uiState.value ?: UiState()
        _uiState.value = current.copy(isLoading = true, loadError = false)

        viewModelScope.launch {
            repository.getItems()
                .onSuccess { items ->
                    _uiState.value = UiState(
                        items = items,
                        isLoading = false,
                        loadError = false
                    )
                }
                .onFailure {
                    val latest = _uiState.value ?: UiState()
                    _uiState.value = latest.copy(
                        isLoading = false,
                        loadError = latest.items.isEmpty()
                    )
                    if (latest.items.isNotEmpty()) {
                        _message.value = UiMessage.REFRESH_FAILED
                    }
                }
        }
    }

    fun addItem(name: String, quantity: Int) {
        viewModelScope.launch {
            repository.addItem(name, quantity)
                .onSuccess { created ->
                    val current = _uiState.value ?: UiState()
                    _uiState.value = current.copy(
                        items = current.items + created,
                        loadError = false
                    )
                }
                .onFailure { _message.value = UiMessage.SAVE_FAILED }
        }
    }

    fun updateItem(item: Item) {
        viewModelScope.launch {
            repository.updateItem(item)
                .onSuccess { updated ->
                    val current = _uiState.value ?: UiState()
                    _uiState.value = current.copy(
                        items = current.items.map {
                            if (it.id == updated.id) updated else it
                        }
                    )
                }
                .onFailure { _message.value = UiMessage.SAVE_FAILED }
        }
    }

    fun setPurchased(item: Item, purchased: Boolean) {
        val previous = _uiState.value ?: return
        val optimisticItems = previous.items.map {
            if (it.id == item.id) it.copy(purchased = purchased) else it
        }
        _uiState.value = previous.copy(items = optimisticItems)

        viewModelScope.launch {
            repository.updateItem(item.copy(purchased = purchased))
                .onSuccess { updated ->
                    val current = _uiState.value ?: UiState()
                    _uiState.value = current.copy(
                        items = current.items.map {
                            if (it.id == updated.id) updated else it
                        }
                    )
                }
                .onFailure {
                    _uiState.value = previous
                    _message.value = UiMessage.SAVE_FAILED
                }
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            repository.deleteItem(item.id)
                .onSuccess {
                    val current = _uiState.value ?: UiState()
                    _uiState.value = current.copy(
                        items = current.items.filterNot { it.id == item.id }
                    )
                }
                .onFailure { _message.value = UiMessage.SAVE_FAILED }
        }
    }

    fun messageShown() {
        _message.value = null
    }
}
