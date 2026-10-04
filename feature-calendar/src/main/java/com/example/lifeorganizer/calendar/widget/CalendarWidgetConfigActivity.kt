package com.example.lifeorganizer.calendar.widget

import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.core.i18n.Str
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.lifeorganizer.calendar.data.AppDatabase
import com.example.lifeorganizer.core.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class CalendarWidgetConfigActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setResult(Activity.RESULT_CANCELED)
        
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            AppTheme {
                val db = remember { AppDatabase.getDatabase(this) }
                val categories by db.eventDao().getCategories().collectAsState(initial = emptyList())
                
                var selectedCategories by remember { mutableStateOf<Set<Long>>(emptySet()) }
                
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(Str.widgetCategories.text()) },
                            actions = {
                                TextButton(onClick = {
                                    saveWidgetConfig(this@CalendarWidgetConfigActivity, appWidgetId, selectedCategories)
                                    val resultValue = Intent().apply {
                                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                    }
                                    setResult(Activity.RESULT_OK, resultValue)
                                    
                                    // Trigger a manual update
                                    runBlocking {
                                        CalendarWidget().update(this@CalendarWidgetConfigActivity, GlanceAppWidgetManager(this@CalendarWidgetConfigActivity).getGlanceIdBy(appWidgetId))
                                    }
                                    finish()
                                }) {
                                    Text(Str.save.text())
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = {
                                selectedCategories = categories.map { it.id }.toSet() + (-1L)
                            }) {
                                Text(Str.selectAll.text())
                            }
                            TextButton(onClick = {
                                selectedCategories = emptySet()
                            }) {
                                Text(Str.clear.text())
                            }
                        }

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedCategories.contains(-1L),
                                        onCheckedChange = { isChecked ->
                                            selectedCategories = if (isChecked) {
                                                selectedCategories + (-1L)
                                            } else {
                                                selectedCategories - (-1L)
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(Str.noCategory.text(), modifier = Modifier.weight(1f))
                                }
                            }
                            items(categories) { category ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedCategories.contains(category.id),
                                        onCheckedChange = { isChecked ->
                                            selectedCategories = if (isChecked) {
                                                selectedCategories + category.id
                                            } else {
                                                selectedCategories - category.id
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(category.name, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    companion object {
        private const val PREFS_NAME = "com.example.lifeorganizer.calendar.widget.CalendarWidget"
        private const val PREF_PREFIX_KEY = "appwidget_"
        
        fun saveWidgetConfig(context: Context, appWidgetId: Int, categoryIds: Set<Long>) {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
            val idStrings = categoryIds.map { it.toString() }.toSet()
            prefs.putStringSet(PREF_PREFIX_KEY + appWidgetId, idStrings)
            prefs.apply()
        }
        
        fun loadWidgetConfig(context: Context, appWidgetId: Int): Set<Long> {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            val strings = prefs.getStringSet(PREF_PREFIX_KEY + appWidgetId, emptySet()) ?: emptySet()
            return strings.mapNotNull { it.toLongOrNull() }.toSet()
        }
    }
}
