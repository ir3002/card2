package com.cardbudget.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.cardbudget.data.dao.CardDao
import com.cardbudget.data.dao.MonthlyBudgetDao
import com.cardbudget.data.dao.TransactionDao
import com.cardbudget.data.entity.*

class Converters {
    @TypeConverter fun fromCardIssuer(v: CardIssuer) = v.name
    @TypeConverter fun toCardIssuer(v: String) = CardIssuer.valueOf(v)
    @TypeConverter fun fromSource(v: TransactionSource) = v.name
    @TypeConverter fun toSource(v: String) = TransactionSource.valueOf(v)
    @TypeConverter fun fromCategory(v: TransactionCategory) = v.name
    @TypeConverter fun toCategory(v: String) = TransactionCategory.valueOf(v)
}

@Database(
    entities = [CardEntity::class, TransactionEntity::class, MonthlyBudgetEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class CardBudgetDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun transactionDao(): TransactionDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
}
