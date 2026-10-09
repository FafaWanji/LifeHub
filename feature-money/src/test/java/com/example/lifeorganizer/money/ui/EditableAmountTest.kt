package com.example.lifeorganizer.money.ui

import com.example.lifeorganizer.core.util.Amounts
import org.junit.Assert.assertEquals
import org.junit.Test

class EditableAmountTest {
    @Test fun roundTrip() {
        assertEquals("12,05", editableAmount(-1205))
        assertEquals(1205L, Amounts.parseCents(editableAmount(1205)))
        assertEquals("", editableAmount(0))
    }
}
