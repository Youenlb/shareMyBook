package fr.enssat.sharemybook.mitosbooking.data

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.enssat.sharemybook.mitosbooking.data.dao.BookDao
import fr.enssat.sharemybook.mitosbooking.data.dao.UserDao
import fr.enssat.sharemybook.mitosbooking.data.database.AppDatabase
import fr.enssat.sharemybook.mitosbooking.data.remote.OpenLibraryService
import fr.enssat.sharemybook.mitosbooking.data.remote.TransactionService
import fr.enssat.sharemybook.mitosbooking.data.repository.BookRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "sharemybook.db"
        )
        .fallbackToDestructiveMigration()
        .fallbackToDestructiveMigrationOnDowngrade()
        .build()
    }

    @Provides
    fun provideBookDao(appDatabase: AppDatabase): BookDao {
        return appDatabase.bookDao()
    }

    @Provides
    fun provideUserDao(appDatabase: AppDatabase): UserDao {
        return appDatabase.userDao()
    }

    @Provides
    @Singleton
    fun provideBookRepository(bookDao: BookDao, userDao: UserDao, openLibraryService: OpenLibraryService, transactionService: TransactionService): BookRepository {
        return BookRepository(bookDao, userDao, openLibraryService, transactionService)
    }

    @Provides
    @Singleton
    @Named("OpenLibrary")
    fun provideOpenLibraryRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://openlibrary.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenLibraryService(@Named("OpenLibrary") retrofit: Retrofit): OpenLibraryService {
        return retrofit.create(OpenLibraryService::class.java)
    }

    @Provides
    @Singleton
    @Named("Transaction")
    fun provideTransactionRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://europe-west9-mythic-cocoa-442917-i7.cloudfunctions.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTransactionService(@Named("Transaction") retrofit: Retrofit): TransactionService {
        return retrofit.create(TransactionService::class.java)
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    }
}
