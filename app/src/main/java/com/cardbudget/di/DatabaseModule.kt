package com.cardbudget.di

import android.content.Context
import androidx.room.Room
import com.cardbudget.data.dao.CardDao
import com.cardbudget.data.dao.MonthlyBudgetDao
import com.cardbudget.data.dao.TransactionDao
import com.cardbudget.data.db.CardBudgetDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CardBudgetDatabase =
        Room.databaseBuilder(
            context,
            CardBudgetDatabase::class.java,
            "card_budget.db"
        )
        .fallbackToDestructiveMigration()
        .build()

    @Provides fun provideCardDao(db: CardBudgetDatabase): CardDao = db.cardDao()
    @Provides fun provideTransactionDao(db: CardBudgetDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideBudgetDao(db: CardBudgetDatabase): MonthlyBudgetDao = db.monthlyBudgetDao()
}
