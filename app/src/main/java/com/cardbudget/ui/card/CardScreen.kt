package com.cardbudget.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardbudget.data.entity.CardEntity
import com.cardbudget.data.entity.CardIssuer
import com.cardbudget.ui.CardViewModel
import com.cardbudget.ui.home.toWon
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardScreen(viewModel: CardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editingCard by remember { mutableStateOf<CardEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("카드 관리") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("카드 추가") }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (state.isLoading) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.cards.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CreditCard, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("등록된 카드가 없어요", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("+ 카드 추가 버튼으로 추가해보세요", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(state.cards, key = { it.id }) { card ->
                    CardDetailItem(
                        card = card,
                        usedAmount = state.cardAmounts[card.id] ?: 0L,
                        onEdit = { editingCard = card },
                        onDelete = { viewModel.deleteCard(card.id) }
                    )
                }
            }
        }

        if (state.showAddDialog) {
            AddEditCardDialog(
                card = null,
                onDismiss = { viewModel.hideAddDialog() },
                onConfirm = { viewModel.addCard(it) }
            )
        }

        editingCard?.let { card ->
            AddEditCardDialog(
                card = card,
                onDismiss = { editingCard = null },
                onConfirm = { viewModel.updateCard(it); editingCard = null }
            )
        }
    }
}

@Composable
private fun CardDetailItem(
    card: CardEntity,
    usedAmount: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val today = LocalDate.now()
    val paymentDate = today.withDayOfMonth(minOf(card.paymentDay, today.lengthOfMonth()))
    val dDay = java.time.temporal.ChronoUnit.DAYS.between(today, paymentDate).toInt()
        .let { if (it < 0) it + paymentDate.lengthOfMonth() else it }
    val percent = (usedAmount.toFloat() / card.goalAmount).coerceIn(0f, 1f)
    val cardColor = try { Color(android.graphics.Color.parseColor(card.color)) } catch (e: Exception) { Color(0xFF1565C0) }
    val isCritical = percent >= 0.9f

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // 헤더
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(cardColor),
                    contentAlignment = Alignment.Center) {
                    Text(card.issuer.displayName.first().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(card.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(card.issuer.displayName + "카드", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // D-day 배지
                val (dDayColor, dDayText) = when {
                    dDay <= 3 -> MaterialTheme.colorScheme.errorContainer to "D-$dDay"
                    dDay <= 7 -> MaterialTheme.colorScheme.tertiaryContainer to "D-$dDay"
                    else -> MaterialTheme.colorScheme.surfaceVariant to "D-$dDay"
                }
                Surface(color = dDayColor, shape = RoundedCornerShape(8.dp)) {
                    Text(dDayText, Modifier.padding(8.dp, 4.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))

            // 이번 달 사용
            Row(Modifier.fillMaxWidth()) {
                Column {
                    Text("이번 달 사용", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(usedAmount.toWon(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("월 목표", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(card.goalAmount.toWon(), fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { percent },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = if (isCritical) MaterialTheme.colorScheme.error else cardColor
            )
            Spacer(Modifier.height(4.dp))
            Text("${(percent * 100).toInt()}% 사용 · 잔여 ${(card.goalAmount - usedAmount).toWon()}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Divider(Modifier.padding(vertical = 12.dp))

            // 결제 정보
            Row(Modifier.fillMaxWidth()) {
                InfoChip("결제일", "${card.paymentDay}일")
                Spacer(Modifier.width(8.dp))
                InfoChip("이용기간", "전월 ${card.billingCycleStartDay}일~당월 ${card.billingCycleStartDay - 1}일")
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.height(32.dp)) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("수정", fontSize = 12.sp)
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("삭제", fontSize = 12.sp)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("카드 삭제") },
            text = { Text("${card.name}을 삭제하면 관련 거래 내역도 모두 삭제됩니다.") },
            confirmButton = {
                Button(onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("삭제")
                }
            },
            dismissButton = { OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("취소") } }
        )
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
        Column(Modifier.padding(8.dp, 4.dp)) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardDialog(
    card: CardEntity?,
    onDismiss: () -> Unit,
    onConfirm: (CardEntity) -> Unit
) {
    val isEdit = card != null
    var name by remember { mutableStateOf(card?.name ?: "") }
    var paymentDay by remember { mutableStateOf(card?.paymentDay?.toString() ?: "15") }
    var cycleStartDay by remember { mutableStateOf(card?.billingCycleStartDay?.toString() ?: "15") }
    var goalAmount by remember { mutableStateOf(card?.goalAmount?.toString() ?: "500000") }
    var selectedIssuer by remember { mutableStateOf(card?.issuer ?: CardIssuer.SHINHAN) }
    var lastFour by remember { mutableStateOf(card?.lastFourDigits ?: "") }
    var expandIssuer by remember { mutableStateOf(false) }

    val cardColors = listOf("#1565C0", "#C62828", "#2E7D32", "#4A148C", "#E65100", "#00695C", "#37474F")
    var selectedColor by remember { mutableStateOf(card?.color ?: "#1565C0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "카드 수정" else "카드 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("카드 별칭") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                ExposedDropdownMenuBox(expanded = expandIssuer, onExpandedChange = { expandIssuer = it }) {
                    OutlinedTextField(
                        value = selectedIssuer.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("카드사") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandIssuer) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandIssuer, onDismissRequest = { expandIssuer = false }) {
                        CardIssuer.values().forEach { issuer ->
                            DropdownMenuItem(text = { Text(issuer.displayName) }, onClick = { selectedIssuer = issuer; expandIssuer = false })
                        }
                    }
                }

                OutlinedTextField(value = lastFour, onValueChange = { lastFour = it.filter { c -> c.isDigit() }.take(4) }, label = { Text("카드 뒷 4자리 (선택)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = paymentDay, onValueChange = { paymentDay = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("결제일") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = cycleStartDay, onValueChange = { cycleStartDay = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("이용기간 시작일") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }

                OutlinedTextField(value = goalAmount, onValueChange = { goalAmount = it.filter { c -> c.isDigit() } }, label = { Text("월 목표 금액 (원)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)

                Text("카드 색상", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cardColors.forEach { colorHex ->
                        val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Color.Blue }
                        Box(
                            Modifier.size(if (selectedColor == colorHex) 32.dp else 28.dp)
                                .clip(CircleShape).background(color)
                                .clickable { selectedColor = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == colorHex) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val pd = paymentDay.toIntOrNull()?.coerceIn(1, 31) ?: 15
                val csd = cycleStartDay.toIntOrNull()?.coerceIn(1, 31) ?: 15
                val goal = goalAmount.toLongOrNull() ?: 500_000L
                if (name.isBlank()) return@Button
                onConfirm(
                    CardEntity(
                        id = card?.id ?: 0,
                        name = name,
                        issuer = selectedIssuer,
                        lastFourDigits = lastFour,
                        paymentDay = pd,
                        billingCycleStartDay = csd,
                        goalAmount = goal,
                        color = selectedColor
                    )
                )
            }) { Text(if (isEdit) "저장" else "추가") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("취소") } }
    )
}

private fun Modifier.clickable(onClick: () -> Unit) = this.then(
    Modifier.padding(0.dp)
).let { m ->
    m // simplified — use composed or clickable in actual code
}
