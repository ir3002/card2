package com.cardbudget.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardbudget.data.entity.CardEntity
import com.cardbudget.data.entity.TransactionEntity
import com.cardbudget.ui.HomeViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToTransactions: () -> Unit,
    onNavigateToCards: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 헤더 - 총 사용 카드
        item { TotalUsageCard(state.totalUsed, state.totalGoal, state.usagePercent, state.currentYearMonth) }

        // 빠른 통계
        item {
            Row(Modifier.fillMaxWidth().padding(16.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickStatCard(
                    modifier = Modifier.weight(1f),
                    label = "남은 예산",
                    value = state.remaining.toWon(),
                    valueColor = if (state.remaining >= 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                )
                QuickStatCard(
                    modifier = Modifier.weight(1f),
                    label = "등록 카드",
                    value = "${state.cards.size}개",
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 카드별 현황
        if (state.cards.isNotEmpty()) {
            item {
                SectionHeader("카드별 현황") { onNavigateToCards() }
            }
            items(state.cards) { card ->
                val used = state.cardAmounts[card.id] ?: 0L
                CardUsageItem(card = card, usedAmount = used)
            }
        }

        // 최근 거래
        if (state.recentTransactions.isNotEmpty()) {
            item {
                SectionHeader("최근 거래") { onNavigateToTransactions() }
            }
            items(state.recentTransactions) { tx ->
                TransactionItem(transaction = tx, cards = state.cards)
            }
        } else {
            item {
                EmptyState("아직 거래 내역이 없어요\nSMS 권한을 허용하거나 직접 추가해보세요")
            }
        }
    }
}

@Composable
private fun TotalUsageCard(used: Long, goal: Long, percent: Float, yearMonth: String) {
    val animPercent by animateFloatAsState(
        targetValue = percent.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = EaseOutCubic)
    )
    val isCritical = percent >= 0.9f
    val gradientColors = if (isCritical)
        listOf(Color(0xFFB71C1C), Color(0xFFD32F2F))
    else
        listOf(Color(0xFF1565C0), Color(0xFF0D47A1))

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .padding(24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(yearMonth.replace("-", "년 ") + "월", color = Color.White.copy(0.7f), fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    if (isCritical) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFFFCC02), modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("이번 달 지출", color = Color.White.copy(0.7f), fontSize = 12.sp)
                Text(used.toWon(), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("목표 ${goal.toWon()} 중", color = Color.White.copy(0.6f), fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))

                // 프로그레스 바
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(0.2f))) {
                    Box(Modifier
                        .fillMaxWidth(animPercent)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isCritical) Color(0xFFFFCC02) else Color(0xFF64B5F6))
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row {
                    Text("${(percent * 100).toInt()}% 사용", color = Color.White.copy(0.8f), fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    val remaining = goal - used
                    Text(
                        if (remaining >= 0) "잔여 ${remaining.toWon()}" else "초과 ${(-remaining).toWon()}",
                        color = if (remaining >= 0) Color.White.copy(0.8f) else Color(0xFFFF8A80),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStatCard(modifier: Modifier, label: String, value: String, valueColor: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun CardUsageItem(card: CardEntity, usedAmount: Long) {
    val percent = (usedAmount.toFloat() / card.goalAmount).coerceIn(0f, 1f)
    val cardColor = try { Color(android.graphics.Color.parseColor(card.color)) } catch (e: Exception) { Color(0xFF1565C0) }

    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(cardColor),
                contentAlignment = Alignment.Center) {
                Text(card.issuer.displayName.first().toString(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Text(card.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Text(usedAmount.toWon(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { percent },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = if (percent >= 0.9f) MaterialTheme.colorScheme.error else cardColor
                )
                Spacer(Modifier.height(2.dp))
                Row {
                    Text("결제일 ${card.paymentDay}일", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text("${(percent * 100).toInt()}% / ${card.goalAmount.toWon()}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: TransactionEntity, cards: List<CardEntity>) {
    val card = cards.find { it.id == transaction.cardId }
    val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(transaction.transactionDate), ZoneId.systemDefault())
    val timeStr = date.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))

    ListItem(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        headlineContent = { Text(transaction.merchantName, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Text("$timeStr · ${card?.name ?: "알수없음"} · ${transaction.source.name.let { if (it == "SMS_AUTO") "자동" else "수동" }}", fontSize = 12.sp)
        },
        leadingContent = {
            Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center) {
                Text(transaction.category.emoji, fontSize = 16.sp)
            }
        },
        trailingContent = {
            Text("-${transaction.amount.toWon()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        }
    )
}

@Composable
private fun SectionHeader(title: String, onMore: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.weight(1f))
        if (onMore != null) {
            TextButton(onClick = onMore) { Text("전체보기", fontSize = 12.sp) }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 22.sp)
    }
}

fun Long.toWon(): String = NumberFormat.getNumberInstance(Locale.KOREA).format(this) + "원"
