package fr.enssat.sharemybook.mitosbooking.data.repository

import android.util.Log
import fr.enssat.sharemybook.mitosbooking.data.dao.BookDao
import fr.enssat.sharemybook.mitosbooking.data.dao.UserDao
import fr.enssat.sharemybook.mitosbooking.data.entity.Book
import fr.enssat.sharemybook.mitosbooking.data.entity.User
import fr.enssat.sharemybook.mitosbooking.data.remote.AcceptRequest
import fr.enssat.sharemybook.mitosbooking.data.remote.InitRequest
import fr.enssat.sharemybook.mitosbooking.data.remote.OpenLibraryService
import fr.enssat.sharemybook.mitosbooking.data.remote.TransactionData
import fr.enssat.sharemybook.mitosbooking.data.remote.TransactionService
import kotlinx.coroutines.flow.Flow

class BookRepository(
    private val bookDao: BookDao,
    private val userDao: UserDao,
    private val openLibraryService: OpenLibraryService,
    private val transactionService: TransactionService
) {

    fun getAllBooks(): Flow<List<Book>> = bookDao.getAllBooks()

    fun getBookById(bookId: String): Flow<Book> = bookDao.getBookById(bookId)

    suspend fun insertBook(book: Book) {
        bookDao.insertBook(book)
    }

    suspend fun updateBook(book: Book) {
        bookDao.updateBook(book)
    }

    suspend fun deleteBook(book: Book) {
        bookDao.deleteBook(book)
    }

    fun getUserById(userId: String): Flow<User> = userDao.getUserById(userId)

    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }

    suspend fun getBookDetailsFromApi(isbn: String): Book? {
        val response = openLibraryService.getBookByIsbn("ISBN:$isbn")
        val bookDetails = response["ISBN:$isbn"]

        return if (bookDetails != null) {
            Book(
                uid = isbn, // Using isbn as uid for simplicity
                isbn = isbn,
                title = bookDetails.title,
                authors = bookDetails.authors?.joinToString { it.name },
                covers = bookDetails.cover?.medium,
                borrowerId = null
            )
        } else {
            null
        }
    }

    suspend fun initTransaction(book: Book, owner: User, action: String): String {
        val request = InitRequest(action, book, owner)
        Log.d("BookRepository", "initTransaction - Sending request: action=$action, book=$book, owner=$owner")
        val response = transactionService.init(request)
        Log.d("BookRepository", "initTransaction - Received response: ${response.shareId}")
        return response.shareId
    }

    suspend fun acceptTransaction(shareId: String, borrower: User): TransactionData {
        val request = AcceptRequest(borrower)
        Log.d("BookRepository", "acceptTransaction - Sending request: shareId=$shareId, borrower=$borrower")
        val result = transactionService.accept(shareId, request)
        Log.d("BookRepository", "acceptTransaction - Received result: $result")
        return result
    }

    suspend fun resultTransaction(shareId: String): TransactionData {
        try {
            val result = transactionService.result(shareId)
            Log.d("BookRepository", "resultTransaction success: $result")
            return result
        } catch (e: Exception) {
            Log.e("BookRepository", "resultTransaction failed", e)
            throw e
        }
    }
}
