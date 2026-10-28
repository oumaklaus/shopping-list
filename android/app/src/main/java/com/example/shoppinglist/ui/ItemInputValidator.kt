package com.example.shoppinglist.ui

enum class InputError {
    NAME_EMPTY,
    NAME_TOO_LONG,
    QUANTITY_INVALID
}

sealed interface ItemInputResult {
    data class Valid(val name: String, val quantity: Int) : ItemInputResult
    data class Invalid(
        val nameError: InputError? = null,
        val quantityError: InputError? = null
    ) : ItemInputResult
}

object ItemInputValidator {
    const val MAX_NAME_LENGTH = 80
    const val MAX_QUANTITY = 9999

    fun validate(rawName: String, rawQuantity: String): ItemInputResult {
        val name = rawName.trim()
        val quantity = rawQuantity.trim().toIntOrNull()

        val nameError = when {
            name.isEmpty() -> InputError.NAME_EMPTY
            name.length > MAX_NAME_LENGTH -> InputError.NAME_TOO_LONG
            else -> null
        }

        val quantityError = when {
            quantity == null || quantity < 1 || quantity > MAX_QUANTITY ->
                InputError.QUANTITY_INVALID
            else -> null
        }

        return if (nameError == null && quantityError == null) {
            ItemInputResult.Valid(name = name, quantity = requireNotNull(quantity))
        } else {
            ItemInputResult.Invalid(
                nameError = nameError,
                quantityError = quantityError
            )
        }
    }
}
