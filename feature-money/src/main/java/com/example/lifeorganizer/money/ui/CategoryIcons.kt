package com.example.lifeorganizer.money.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.ui.graphics.vector.ImageVector

val categoryIconKeys = listOf("cart", "home", "car", "party", "subscriptions", "health", "shopping", "other", "salary", "income")

fun categoryIcon(key: String): ImageVector = when (key) {
    "cart" -> Icons.Outlined.ShoppingCart
    "home" -> Icons.Outlined.Home
    "car" -> Icons.Outlined.DirectionsCar
    "party" -> Icons.Outlined.Celebration
    "subscriptions" -> Icons.Outlined.Subscriptions
    "health" -> Icons.Outlined.HealthAndSafety
    "shopping" -> Icons.Outlined.ShoppingBag
    "salary" -> Icons.Outlined.Payments
    "income" -> Icons.Outlined.Savings
    else -> Icons.Outlined.Category
}
