package fr.enssat.sharemybook.mitosbooking.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val uid: String,
    val isbn: String,
    val title: String,
    val authors: String?,
    val covers: String?,
    val borrowerId: String?,
    val lenderId: String? = null // New field for borrowed books
)
