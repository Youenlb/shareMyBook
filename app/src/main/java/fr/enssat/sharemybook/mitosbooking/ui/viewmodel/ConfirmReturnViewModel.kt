package fr.enssat.sharemybook.mitosbooking.ui.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.enssat.sharemybook.mitosbooking.data.entity.Book
import fr.enssat.sharemybook.mitosbooking.data.entity.User
import fr.enssat.sharemybook.mitosbooking.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfirmReturnViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book

    private val _borrower = MutableStateFlow<User?>(null)
    val borrower: StateFlow<User?> = _borrower

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _returnConfirmed = MutableStateFlow(false)
    val returnConfirmed: StateFlow<Boolean> = _returnConfirmed


    fun loadReturnData(bookUid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val bookFromDb = bookRepository.getBookById(bookUid).first()
                _book.value = bookFromDb

                // Récupérer l'emprunteur depuis le borrowerId du livre
                if (bookFromDb.borrowerId != null) {
                    val borrowerFromDb = bookRepository.getUserById(bookFromDb.borrowerId).first()
                    _borrower.value = borrowerFromDb

                    // Lancer le polling pour détecter quand l'emprunteur confirme le retour
                    pollForReturnConfirmation(bookUid)
                } else {
                    val errorMsg = "Ce livre n'a pas d'emprunteur"
                    _errorMessage.value = errorMsg
                    showToast(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Erreur lors du chargement des données de retour: ${e.localizedMessage}"
                _errorMessage.value = errorMsg
                showToast(errorMsg)
                Log.e("ConfirmReturnViewModel", "Erreur loadReturnData", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun pollForReturnConfirmation(bookUid: String) {
        viewModelScope.launch {
            var pollCount = 0
            val maxPollAttempts = 60 // Maximum 2 minutes (60 * 2 secondes)

            while (!_returnConfirmed.value && pollCount < maxPollAttempts) {
                try {
                    kotlinx.coroutines.delay(2000) // Attendre 2 secondes entre chaque vérification
                    pollCount++

                    // Note: Ce ViewModel n'est plus utilisé dans la nouvelle architecture
                    // qui utilise TransactionActivity pour les retours
                    // Vérifier la base locale (pour compatibilité)
                    val updatedBook = bookRepository.getBookById(bookUid).first()

                    // Si le livre n'a plus de borrower, c'est qu'il a été retourné
                    if (updatedBook.borrowerId == null) {
                        // Le livre a été retourné !
                        _book.value = updatedBook
                        _returnConfirmed.value = true
                        showToast("Livre retourné avec succès")
                        Log.d("ConfirmReturnViewModel", "Livre retourné avec succès")
                        break
                    }
                } catch (e: Exception) {
                    Log.e("ConfirmReturnViewModel", "Erreur lors du polling", e)
                    // Continue le polling même en cas d'erreur
                }
            }

            if (pollCount >= maxPollAttempts && !_returnConfirmed.value) {
                val errorMsg = "Délai d'attente dépassé. Veuillez réessayer."
                _errorMessage.value = errorMsg
                showToast(errorMsg)
            }
        }
    }

    private fun showToast(message: String) {
        viewModelScope.launch {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
