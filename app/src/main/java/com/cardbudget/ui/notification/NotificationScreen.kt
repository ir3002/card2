package com.cardbudget.ui.notification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardbudget.data.entity.CardEntity
import com.cardbudget.data.entity.MonthlyBudgetEntity
import com.cardbudget.data.repository.BudgetRepository
import com.cardbudget.data.repository.CardRepository
import com.cardbudget.ui.currentYearMonth
import com.cardbudget.ui.home.toWon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── NotificationViewModel ─────────────────────────────────
data class NotificationUiState(
    val cards: List<CardEntity> = emptyList(),
    val budgets: List<MonthlyBudgetEntity> = emptyList(),
    val globalGoal: Long = 500_000L,
    val isLoading: Boolean = true
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val cardRepo: CardRepository,
    private val budgetRepo: BudgetRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationUiState())
    val state: StateFlow<NotificationUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                cardRepo.getAllActiveCards(),
                budgetRepo.getBudgetsByMonth(currentYearMonth())
            ) { cards, budgets -> Pair(cards, budgets) }
            .collect { (cards, budgets) ->
                _state.value = NotificationUiState(cards = cards, budgets = budgets, isLoading = false)
            }
        }
    }

    fun updateBudgetAlert(cardId: Long?, field: String, value: Boolean) {
        viewModelScope.launch {
            val ym = currentYearMonth()
            val existing = if (cardId == null) budgetRepo.getTotalBudget(ym)
                           else budgetRepo.getCardBudget(cardId, ym)
            val budget = existing ?: MonthlyBudgetEntity(cardId = cardId, yearMonth = ym, goalAmount = 500_000L)
            val updated = when (field) {
                "70" -> budget.copy(alertAt70 = value)
                "90" -> budget.copy(alertAt90 = value)
                "100" -> budget.copy(alertAt100 = value)
                else -> budget
            }
            budgetRepo.upsertBudget(updated)
        }
    }

    fun updateGoalAmount(cardId: Long?, amount: Long) {
        viewModelScope.launch {
            val ym = currentYearMonth()
            val existing = if (cardId == null) budgetRepo.getTotalBudget(ym)
                           else cardId.let { budgetRepo.getCardBudget(it, ym) }
            val budget = existing ?: MonthlyBudgetEntity(cardId = cardId, yearMonth = ym, goalAmount = amount)
            budgetRepo.upsertBudget(budget.copy(goalAmount = amount))
            if (cardId != null) {
                val card = cardRepo.getCardById(cardId)
                card?.let { cardRepo.updateCard(it.copy(goalAmount = amount)) }
            }
        }
    }
}

// ─── Screen ────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(viewModel: NotificationViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("알림 & 예산 설정") }) }) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {

            // 전체 예산 알림
            item { SectionTitle("전체 예산 알림") }
            item {
                val totalBudget = state.budgets.firstOrNull { it.cardId == null }
                BudgetAlertCard(
                    title = "전체 통합",
                    goalAmount = totalBudget?.goalAmount ?: 500_000L,
                    alert70 = totalBudget?.alertAt70 ?: true,
                    alert90 = totalBudget?.alertAt90 ?: true,
                    alert100 = totalBudget?.alertAt100 ?: true,
                    onToggle70 = { viewModel.updateBudgetAlert(null, "70", it) },
                    onToggle90 = { viewModel.updateBudgetAlert(null, "90", it) },
                    onToggle100 = { viewModel.updateBudgetAlert(null, "100", it) },
                    onGoalChange = { viewModel.updateGoalAmount(null, it) }
                )
            }

            // 카드별 예산 알림
            item { SectionTitle("카드별 예산 알림") }
            items(state.cards.size) { i ->
                val card = state.cards[i]
                val budget = state.budgets.firstOrNull { it.cardId == card.id }
                BudgetAlertCard(
                    title = card.name,
                    goalAmount = budget?.goalAmount ?: card.goalAmount,
                    alert70 = budget?.alertAt70 ?: true,
                    alert90 = budget?.alertAt90 ?: true,
                    alert100 = budget?.alertAt100 ?: true,
                    onToggle70 = { viewModel.updateBudgetAlert(card.id, "70", it) },
                    onToggle90 = { viewModel.updateBudgetAlert(card.id, "90", it) },
                    onToggle100 = { viewModel.updateBudgetAlert(card.id, "100", it) },
                    onGoalChange = { viewModel.updateGoalAmount(card.id, it) }
                )
            }

            // 결제일 알림 정보
            item { SectionTitle("결제일 알림") }
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AlertToggleRow(Icons.Default.CalendarMonth, "결제일 3일 전 알림", "카드별 결제일 3일 전에 알림", true, {})
                        AlertToggleRow(Icons.Default.CalendarToday, "결제일 1일 전 알림", "D-1 최종 알림", false, {})
                        AlertToggleRow(Icons.Default.Sms, "SMS 수집 실패 알림", "자동 수집 실패 시 수동 입력 유도", false, {})
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetAlertCard(
    title: String,
    goalAmount: Long,
    alert70: Boolean, alert90: Boolean, alert100: Boolean,
    onToggle70: (Boolean) -> Unit, onToggle90: (Boolean) -> Unit, onToggle100: (Boolean) -> Unit,
    onGoalChange: (Long) -> Unit
) {
    var editingGoal by remember { mutableStateOf(false) }
    var goalText by remember { mutableStateOf(goalAmount.toString()) }

    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { editingGoal = !editingGoal }) {
                    Text(if (editingGoal) "저장" else goalAmount.toWon(), fontSize = 12.sp)
                    if (!editingGoal) Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp).padding(start = 2.dp))
                }
            }
            if (editingGoal) {
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it.filter { c -> c.isDigit() } },
                    label = { Text("목표 금액") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    singleLine = true,
                    trailingIcon = {
                        TextButton(onClick = {
                            goalText.toLongOrNull()?.let { onGoalChange(it) }
                            editingGoal = false
                        }) { Text("적용") }
                    }
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            AlertToggleRow(Icons.Default.BarChart, "70% 도달 시 알림", "주의 알림", alert70, onToggle70)
            AlertToggleRow(Icons.Default.Warning, "90% 도달 시 알림", "경고 알림", alert90, onToggle90)
            AlertToggleRow(Icons.Default.Error, "100% 초과 시 알림", "한도 초과", alert100, onToggle100)
        }
    }
}

@Composable
private fun AlertToggleRow(
    icon: ImageVector,
    label: String,
    sub: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onToggle, modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp), fontWeight = FontWeight.Bold, fontSize = 15.sp)
}
