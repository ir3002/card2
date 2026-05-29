package com.cardbudget.util

import com.cardbudget.data.entity.CardIssuer
import java.text.SimpleDateFormat
import java.util.*

data class ParsedSmsTransaction(
    val merchantName: String,
    val amount: Long,
    val cardIssuer: CardIssuer,
    val lastFourDigits: String,
    val transactionDate: Long,
    val rawBody: String
)

object SmsParser {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)

    // ─── 카드사별 SMS 패턴 ─────────────────────────────────
    private val patterns = listOf(
        // 신한카드: [신한카드] 1234 05/29 14:23 스타벅스 6,500원 사용
        SmsPattern(
            issuer = CardIssuer.SHINHAN,
            keywords = listOf("신한카드", "[신한]"),
            amountRegex = Regex("""(\d{1,3}(?:,\d{3})*)원\s*(?:승인|사용)"""),
            merchantRegex = Regex("""(\d{2}/\d{2})\s+\d{2}:\d{2}\s+(.+?)\s+\d"""),
            cardNumRegex = Regex("""(\d{4})(?:\s|카드)""")
        ),
        // 국민카드: [Web발신] KB국민카드(1234) 05.29 14:23 이마트 32,400원 승인
        SmsPattern(
            issuer = CardIssuer.KOOKMIN,
            keywords = listOf("KB국민카드", "국민카드"),
            amountRegex = Regex("""(\d{1,3}(?:,\d{3})*)원\s*승인"""),
            merchantRegex = Regex("""\d{2}\.\d{2}\s+\d{2}:\d{2}\s+(.+?)\s+\d"""),
            cardNumRegex = Regex("""\((\d{4})\)""")
        ),
        // 삼성카드: [삼성카드] 5,500원(일시불) GS25 1234 05/29 14:00 승인
        SmsPattern(
            issuer = CardIssuer.SAMSUNG,
            keywords = listOf("삼성카드", "[삼성]"),
            amountRegex = Regex("""(\d{1,3}(?:,\d{3})*)원"""),
            merchantRegex = Regex("""원(?:\(\S+\))?\s+(.+?)\s+\d{4}"""),
            cardNumRegex = Regex("""(\d{4})\s+\d{2}/\d{2}""")
        ),
        // 현대카드: [현대카드] 현대카드(1234) 14:23 35,000원 주유소 승인
        SmsPattern(
            issuer = CardIssuer.HYUNDAI,
            keywords = listOf("현대카드", "[현대]"),
            amountRegex = Regex("""(\d{1,3}(?:,\d{3})*)원"""),
            merchantRegex = Regex("""원\s+(.+?)\s+(?:승인|취소)"""),
            cardNumRegex = Regex("""\((\d{4})\)""")
        ),
        // 롯데카드: [롯데카드] 1234 05/29 11,000원 롯데마트 승인
        SmsPattern(
            issuer = CardIssuer.LOTTE,
            keywords = listOf("롯데카드", "[롯데]"),
            amountRegex = Regex("""(\d{1,3}(?:,\d{3})*)원"""),
            merchantRegex = Regex("""원\s+(.+?)\s+(?:승인|취소)"""),
            cardNumRegex = Regex("""(\d{4})\s+\d{2}/\d{2}""")
        ),
        // 하나카드: [하나카드] 하나(1234) 05/29 14:23 GS칼텍스 58,000원 승인
        SmsPattern(
            issuer = CardIssuer.HANA,
            keywords = listOf("하나카드", "[하나]", "외환카드"),
            amountRegex = Regex("""(\d{1,3}(?:,\d{3})*)원\s*승인"""),
            merchantRegex = Regex("""\d{2}/\d{2}\s+\d{2}:\d{2}\s+(.+?)\s+\d"""),
            cardNumRegex = Regex("""\((\d{4})\)""")
        ),
        // 우리카드: [우리카드] 1234 05/29 14:23 카카오페이 15,000원
        SmsPattern(
            issuer = CardIssuer.WOORI,
            keywords = listOf("우리카드", "[우리]"),
            amountRegex = Regex("""(\d{1,3}(?:,\d{3})*)원"""),
            merchantRegex = Regex("""\d{2}/\d{2}\s+\d{2}:\d{2}\s+(.+?)\s+\d"""),
            cardNumRegex = Regex("""(\d{4})\s""")
        ),
        // NH농협: [NH농협카드] 1234 29일 14:23 편의점 8,900원 승인
        SmsPattern(
            issuer = CardIssuer.NH,
            keywords = listOf("NH농협카드", "[NH]", "농협카드"),
            amountRegex = Regex("""(\d{1,3}(?:,\d{3})*)원\s*승인"""),
            merchantRegex = Regex("""\d{2}일\s+\d{2}:\d{2}\s+(.+?)\s+\d"""),
            cardNumRegex = Regex("""(\d{4})\s""")
        )
    )

    // ─── 제외 키워드 (승인취소, 정산, 광고 등) ──────────────
    private val excludeKeywords = listOf(
        "취소", "정산", "포인트", "광고", "안내", "만기", "한도",
        "결제예정", "이용대금", "명세서"
    )

    fun parse(sender: String, body: String): ParsedSmsTransaction? {
        // 취소 및 제외 메시지 필터
        if (excludeKeywords.any { body.contains(it) && !body.contains("사용취소") }) {
            if (body.contains("취소")) return null
        }

        // 카드사 패턴 매칭
        val pattern = patterns.firstOrNull { p ->
            p.keywords.any { kw -> body.contains(kw) || sender.contains(kw) }
        } ?: return null

        // 금액 파싱
        val amountStr = pattern.amountRegex.find(body)
            ?.groupValues?.get(1)
            ?.replace(",", "")
            ?: return null
        val amount = amountStr.toLongOrNull() ?: return null
        if (amount <= 0) return null

        // 가맹점명 파싱
        val merchantName = parseMerchantName(body, pattern) ?: return null

        // 카드번호 마지막 4자리
        val lastFour = pattern.cardNumRegex.find(body)?.groupValues?.get(1) ?: "****"

        return ParsedSmsTransaction(
            merchantName = merchantName.trim(),
            amount = amount,
            cardIssuer = pattern.issuer,
            lastFourDigits = lastFour,
            transactionDate = System.currentTimeMillis(),
            rawBody = body
        )
    }

    private fun parseMerchantName(body: String, pattern: SmsPattern): String? {
        return pattern.merchantRegex.find(body)?.let { match ->
            val groups = match.groupValues
            when {
                groups.size > 2 -> groups[2]
                groups.size > 1 -> groups[1]
                else -> null
            }
        }?.let { raw ->
            // 불필요한 suffix 제거
            raw.replace(Regex("""\s*(승인|사용|일시불|\d+개월).*"""), "")
                .trim()
                .take(30)
        }
    }

    // ─── 문자메시지함에서 과거 SMS 일괄 읽기 ────────────────
    fun detectIssuerFromKeywords(smsBody: String): CardIssuer? {
        return CardIssuer.values().firstOrNull { issuer ->
            issuer.smsKeywords.any { kw -> smsBody.contains(kw) }
        }
    }
}

private data class SmsPattern(
    val issuer: CardIssuer,
    val keywords: List<String>,
    val amountRegex: Regex,
    val merchantRegex: Regex,
    val cardNumRegex: Regex
)
