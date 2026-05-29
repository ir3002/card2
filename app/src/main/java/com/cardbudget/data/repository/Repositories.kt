package com.cardbudget.data.repository

import com.cardbudget.data.dao.CardDao
import com.cardbudget.data.dao.CategoryTotal
import com.cardbudget.data.dao.MonthlyBudgetDao
import com.cardbudget.data.dao.MonthlyTotal
import com.cardbudget.data.dao.TransactionDao
import com.cardbudget.data.entity.CardEntity
import com.cardbudget.data.entity.MonthlyBudgetEntity
import com.cardbudget.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

// ─── CardRepository ────────────────────────────────────────
@Singleton
class CardRepository @Inject constructor(private val cardDao: CardDao) {

    fun getAllActiveCards(): Flow<List<CardEntity>> = cardDao.getAllActiveCards()

    suspend fun getAllActiveCardsOnce(): List<CardEntity> =
        cardDao.getAllActiveCards().first()

    suspend fun getCardById(id: Long): CardEntity? = cardDao.getCardById(id)

    suspend fun insertCard(card: CardEntity): Long = cardDao.insertCard(card)

    suspend fun updateCard(card: CardEntity) = cardDao.updateCard(card)

    suspend fun deleteCard(id: Long) = cardDao.softDeleteCard(id)
}

// ─── TransactionRepository ────────────────────────────────
@Singleton
class TransactionRepository @Inject constructor(private val dao: TransactionDao) {

    fun getTransactionsByMonth(yearMonth: String): Flow<List<TransactionEntity>> =
        dao.getTransactionsByMonth(yearMonth)

    fun getTransactionsByCardAndMonth(cardId: Long, yearMonth: String): Flow<List<TransactionEntity>> =
        dao.getTransactionsByCardAndMonth(cardId, yearMonth)

    fun getTotalAmountByMonth(yearMonth: String): Flow<Long> =
        dao.getTotalAmountByMonth(yearMonth)

    fun getTotalAmountByCardAndMonth(cardId: Long, yearMonth: String): Flow<Long> =
        dao.getTotalAmountByCardAndMonth(cardId, yearMonth)

    fun getRecentTransactions(yearMonth: String, limit: Int = 20): Flow<List<TransactionEntity>> =
        dao.getRecentTransactions(yearMonth, limit)

    fun getCategoryBreakdown(yearMonth: String): Flow<List<CategoryTotal>> =
        dao.getCategoryBreakdown(yearMonth)

    fun getMonthlyTotals(): Flow<List<MonthlyTotal>> =
        dao.getMonthlyTotals()

    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        dao.insertTransaction(transaction)

    suspend fun insertTransactions(transactions: List<TransactionEntity>) =
        dao.insertTransactions(transactions)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        dao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        dao.deleteTransaction(transaction)

    suspend fun findDuplicate(merchantName: String, amount: Long, from: Long, to: Long): TransactionEntity? =
        dao.findDuplicate(merchantName, amount, from, to)
}

// ─── BudgetRepository ─────────────────────────────────────
@Singleton
class BudgetRepository @Inject constructor(private val dao: MonthlyBudgetDao) {

    fun getBudgetsByMonth(yearMonth: String): Flow<List<MonthlyBudgetEntity>> =
        dao.getBudgetsByMonth(yearMonth)

    suspend fun getTotalBudget(yearMonth: String): MonthlyBudgetEntity? =
        dao.getTotalBudget(yearMonth)

    suspend fun getCardBudget(cardId: Long, yearMonth: String): MonthlyBudgetEntity? =
        dao.getCardBudget(cardId, yearMonth)

    suspend fun upsertBudget(budget: MonthlyBudgetEntity) =
        dao.insertOrUpdate(budget)

    suspend fun deleteBudget(budget: MonthlyBudgetEntity) =
        dao.deleteBudget(budget)
}
