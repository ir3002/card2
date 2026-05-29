package com.cardbudget.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardbudget.data.entity.*
import com.cardbudget.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─── 공통 현재 달 계산 ──────────────────────────────────────
fun currentYearMonth(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

// ─── HomeViewModel ─────────────────────────────────────────
data class HomeUiState(
    val currentYearMonth: String = currentYearMonth(),
    val totalUsed: Long = 0L,
    val totalGoal: Long = 500_000L,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val cards: List<CardEntity> = emptyList(),
    val cardAmounts: Map<Long, Long> = emptyMap(),
    val isLoading: Boolean = true
) {
    val usagePercent: Float get() = if (totalGoal > 0) totalUsed.toFloat() / totalGoal else 0f
    val remaining: Long get() = totalGoal - totalUsed
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val cardRepo: CardRepository,
    private val budgetRepo: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        val ym = currentYearMonth()
        viewModelScope.launch {
            combine(
                transactionRepo.getTotalAmountByMonth(ym),
                transactionRepo.getRecentTransactions(ym, 5),
                cardRepo.getAllActiveCards()
            ) { total, recent, cards -> Triple(total, recent, cards) }
            .collect { (total, recent, cards) ->
                val budget = budgetRepo.getTotalBudget(ym)
                val goal = budget?.goalAmount ?: cards.sumOf { it.goalAmount }
                val cardAmounts = cards.associate { card ->
                    card.id to transactionRepo
                        .getTotalAmountByCardAndMonth(card.id, ym).first()
                }
                _uiState.value = HomeUiState(
                    currentYearMonth = ym,
                    totalUsed = total,
                    totalGoal = goal.coerceAtLeast(1L),
                    recentTransactions = recent,
                    cards = cards,
                    cardAmounts = cardAmounts,
                    isLoading = false
                )
            }
        }
    }
}

// ─── TransactionViewModel ──────────────────────────────────
data class TransactionUiState(
    val yearMonth: String = currentYearMonth(),
    val transactions: List<TransactionEntity> = emptyList(),
    val selectedCardId: Long? = null,
    val cards: List<CardEntity> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val cardRepo: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cardRepo.getAllActiveCards().collect { cards ->
                _uiState.update { it.copy(cards = cards) }
            }
        }
        loadTransactions()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            _uiState.flatMapLatest { state ->
                if (state.selectedCardId != null) {
                    transactionRepo.getTransactionsByCardAndMonth(state.selectedCardId, state.yearMonth)
                } else {
                    transactionRepo.getTransactionsByMonth(state.yearMonth)
                }
            }.collect { transactions ->
                _uiState.update { it.copy(transactions = transactions, isLoading = false) }
            }
        }
    }

    fun selectCard(cardId: Long?) {
        _uiState.update { it.copy(selectedCardId = cardId) }
        loadTransactions()
    }

    fun changeMonth(yearMonth: String) {
        _uiState.update { it.copy(yearMonth = yearMonth) }
        loadTransactions()
    }

    fun showAddDialog() = _uiState.update { it.copy(showAddDialog = true) }
    fun hideAddDialog() = _uiState.update { it.copy(showAddDialog = false) }

    fun addManualTransaction(
        cardId: Long,
        merchantName: String,
        amount: Long,
        category: TransactionCategory,
        memo: String,
        date: Long
    ) {
        viewModelScope.launch {
            val billingMonth = _uiState.value.yearMonth
            val tx = TransactionEntity(
                cardId = cardId,
                merchantName = merchantName,
                amount = amount,
                transactionDate = date,
                billingMonth = billingMonth,
                source = TransactionSource.MANUAL,
                category = category,
                memo = memo
            )
            transactionRepo.insertTransaction(tx)
            hideAddDialog()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch { transactionRepo.deleteTransaction(transaction) }
    }
}

// ─── CardViewModel ─────────────────────────────────────────
data class CardUiState(
    val cards: List<CardEntity> = emptyList(),
    val cardAmounts: Map<Long, Long> = emptyMap(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false
)

@HiltViewModel
class CardViewModel @Inject constructor(
    private val cardRepo: CardRepository,
    private val transactionRepo: TransactionRepository,
    private val budgetRepo: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardUiState())
    val uiState: StateFlow<CardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cardRepo.getAllActiveCards().collect { cards ->
                val ym = currentYearMonth()
                val amounts = cards.associate { card ->
                    card.id to transactionRepo
                        .getTotalAmountByCardAndMonth(card.id, ym).first()
                }
                _uiState.value = CardUiState(cards = cards, cardAmounts = amounts, isLoading = false)
            }
        }
    }

    fun addCard(card: CardEntity) {
        viewModelScope.launch {
            val id = cardRepo.insertCard(card)
            // 기본 예산 설정
            budgetRepo.upsertBudget(
                com.cardbudget.data.entity.MonthlyBudgetEntity(
                    cardId = id,
                    yearMonth = currentYearMonth(),
                    goalAmount = card.goalAmount
                )
            )
            hideAddDialog()
        }
    }

    fun updateCard(card: CardEntity) {
        viewModelScope.launch { cardRepo.updateCard(card) }
    }

    fun deleteCard(id: Long) {
        viewModelScope.launch { cardRepo.deleteCard(id) }
    }

    fun showAddDialog() = _uiState.update { it.copy(showAddDialog = true) }
    fun hideAddDialog() = _uiState.update { it.copy(showAddDialog = false) }
}
