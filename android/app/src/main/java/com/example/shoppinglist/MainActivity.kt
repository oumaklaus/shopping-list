package com.example.shoppinglist

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shoppinglist.data.Item
import com.example.shoppinglist.data.AuthSession
import com.example.shoppinglist.data.SharedPreferencesSessionStorage
import com.example.shoppinglist.data.SessionStorage
import com.example.shoppinglist.databinding.ActivityMainBinding
import com.example.shoppinglist.databinding.DialogItemBinding
import com.example.shoppinglist.ui.InputError
import com.example.shoppinglist.ui.ItemAdapter
import com.example.shoppinglist.ui.ItemInputResult
import com.example.shoppinglist.ui.ItemInputValidator
import com.example.shoppinglist.ui.ShoppingListViewModel
import com.example.shoppinglist.ui.UiMessage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var session: SessionStorage
    private val viewModel: ShoppingListViewModel by viewModels()

    private val adapter = ItemAdapter(
        onPurchasedChanged = { item, purchased -> viewModel.setPurchased(item, purchased) },
        onEdit = { item -> showItemDialog(item) },
        onDelete = { item -> confirmDelete(item) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SharedPreferencesSessionStorage(this)
        if (session.token.isNullOrBlank()) {
            openLogin(false)
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerItems.layoutManager = LinearLayoutManager(this)
        binding.recyclerItems.adapter = adapter

        binding.toolbar.subtitle = session.email
        binding.toolbar.inflateMenu(R.menu.main_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_logout) {
                session.clear()
                openLogin(false)
                true
            } else {
                false
            }
        }

        AuthSession.expired.observe(this) { expired ->
            if (expired == true && !isFinishing) {
                AuthSession.expirationHandled()
                openLogin(true)
            }
        }

        binding.fabAdd.setOnClickListener { showItemDialog(null) }
        binding.buttonRetry.setOnClickListener { viewModel.loadItems() }

        observeViewModel()
    }

    private fun openLogin(sessionExpired: Boolean) {
        startActivity(
            Intent(this, LoginActivity::class.java)
                .putExtra(LoginActivity.EXTRA_SESSION_EXPIRED, sessionExpired)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            adapter.submitList(state.items)

            binding.progressBar.isVisible = state.isLoading && state.items.isEmpty()
            binding.recyclerItems.isVisible = state.items.isNotEmpty()
            binding.emptyState.isVisible =
                !state.isLoading && state.items.isEmpty() && !state.loadError
            binding.errorState.isVisible =
                !state.isLoading && state.items.isEmpty() && state.loadError
        }

        viewModel.message.observe(this) { message ->
            if (message == null) return@observe

            val textRes = when (message) {
                UiMessage.REFRESH_FAILED -> R.string.error_refresh
                UiMessage.SAVE_FAILED -> R.string.error_save
            }
            Snackbar.make(binding.root, textRes, Snackbar.LENGTH_LONG).show()
            viewModel.messageShown()
        }
    }

    private fun showItemDialog(item: Item?) {
        val dialogBinding = DialogItemBinding.inflate(layoutInflater)

        if (item == null) {
            dialogBinding.inputQuantity.setText(
                String.format(Locale.US, "%d", DEFAULT_QUANTITY)
            )
        } else {
            dialogBinding.inputName.setText(item.name)
            dialogBinding.inputQuantity.setText(
                String.format(Locale.US, "%d", item.quantity)
            )
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (item == null) R.string.add_item else R.string.edit_item)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val result = ItemInputValidator.validate(
                    rawName = dialogBinding.inputName.text?.toString().orEmpty(),
                    rawQuantity = dialogBinding.inputQuantity.text?.toString().orEmpty()
                )

                when (result) {
                    is ItemInputResult.Valid -> {
                        if (item == null) {
                            viewModel.addItem(result.name, result.quantity)
                        } else {
                            viewModel.updateItem(
                                item.copy(name = result.name, quantity = result.quantity)
                            )
                        }
                        dialog.dismiss()
                    }

                    is ItemInputResult.Invalid -> {
                        dialogBinding.layoutName.error =
                            result.nameError?.let { getString(it.toMessage()) }
                        dialogBinding.layoutQuantity.error =
                            result.quantityError?.let { getString(it.toMessage()) }
                    }
                }
            }
        }

        dialog.show()
    }

    private fun confirmDelete(item: Item) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_item)
            .setMessage(getString(R.string.delete_item_message, item.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteItem(item) }
            .show()
    }

    private fun InputError.toMessage(): Int = when (this) {
        InputError.NAME_EMPTY -> R.string.error_name_required
        InputError.NAME_TOO_LONG -> R.string.error_name_too_long
        InputError.QUANTITY_INVALID -> R.string.error_quantity_invalid
    }

    companion object {
        private const val DEFAULT_QUANTITY = 1
    }
}
