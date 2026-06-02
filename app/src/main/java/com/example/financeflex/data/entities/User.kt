package com.example.financeflex.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity class representing a User in the FinanceFlex application.
 * This table stores all registered user information required for authentication
 * and profile management within the system.
 */

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val userId: String = "",
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val phone: String = "",
    val username: String = "",
    val password: String = ""
)