package fr.enssat.sharemybook.mitosbooking.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import fr.enssat.sharemybook.mitosbooking.data.dao.BookDao
import fr.enssat.sharemybook.mitosbooking.data.dao.UserDao
import fr.enssat.sharemybook.mitosbooking.data.entity.Book
import fr.enssat.sharemybook.mitosbooking.data.entity.User

@Database(entities = [Book::class, User::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun userDao(): UserDao
}
