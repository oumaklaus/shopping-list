package com.example.shoppinglist

import com.example.shoppinglist.ui.InputError
import com.example.shoppinglist.ui.ItemInputResult
import com.example.shoppinglist.ui.ItemInputValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemInputValidatorTest {

    @Test
    fun `valid input is trimmed and accepted`() {
        val result = ItemInputValidator.validate("  Milk  ", "2")

        assertTrue(result is ItemInputResult.Valid)
        result as ItemInputResult.Valid
        assertEquals("Milk", result.name)
        assertEquals(2, result.quantity)
    }

    @Test
    fun `blank name is rejected`() {
        val result = ItemInputValidator.validate("   ", "2")

        assertEquals(
            InputError.NAME_EMPTY,
            (result as ItemInputResult.Invalid).nameError
        )
    }

    @Test
    fun `long name is rejected`() {
        val result = ItemInputValidator.validate("a".repeat(81), "2")

        assertEquals(
            InputError.NAME_TOO_LONG,
            (result as ItemInputResult.Invalid).nameError
        )
    }

    @Test
    fun `non numeric quantity is rejected`() {
        val result = ItemInputValidator.validate("Milk", "two")

        assertEquals(
            InputError.QUANTITY_INVALID,
            (result as ItemInputResult.Invalid).quantityError
        )
    }

    @Test
    fun `zero quantity is rejected`() {
        val result = ItemInputValidator.validate("Milk", "0")

        assertEquals(
            InputError.QUANTITY_INVALID,
            (result as ItemInputResult.Invalid).quantityError
        )
    }

    @Test
    fun `quantity above maximum is rejected`() {
        val result = ItemInputValidator.validate("Milk", "10000")

        assertEquals(
            InputError.QUANTITY_INVALID,
            (result as ItemInputResult.Invalid).quantityError
        )
    }

    @Test
    fun `maximum quantity is accepted`() {
        val result = ItemInputValidator.validate("Milk", "9999")

        assertTrue(result is ItemInputResult.Valid)
    }
}
