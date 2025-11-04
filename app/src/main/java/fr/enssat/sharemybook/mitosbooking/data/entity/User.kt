package fr.enssat.sharemybook.mitosbooking.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val uid: String,
    val fullName: String,
    val tel: String,
    val email: String
)
