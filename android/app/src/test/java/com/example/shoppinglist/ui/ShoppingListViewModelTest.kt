package com.example.shoppinglist.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.shoppinglist.data.Item
import com.example.shoppinglist.data.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeItemRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeItemRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads items on start`() = runTest(dispatcher) {
        repository.items = mutableListOf(item(id = "1", name = "Milk"))

        val viewModel = ShoppingListViewModel(repository)
        advanceUntilIdle()

        val state = requireNotNull(viewModel.uiState.value)
        assertEquals(listOf("Milk"), state.items.map { it.name })
        assertFalse(state.isLoading)
        assertFalse(state.loadError)
    }

    @Test
    fun `load failure shows error state when list is empty`() = runTest(dispatcher) {
        repository.failGet = true

        val viewModel = ShoppingListViewModel(repository)
        advanceUntilIdle()

        val state = requireNotNull(viewModel.uiState.value)
        assertTrue(state.items.isEmpty())
        assertTrue(state.loadError)
        assertFalse(state.isLoading)
    }

    @Test
    fun `refresh failure keeps existing items and shows a message`() = runTest(dispatcher) {
        repository.items = mutableListOf(item(id = "1", name = "Milk"))
        val viewModel = ShoppingListViewModel(repository)
        advanceUntilIdle()

        repository.failGet = true
        viewModel.loadItems()
        advanceUntilIdle()

        val state = requireNotNull(viewModel.uiState.value)
        assertEquals(listOf("Milk"), state.items.map { it.name })
        assertFalse(state.loadError)
        assertEquals(UiMessage.REFRESH_FAILED, viewModel.message.value)
    }

    @Test
    fun `add item appends the created item`() = runTest(dispatcher) {
        val viewModel = ShoppingListViewModel(repository)
        advanceUntilIdle()

        viewModel.addItem("Bread", 2)
        advanceUntilIdle()

        val state = requireNotNull(viewModel.uiState.value)
        assertEquals(listOf("Bread"), state.items.map { it.name })
        assertEquals(2, state.items.single().quantity)
        assertNull(viewModel.message.value)
    }

    @Test
    fun `toggle updates immediately and keeps server result`() = runTest(dispatcher) {
        repository.items = mutableListOf(item(id = "1", name = "Milk"))
        val viewModel = ShoppingListViewModel(repository)
        advanceUntilIdle()
        val milk = requireNotNull(viewModel.uiState.value).items.single()

        viewModel.setPurchased(milk, true)

        assertTrue(requireNotNull(viewModel.uiState.value).items.single().purchased)

        advanceUntilIdle()

        assertTrue(requireNotNull(viewModel.uiState.value).items.single().purchased)
    }

    @Test
    fun `toggle failure rolls back and shows a message`() = runTest(dispatcher) {
        repository.items = mutableListOf(item(id = "1", name = "Milk"))
        val viewModel = ShoppingListViewModel(repository)
        advanceUntilIdle()
        val milk = requireNotNull(viewModel.uiState.value).items.single()

        repository.failMutations = true
        viewModel.setPurchased(milk, true)
        assertTrue(requireNotNull(viewModel.uiState.value).items.single().purchased)

        advanceUntilIdle()

        assertFalse(requireNotNull(viewModel.uiState.value).items.single().purchased)
        assertEquals(UiMessage.SAVE_FAILED, viewModel.message.value)
    }

    @Test
    fun `delete removes the item`() = runTest(dispatcher) {
        repository.items = mutableListOf(item(id = "1", name = "Milk"))
        val viewModel = ShoppingListViewModel(repository)
        advanceUntilIdle()
        val milk = requireNotNull(viewModel.uiState.value).items.single()

        viewModel.deleteItem(milk)
        advanceUntilIdle()

        assertTrue(requireNotNull(viewModel.uiState.value).items.isEmpty())
    }

    @Test
    fun `save failure shows a message`() = runTest(dispatcher) {
        val viewModel = ShoppingListViewModel(repository)
        advanceUntilIdle()

        repository.failMutations = true
        viewModel.addItem("Bread", 1)
        advanceUntilIdle()

        assertEquals(UiMessage.SAVE_FAILED, viewModel.message.value)
    }

    private fun item(
        id: String,
        name: String,
        quantity: Int = 1,
        purchased: Boolean = false
    ) = Item(
        id = id,
        name = name,
        quantity = quantity,
        purchased = purchased,
        createdAt = 0L
    )
}

private class FakeItemRepository : ItemRepository {
    var items: MutableList<Item> = mutableListOf()
    var failGet = false
    var failMutations = false
    private var nextId = 1

    override suspend fun getItems(): Result<List<Item>> =
        if (failGet) Result.failure(IOException("offline")) else Result.success(items.toList())

    override suspend fun addItem(name: String, quantity: Int): Result<Item> {
        if (failMutations) return Result.failure(IOException("offline"))
        val created = Item(
            id = "id-${nextId++}",
            name = name,
            quantity = quantity,
            purchased = false,
            createdAt = 0L
        )
        items.add(created)
        return Result.success(created)
    }

    override suspend fun updateItem(item: Item): Result<Item> {
        if (failMutations) return Result.failure(IOException("offline"))
        val index = items.indexOfFirst { it.id == item.id }
        if (index == -1) return Result.failure(IOException("missing"))
        items[index] = item
        return Result.success(item)
    }

    override suspend fun deleteItem(id: String): Result<Unit> {
        if (failMutations) return Result.failure(IOException("offline"))
        items.removeAll { it.id == id }
        return Result.success(Unit)
    }
}
