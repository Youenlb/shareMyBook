package fr.enssat.sharemybook.mitosbooking.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.enssat.sharemybook.mitosbooking.data.entity.Book
import fr.enssat.sharemybook.mitosbooking.data.entity.User
import fr.enssat.sharemybook.mitosbooking.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MyLibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val sharedPreferences: SharedPreferences,
    private val application: Application
) : ViewModel() {

    private val currentUserId: String = sharedPreferences.getString("user_id", null)
        ?: UUID.randomUUID().toString().also { sharedPreferences.edit().putString("user_id", it).apply() }

    private val _toastMessages = MutableSharedFlow<String>()
    val toastMessages = _toastMessages.asSharedFlow()

    val books: StateFlow<List<Book>> = bookRepository.getAllBooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Livres que je possède (propriétaire ou lenderId == null)
    val myOwnedBooks = books.map { allBooks ->
        allBooks.filter {
            (it.lenderId == null || it.lenderId == currentUserId) && it.borrowerId == null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Livres que j'ai prêtés à quelqu'un (je suis le lenderId et il y a un borrowerId)
    val myLoans = books.map { allBooks ->
        allBooks.filter {
            (it.lenderId == null || it.lenderId == currentUserId) && it.borrowerId != null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Livres que j'ai empruntés (je suis le borrowerId)
    val myBorrows = books.map { allBooks ->
        allBooks.filter {
            it.borrowerId == currentUserId
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            // Ensure a user exists on first launch
            if (bookRepository.getAllBooks().first().isEmpty()) {
                prepopulateDatabase()
            }
        }
    }

    private suspend fun prepopulateDatabase() {
        val booksToInsert = listOf(
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0321721349", title = "The Lord of the Rings", authors = "J.R.R. Tolkien", covers = "https://covers.openlibrary.org/b/id/8264423-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0743273565", title = "The Hitchhiker's Guide to the Galaxy", authors = "Douglas Adams", covers = "https://covers.openlibrary.org/b/id/9144214-L.jpg", borrowerId = null, lenderId = null),
            Book(uid = UUID.randomUUID().toString(), isbn = "978-0439064873", title = "Harry Potter and the Chamber of Secrets", authors = "J.K. Rowling", covers = "https://covers.openlibrary.org/b/id/8773826-L.jpg", borrowerId = null, lenderId = null)
        )
        booksToInsert.forEach { bookRepository.insertBook(it) }
    }

    fun onIsbnScanned(isbn: String) {
        viewModelScope.launch {
            try {
                val book = bookRepository.getBookDetailsFromApi(isbn)
                if (book != null) {
                    bookRepository.insertBook(book.copy(lenderId = null)) // Ensure newly added books are not marked as borrowed
                    _toastMessages.emit("Livre ajouté à votre bibliothèque !")
                } else {
                    _toastMessages.emit("Livre non trouvé.")
                }
            } catch (e: Exception) {
                _toastMessages.emit("Erreur lors de la récupération du livre.")
            }
        }
    }

    // Helper function to get a user by ID and handle potential null values
    suspend fun getUserFullName(userId: String?): String {
        return if (userId != null) {
            try {
                bookRepository.getUserById(userId).first().fullName
            } catch (e: Exception) {
                "Utilisateur inconnu"
            }
        } else {
            ""
        }
    }
}
