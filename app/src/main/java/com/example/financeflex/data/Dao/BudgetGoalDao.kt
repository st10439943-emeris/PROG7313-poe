package com.example.financeflex.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.financeflex.data.entities.BudgetGoal
/*
 * BudgetGoalDao defines database operations related to the BudgetGoal entity.
 * DAO (Data Access Object) is a key component of the Room Persistence Library,
 * used to abstract database access and provide a clean API for data operations.
 * Source: Android Developers (2024) – Room Persistence Library
 */
@Dao
interface BudgetGoalDao {

    @Insert
    suspend fun insertBudgetGoal(budgetGoal: BudgetGoal)

    @Update
    suspend fun updateBudgetGoal(budgetGoal: BudgetGoal)

    @Query("SELECT * FROM budget_goals WHERE userId = :userId AND month = :month AND year = :year LIMIT 1")
    suspend fun getBudgetGoalForMonth(userId: Int, month: Int, year: Int): BudgetGoal?
}
