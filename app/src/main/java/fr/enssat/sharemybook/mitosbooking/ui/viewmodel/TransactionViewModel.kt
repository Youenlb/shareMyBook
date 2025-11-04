package fr.enssat.sharemybook.mitosbooking.ui.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.enssat.sharemybook.mitosbooking.data.remote.TransactionData
import fr.enssat.sharemybook.mitosbooking.data.repository.BookRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val sharedPreferences: SharedPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookId: String = savedStateHandle.get<String>("bookId")!!
    private val action: String = savedStateHandle.get<String>("action")!!

    private val _shareId = MutableStateFlow<String?>(null)
    val shareId: StateFlow<String?> = _shareId

    private val _transactionResult = MutableStateFlow<TransactionData?>(null)
    val transactionResult: StateFlow<TransactionData?> = _transactionResult

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        initTransaction()
    }

    private fun initTransaction() {
        viewModelScope.launch {
            val userId = sharedPreferences.getString("user_id", null)
            if (userId != null) {
                try {
                    val user = bookRepository.getUserById(userId).first()
                    if (user == null) {
                        _errorMessage.value = "Profil utilisateur non trouvé. Veuillez configurer votre profil."
                        return@launch
                    }

                    val bookFromDb = bookRepository.getBookById(bookId).first()

                    val bookToSend = if (action == "RETURN") {
                        bookFromDb.copy(borrowerId = null) // Clear borrowerId for RETURN action
                    } else {
                        bookFromDb
                    }

                    val id = bookRepository.initTransaction(bookToSend, user, action)
                    _shareId.value = id
                    pollForResult(id)
                } catch (e: Exception) {
                    _errorMessage.value = "Erreur lors de l'initialisation de la transaction: ${e.localizedMessage}"
                }
            } else {
                _errorMessage.value = "User ID non trouvé. Veuillez vous connecter."
            }
        }
    }

    private fun pollForResult(shareId: String) {
        viewModelScope.launch {
            while (_transactionResult.value == null) {
                try {
                    val result = bookRepository.resultTransaction(shareId)
                    if (result.borrower != null) {
                        // LOAN: Borrower accepted, update local book and add borrower user
                        _transactionResult.value = result
                        val bookToUpdate = bookRepository.getBookById(result.book.uid).first()
                        bookRepository.updateBook(bookToUpdate.copy(borrowerId = result.borrower.uid))
                        bookRepository.insertUser(result.borrower)
                        break
                    } else if (action == "RETURN" && result.book.borrowerId == null) {
                        // RETURN: Backend confirmed return, clear borrowerId locally
                        _transactionResult.value = result
                        val bookToUpdate = bookRepository.getBookById(result.book.uid).first()
                        bookRepository.updateBook(bookToUpdate.copy(borrowerId = null))
                        // Optionally remove borrower user if no longer needed - for now, keep it.
                        break
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Erreur lors de la récupération du résultat: ${e.localizedMessage}"
                }
                delay(1000) // Poll every second
            }
        }
    }
}
