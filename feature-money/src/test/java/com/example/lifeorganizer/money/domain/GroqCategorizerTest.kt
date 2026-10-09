package com.example.lifeorganizer.money.domain

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroqCategorizerTest {
    @Test fun requestContainsOnlyPayeesAndCategories() {
        val req = GroqCategorizer.buildRequest(listOf("REWE Markt", "Aral"), listOf("Lebensmittel", "Mobilität"))
        val text = req.toString()
        assertTrue(text.contains("REWE Markt"))
        assertTrue(text.contains("Mobilität"))
        assertFalse(text.contains("€"))
        assertEquals("json_object", req.getJSONObject("response_format").getString("type"))
    }

    @Test fun parsesKnownCategoriesAndDropsUnknown() {
        val content = """{"items":[{"payee":"REWE Markt","category":"lebensmittel"},{"payee":"Aral","category":"Urlaub"}]}"""
        val body = JSONObject().put("choices", org.json.JSONArray().put(
            JSONObject().put("message", JSONObject().put("content", content))
        )).toString()
        assertEquals(mapOf("REWE Markt" to "Lebensmittel"), GroqCategorizer.parseResponse(body, listOf("Lebensmittel", "Mobilität")))
    }

    @Test fun brokenResponseGivesEmptyMap() {
        assertEquals(emptyMap<String, String>(), GroqCategorizer.parseResponse("not json", listOf("A")))
    }
}
