package com.cardbudget

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.cardbudget.util.BudgetAlertWorker
import com.cardbudget.util.NotificationHelper
import com.cardbudget.util.PaymentReminderWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CardBudgetApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        BudgetAlertWorker.schedule(this)
        PaymentReminderWorker.schedule(this)
    }
}
