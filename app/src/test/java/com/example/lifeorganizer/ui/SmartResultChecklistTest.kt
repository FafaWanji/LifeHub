package com.example.lifeorganizer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SmartResultChecklistTest {

    @Test
    fun `plain and bulleted lines become checklist items`() {
        assertEquals("- [ ] Milch\n- [ ] Brot\n- [ ] Eier", toChecklist("Milch\n- Brot\n\n1. Eier"))
    }

    @Test
    fun `existing task items are kept as they are`() {
        assertEquals("- [x] Milch\n- [ ] Brot", toChecklist("- [x] Milch\n  - [ ] Brot"))
    }
}
