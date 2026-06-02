package com.example.financeflex.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.financeflex.data.entities.User
import androidx.room.Update

//DAO for managing User entities in the Room database.
//Reference: Android Developers (2024). Room Persistence Library Guide.

@Dao
interface UserDao {

    //Inserts a new user into the database.
    
    @Insert
    suspend fun insertUser(user: User)
    
//Authenticates a user by checking username and password, returns a User object if credentials match, otherwise null.

    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    suspend fun login(username: String, password: String): User?

    //Retrieves a user based on username, useful for validation during registration.

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?
    
//Retrieves a user based on email address, used to prevent duplicate registrations.
    
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?
    
//Retrieves a user by their unique ID, commonly used for profile viewing and updates.
    
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: Int): User?
    
//Updates an existing user record in the database.
    
    @Update
    suspend fun updateUser(user: User)
}
