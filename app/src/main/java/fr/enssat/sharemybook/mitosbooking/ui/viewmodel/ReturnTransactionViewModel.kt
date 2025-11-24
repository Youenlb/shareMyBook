package fr.enssat.sharemybook.mitosbooking.ui.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.enssat.sharemybook.mitosbooking.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReturnTransactionViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _book = MutableStateFlow<fr.enssat.sharemybook.mitosbooking.data.entity.Book?>(null)
    val book: StateFlow<fr.enssat.sharemybook.mitosbooking.data.entity.Book?> = _book

    private val _transactionCompleted = MutableStateFlow(false)
    val transactionCompleted: StateFlow<Boolean> = _transactionCompleted

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private fun showToast(message: String) {
        viewModelScope.launch {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    fun initiateReturn(bookId: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            _isLoading.value = true
            try {
                val loadedBook = bookRepository.getBookById(bookId).first()
                _book.value = loadedBook
            } catch (e: Exception) {
                val errorMsg = "Erreur lors du chargement du livre: ${e.localizedMessage}"
                _errorMessage.value = errorMsg
                showToast(errorMsg)
                Log.e("ReturnTransactionViewModel", "Erreur initiateReturn", e)
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
                    val errorMsg = "Livre non trouvé"
                    _errorMessage.value = errorMsg
                    showToast(errorMsg)
                    return@launch
                }

                // Récupérer l'ID utilisateur via SharedPreferences
                val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val currentUserUid = sharedPreferences.getString("user_id", null)
                if (currentUserUid == null) {
                    val errorMsg = "User ID non trouvé."
                    _errorMessage.value = errorMsg
                    showToast(errorMsg)
                    return@launch
                }

                // Note: Ce ViewModel n'est plus utilisé dans la nouvelle architecture
                // qui utilise TransactionActivity avec AcceptTransactionViewModel pour les retours

                // Supprimer le livre de la base locale de l'emprunteur
                bookRepository.deleteBook(currentBook)

                _transactionCompleted.value = true
                showToast("Livre retourné avec succès!")
            } catch (e: Exception) {
                val errorMsg = "Erreur lors de la confirmation du retour: ${e.localizedMessage}"
                _errorMessage.value = errorMsg
                showToast(errorMsg)
                Log.e("ReturnTransactionViewModel", "Erreur confirmReturn", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
