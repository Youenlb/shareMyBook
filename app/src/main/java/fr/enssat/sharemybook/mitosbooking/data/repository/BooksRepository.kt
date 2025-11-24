package fr.enssat.sharemybook.mitosbooking.data.repository

import fr.enssat.sharemybook.mitosbooking.data.entity.Book
import fr.enssat.sharemybook.mitosbooking.data.entity.User
import fr.enssat.sharemybook.mitosbooking.data.remote.TransactionData
import kotlinx.coroutines.flow.Flow

/**
 * Repository that provides insert, update, delete, and retrieve of [Book] and [User] from data sources.
 * Follows the Repository pattern from TP4.
 */
interface BooksRepository {
    /**
     * Retrieve all books from the database
     */
    fun getAllBooksStream(): Flow<List<Book>>

    /**
     * Retrieve a book by its ID from the database
     */
    fun getBookStream(bookId: String): Flow<Book?>

    /**
     * Insert a book in the database
     */
    suspend fun insertBook(book: Book)

    /**
     * Update a book in the database
     */
    suspend fun updateBook(book: Book)

    /**
     * Delete a book from the database
     */
    suspend fun deleteBook(book: Book)

    /**
     * Get a user by ID from the database
     */
    fun getUserStream(userId: String): Flow<User?>

    /**
     * Insert a user in the database
     */
    suspend fun insertUser(user: User)

    /**
     * Update a user in the database
     */
    suspend fun updateUser(user: User)

    /**
     * Delete a user from the database
     */
    suspend fun deleteUser(user: User)

    /**
     * Fetch book details from OpenLibrary API
     */
    suspend fun fetchBookFromApi(isbn: String): Book?

    /**
     * Initialize a transaction (loan or return)
     */
    suspend fun initTransaction(book: Book, owner: User, action: String): String

    /**
     * Accept a transaction
     */
    suspend fun acceptTransaction(shareId: String, borrower: User): TransactionData

    /**
     * Get transaction result
     */
    suspend fun getTransactionResult(shareId: String): TransactionData
}

