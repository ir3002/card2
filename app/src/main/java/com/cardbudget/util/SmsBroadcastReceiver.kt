package com.cardbudget.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.cardbudget.data.entity.TransactionCategory
import com.cardbudget.data.entity.TransactionEntity
import com.cardbudget.data.entity.TransactionSource
import com.cardbudget.data.repository.CardRepository
import com.cardbudget.data.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class SmsBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var cardRepository: CardRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val fullBody = messages.joinToString("") { it.messageBody }
        val sender = messages.firstOrNull()?.originatingAddress ?: ""

        val parsed = SmsParser.parse(sender, fullBody) ?: return

        scope.launch {
            // 카드 목록에서 일치하는 카드 찾기
            val cards = cardRepository.getAllActiveCardsOnce()
            val matchedCard = cards.firstOrNull { card ->
                card.issuer.smsKeywords.any { kw -> fullBody.contains(kw) } &&
                    (card.lastFourDigits.isEmpty() || card.lastFourDigits == parsed.lastFourDigits)
            } ?: return@launch // 등록된 카드가 없으면 무시

            // 중복 체크 (5분 이내 동일 금액/가맹점)
            val fiveMinutes = 5 * 60 * 1000L
            val duplicate = transactionRepository.findDuplicate(
                merchantName = parsed.merchantName,
                amount = parsed.amount,
                from = parsed.transactionDate - fiveMinutes,
                to = parsed.transactionDate + fiveMinutes
            )
            if (duplicate != null) return@launch

            val billingMonth = calculateBillingMonth(matchedCard.paymentDay, matchedCard.billingCycleStartDay)

            val transaction = TransactionEntity(
                cardId = matchedCard.id,
                merchantName = parsed.merchantName,
                amount = parsed.amount,
                transactionDate = parsed.transactionDate,
                billingMonth = billingMonth,
                source = TransactionSource.SMS_AUTO,
                category = guessCategoryFromMerchant(parsed.merchantName),
                rawSmsBody = parsed.rawBody
            )
            transactionRepository.insertTransaction(transaction)
        }
    }

    // 결제일 기준 청구 월 계산
    private fun calculateBillingMonth(paymentDay: Int, cycleStartDay: Int): String {
        val today = LocalDate.now()
        val dayOfMonth = today.dayOfMonth
        val billingDate = if (dayOfMonth >= cycleStartDay) {
            today  // 이번 달 청구
        } else {
            today.minusMonths(1)  // 전월 청구
        }
        return billingDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }

    private fun guessCategoryFromMerchant(merchant: String): TransactionCategory {
        val lower = merchant.lowercase()
        return when {
            lower.containsAny("스타벅스", "커피", "카페", "투썸", "할리스", "이디야") -> TransactionCategory.CAFE
            lower.containsAny("이마트", "홈플러스", "롯데마트", "gs25", "cu", "세븐", "편의점", "마트") -> TransactionCategory.MART
            lower.containsAny("주유", "칼텍스", "sk에너지", "gs칼텍스", "오일") -> TransactionCategory.GAS
            lower.containsAny("지하철", "버스", "택시", "카카오택시", "티머니", "교통") -> TransactionCategory.TRANSPORT
            lower.containsAny("넷플릭스", "유튜브", "멜론", "구독", "애플", "구글") -> TransactionCategory.SUBSCRIPTION
            lower.containsAny("약국", "병원", "의원", "클리닉", "한의") -> TransactionCategory.HEALTH
            lower.containsAny("영화", "cgv", "롯데시네마", "메가박스", "헬스", "피트니스") -> TransactionCategory.CULTURE
            lower.containsAny("쿠팡", "배민", "우아한", "쿠팡이츠", "올리브영") -> TransactionCategory.SHOPPING
            lower.containsAny("식당", "김밥", "치킨", "피자", "분식", "한식", "중식") -> TransactionCategory.FOOD
            else -> TransactionCategory.OTHER
        }
    }

    private fun String.containsAny(vararg keys: String) = keys.any { this.contains(it) }
}

// ─── Boot Receiver ─────────────────────────────────────────
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // WorkManager는 자동 재시작되므로 추가 처리 불필요
        }
    }
}
