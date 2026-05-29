package com.cardbudget.data.dao

import androidx.room.*
import com.cardbudget.data.entity.CardEntity
import com.cardbudget.data.entity.MonthlyBudgetEntity
import com.cardbudget.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

// ─── CardDao ───────────────────────────────────────────────
@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE isActive = 1 ORDER BY createdAt ASC")
    fun getAllActiveCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCardById(id: Long): CardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long

    @Update
    suspend fun updateCard(card: CardEntity)

    @Query("UPDATE cards SET isActive = 0 WHERE id = :id")
    suspend fun softDeleteCard(id: Long)

    @Query("SELECT COUNT(*) FROM cards WHERE isActive = 1")
    suspend fun getActiveCardCount(): Int
}

// ─── TransactionDao ────────────────────────────────────────
@Dao
interface TransactionDao {
    @Query("""
        SELECT * FROM transactions 
        WHERE billingMonth = :yearMonth 
        ORDER BY transactionDate DESC
    """)
    fun getTransactionsByMonth(yearMonth: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE cardId = :cardId AND billingMonth = :yearMonth
        ORDER BY transactionDate DESC
    """)
    fun getTransactionsByCardAndMonth(cardId: Long, yearMonth: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE billingMonth = :yearMonth
    """)
    fun getTotalAmountByMonth(yearMonth: String): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE cardId = :cardId AND billingMonth = :yearMonth
    """)
    fun getTotalAmountByCardAndMonth(cardId: Long, yearMonth: String): Flow<Long>

    @Query("""
        SELECT * FROM transactions
        WHERE billingMonth = :yearMonth
        ORDER BY transactionDate DESC
        LIMIT :limit
    """)
    fun getRecentTransactions(yearMonth: String, limit: Int = 20): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("""
        SELECT * FROM transactions
        WHERE merchantName = :merchant AND amount = :amount 
          AND transactionDate BETWEEN :from AND :to
        LIMIT 1
    """)
    suspend fun findDuplicate(merchant: String, amount: Long, from: Long, to: Long): TransactionEntity?

    @Query("""
        SELECT category, SUM(amount) as total FROM transactions
        WHERE billingMonth = :yearMonth
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getCategoryBreakdown(yearMonth: String): Flow<List<CategoryTotal>>

    @Query("""
        SELECT billingMonth, SUM(amount) as total FROM transactions
        GROUP BY billingMonth
        ORDER BY billingMonth DESC
        LIMIT 6
    """)
    fun getMonthlyTotals(): Flow<List<MonthlyTotal>>
}

data class CategoryTotal(val category: String, val total: Long)
data class MonthlyTotal(val billingMonth: String, val total: Long)

// ─── MonthlyBudgetDao ──────────────────────────────────────
@Dao
interface MonthlyBudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE yearMonth = :yearMonth")
    fun getBudgetsByMonth(yearMonth: String): Flow<List<MonthlyBudgetEntity>>

    @Query("SELECT * FROM monthly_budgets WHERE cardId IS NULL AND yearMonth = :yearMonth LIMIT 1")
    suspend fun getTotalBudget(yearMonth: String): MonthlyBudgetEntity?

    @Query("SELECT * FROM monthly_budgets WHERE cardId = :cardId AND yearMonth = :yearMonth LIMIT 1")
    suspend fun getCardBudget(cardId: Long, yearMonth: String): MonthlyBudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budget: MonthlyBudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: MonthlyBudgetEntity)
}
