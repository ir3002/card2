package com.cardbudget.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ─── 카드사 enum ───────────────────────────────────────────
enum class CardIssuer(val displayName: String, val smsKeywords: List<String>) {
    SHINHAN("신한", listOf("신한카드", "[신한]", "신한BC")),
    KOOKMIN("국민", listOf("KB국민카드", "[KB]", "국민카드")),
    SAMSUNG("삼성", listOf("삼성카드", "[삼성]")),
    HYUNDAI("현대", listOf("현대카드", "[현대]")),
    LOTTE("롯데", listOf("롯데카드", "[롯데]")),
    HANA("하나", listOf("하나카드", "[하나]", "외환카드")),
    WOORI("우리", listOf("우리카드", "[우리]")),
    NH("농협", listOf("NH농협카드", "[NH]", "농협카드")),
    BC("BC", listOf("BC카드", "[BC]")),
    OTHER("기타", listOf())
}

// ─── 카드 엔티티 ───────────────────────────────────────────
@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                    // 카드 별칭 (예: "신한 Deep Dream")
    val issuer: CardIssuer,              // 카드사
    val lastFourDigits: String = "",     // 카드 번호 마지막 4자리
    val paymentDay: Int,                 // 결제일 (1~31)
    val billingCycleStartDay: Int,       // 이용기간 시작일 (보통 결제일 +1 전월)
    val goalAmount: Long = 500_000L,     // 월 목표 금액 (원)
    val color: String = "#1565C0",       // 카드 UI 색상
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// ─── 거래내역 엔티티 ───────────────────────────────────────
enum class TransactionSource { SMS_AUTO, MANUAL }
enum class TransactionCategory(val displayName: String, val emoji: String) {
    FOOD("식비", "🍔"),
    TRANSPORT("교통", "🚗"),
    SHOPPING("쇼핑", "🛍️"),
    CULTURE("문화/여가", "🎬"),
    HEALTH("의료/건강", "💊"),
    CAFE("카페", "☕"),
    MART("마트/편의점", "🛒"),
    GAS("주유", "⛽"),
    SUBSCRIPTION("구독", "📱"),
    OTHER("기타", "💳")
}

@Entity(
    tableName = "transactions",
    foreignKeys = [ForeignKey(
        entity = CardEntity::class,
        parentColumns = ["id"],
        childColumns = ["cardId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cardId"), Index("transactionDate")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val merchantName: String,
    val amount: Long,                            // 원 단위 (양수 = 지출)
    val transactionDate: Long,                   // epoch millis
    val billingMonth: String,                    // "2026-05" 형식
    val source: TransactionSource,
    val category: TransactionCategory = TransactionCategory.OTHER,
    val memo: String = "",
    val rawSmsBody: String = "",                 // 원문 SMS 저장
    val createdAt: Long = System.currentTimeMillis()
)

// ─── 월 예산 설정 엔티티 ────────────────────────────────────
@Entity(
    tableName = "monthly_budgets",
    indices = [Index(value = ["cardId", "yearMonth"], unique = true)]
)
data class MonthlyBudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long?,               // null = 전체 통합 예산
    val yearMonth: String,           // "2026-05"
    val goalAmount: Long,
    val alertAt70: Boolean = true,
    val alertAt90: Boolean = true,
    val alertAt100: Boolean = true
)
