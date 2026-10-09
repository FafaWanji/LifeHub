package com.example.lifeorganizer.money.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.lifeorganizer.money.data.MoneyRepository
import java.util.concurrent.TimeUnit

/** Books due fixed costs once a day, also when the app is not opened. */
class RecurringWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        MoneyRepository.get(applicationContext).bookDueRecurring()
        return Result.success()
    }

    companion object {
        private const val NAME = "money_recurring"

        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME, ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<RecurringWorker>(1, TimeUnit.DAYS).build()
            )
        }
    }
}
