package com.cardbudget.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cardbudget.MainActivity
import com.cardbudget.R
import com.cardbudget.data.repository.BudgetRepository
import com.cardbudget.data.repository.CardRepository
import com.cardbudget.data.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

// ─── 알림 채널 & 유틸 ──────────────────────────────────────
object NotificationHelper {
    const val CHANNEL_BUDGET = "budget_alert"
    const val CHANNEL_PAYMENT = "payment_reminder"
    const val CHANNEL_SMS = "sms_collected"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            NotificationChannel(CHANNEL_BUDGET, "예산 알림", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "예산 70%/90%/100% 도달 시 알림"
            },
            NotificationChannel(CHANNEL_PAYMENT, "결제일 알림", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "카드 결제일 안내"
            },
            NotificationChannel(CHANNEL_SMS, "거래 자동 수집", NotificationManager.IMPORTANCE_LOW).apply {
                description = "SMS 자동 거래 수집 알림"
            }
        ).forEach { nm.createNotificationChannel(it) }
    }

    fun sendBudgetAlert(context: Context, percent: Int, usedAmount: Long, goalAmount: Long, cardName: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val (title, body) = when {
            percent >= 100 -> "🚨 예산 초과!" to "$cardName 목표 금액을 초과했습니다\n사용: ${usedAmount.toKoreanWon()} / 목표: ${goalAmount.toKoreanWon()}"
            percent >= 90 -> "⚠️ 예산 90% 도달" to "$cardName 거의 다 사용했어요\n잔여: ${(goalAmount - usedAmount).toKoreanWon()}"
            else -> "📊 예산 70% 도달" to "$cardName 이번 달 예산 관리를 확인하세요\n잔여: ${(goalAmount - usedAmount).toKoreanWon()}"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(percent * 1000 + cardName.hashCode(), notification)
    }

    fun sendPaymentReminder(context: Context, cardName: String, amount: Long, dDay: Int, paymentDay: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = "결제 예정: ${amount.toKoreanWon()} (매월 ${paymentDay}일)"
        val notification = NotificationCompat.Builder(context, CHANNEL_PAYMENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("💳 $cardName 결제일 D-$dDay")
            .setContentText(body)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        nm.notify(cardName.hashCode() + paymentDay, notification)
    }

    fun sendSmsCollected(context: Context, merchantName: String, amount: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_SMS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("거래 자동 수집")
            .setContentText("$merchantName ${amount.toKoreanWon()} 추가됨")
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun Long.toKoreanWon(): String = "%,d원".format(this)
}

// ─── 예산 알림 WorkManager ─────────────────────────────────
@HiltWorker
class BudgetAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val cardRepository: CardRepository,
    private val budgetRepository: BudgetRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val cards = cardRepository.getAllActiveCardsOnce()

        cards.forEach { card ->
            val used = transactionRepository.getTotalAmountByCardAndMonth(card.id, yearMonth).first()
            val budget = budgetRepository.getCardBudget(card.id, yearMonth)
            val goal = budget?.goalAmount ?: card.goalAmount
            if (goal <= 0) return@forEach

            val percent = (used * 100 / goal).toInt()
            val alerts = budget ?: return@forEach

            when {
                percent >= 100 && alerts.alertAt100 ->
                    NotificationHelper.sendBudgetAlert(applicationContext, 100, used, goal, card.name)
                percent in 90..99 && alerts.alertAt90 ->
                    NotificationHelper.sendBudgetAlert(applicationContext, 90, used, goal, card.name)
                percent in 70..89 && alerts.alertAt70 ->
                    NotificationHelper.sendBudgetAlert(applicationContext, 70, used, goal, card.name)
            }
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BudgetAlertWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "budget_alert",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}

// ─── 결제일 알림 WorkManager ───────────────────────────────
@HiltWorker
class PaymentReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val cardRepository: CardRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val yearMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val cards = cardRepository.getAllActiveCardsOnce()

        cards.forEach { card ->
            val paymentDate = today.withDayOfMonth(minOf(card.paymentDay, today.lengthOfMonth()))
            val dDay = java.time.temporal.ChronoUnit.DAYS.between(today, paymentDate).toInt()
            if (dDay in 1..3) {
                val amount = transactionRepository
                    .getTotalAmountByCardAndMonth(card.id, yearMonth).first()
                if (amount > 0) {
                    NotificationHelper.sendPaymentReminder(
                        applicationContext, card.name, amount, dDay, card.paymentDay
                    )
                }
            }
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PaymentReminderWorker>(1, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "payment_reminder",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
