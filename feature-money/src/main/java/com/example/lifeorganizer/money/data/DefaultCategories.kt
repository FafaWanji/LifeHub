package com.example.lifeorganizer.money.data

import com.example.lifeorganizer.core.i18n.Txt

/** Categories created on first start, named in the app language. All can be renamed later. */
object DefaultCategories {
    data class Def(val icon: String, val name: Txt, val color: Int, val kind: CategoryKind, val fallback: Boolean = false)

    val all = listOf(
        Def("cart", Txt("Groceries", "Lebensmittel", "Market", "Supermercado"), 0xFF4CAF50.toInt(), CategoryKind.EXPENSE),
        Def("home", Txt("Housing", "Wohnen", "Konut", "Vivienda"), 0xFF795548.toInt(), CategoryKind.EXPENSE),
        Def("car", Txt("Transport", "Mobilität", "Ulaşım", "Transporte"), 0xFF2196F3.toInt(), CategoryKind.EXPENSE),
        Def("party", Txt("Leisure", "Freizeit", "Eğlence", "Ocio"), 0xFFFF9800.toInt(), CategoryKind.EXPENSE),
        Def("subscriptions", Txt("Subscriptions", "Abos", "Abonelikler", "Suscripciones"), 0xFF9C27B0.toInt(), CategoryKind.EXPENSE),
        Def("health", Txt("Health", "Gesundheit", "Sağlık", "Salud"), 0xFFE91E63.toInt(), CategoryKind.EXPENSE),
        Def("shopping", Txt("Shopping", "Shopping", "Alışveriş", "Compras"), 0xFF00BCD4.toInt(), CategoryKind.EXPENSE),
        Def("other", Txt("Other", "Sonstiges", "Diğer", "Otros"), 0xFF9E9E9E.toInt(), CategoryKind.EXPENSE, fallback = true),
        Def("salary", Txt("Salary", "Gehalt", "Maaş", "Salario"), 0xFF388E3C.toInt(), CategoryKind.INCOME),
        Def("income", Txt("Other income", "Sonstige Einnahmen", "Diğer gelir", "Otros ingresos"), 0xFF8BC34A.toInt(), CategoryKind.INCOME, fallback = true)
    )
}
