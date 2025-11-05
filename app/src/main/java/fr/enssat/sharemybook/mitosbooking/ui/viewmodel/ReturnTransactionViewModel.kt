package fr.enssat.sharemybook.mitosbooking.ui.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.enssat.sharemybook.mitosbooking.data.remote.ReturnQrCode
import fr.enssat.sharemybook.mitosbooking.data.repository.BookRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReturnTransactionViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _book = MutableStateFlow<fr.enssat.sharemybook.mitosbooking.data.entity.Book?>(null)
    val book: StateFlow<fr.enssat.sharemybook.mitosbooking.data.entity.Book?> = _book

    private val _transactionCompleted = MutableStateFlow(false)
    val transactionCompleted: StateFlow<Boolean> = _transactionCompleted

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun initiateReturn(bookId: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            _isLoading.value = true
            try {
                val loadedBook = bookRepository.getBookById(bookId).first()
                _book.value = loadedBook
            } catch (e: Exception) {
                _errorMessage.value = "Erreur lors du chargement du livre: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun confirmReturn() {
        viewModelScope.launch {
            _errorMessage.value = null
            _isLoading.value = true
            try {
                val currentBook = _book.value
                if (currentBook == null) {
                    _errorMessage.value = "Livre non trouvé"
                    return@launch
                }

                val currentUserUid = sharedPreferences.getString("user_id", null)
                if (currentUserUid == null) {
                    _errorMessage.value = "User ID non trouvé."
                    return@launch
                }

                // Note: Ce ViewModel n'est plus utilisé dans la nouvelle architecture
                // qui utilise TransactionActivity avec AcceptTransactionViewModel pour les retours

                // Supprimer le livre de la base locale de l'emprunteur
                bookRepository.deleteBook(currentBook)

                _transactionCompleted.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Erreur lors de la confirmation du retour: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
