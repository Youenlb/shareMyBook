package fr.enssat.sharemybook.mitosbooking.ui.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookId: String = savedStateHandle.get<String>("bookId")!!
    private val actionString: String = savedStateHandle.get<String>("action")!!

    private val _action = MutableStateFlow(actionString)
    val action: StateFlow<String> = _action

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
            val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getString("user_id", null)
            if (userId != null) {
                try {
                    val user = bookRepository.getUserById(userId).first()

                    // Vérifier que l'utilisateur a complété son profil
                    if (user.fullName.isEmpty() || user.tel.isEmpty() || user.email.isEmpty()) {
                        val errorMsg = "Veuillez compléter votre profil (nom, téléphone, email) avant de faire une transaction"
                        _errorMessage.value = errorMsg
                        showToast(errorMsg)
                        Log.w("TransactionViewModel", "Profil utilisateur incomplet")
                        return@launch
                    }

                    val bookFromDb = bookRepository.getBookById(bookId).first()

                    val bookToSend = if (actionString == "RETURN") {
                        bookFromDb.copy(borrowerId = null) // Clear borrowerId for RETURN action
                    } else {
                        bookFromDb
                    }

                    val id = bookRepository.initTransaction(bookToSend, user, actionString)
                    _shareId.value = id
                    pollForResult(id)
                } catch (e: Exception) {
                    val errorMsg = "Erreur lors de l'initialisation de la transaction: ${e.localizedMessage}"
                    _errorMessage.value = errorMsg
                    showToast(errorMsg)
                    Log.e("TransactionViewModel", "Erreur initTransaction", e)
                }
            } else {
                val errorMsg = "User ID non trouvé. Veuillez vous connecter."
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

    private fun pollForResult(shareId: String) {
        viewModelScope.launch {
            var pollCount = 0
            val maxPollAttempts = 120 // Maximum 2 minutes (120 * 1 seconde)
            var initialBorrowerState: String? = null

            while (_transactionResult.value == null && pollCount < maxPollAttempts) {
                try {
                    delay(1000) // Attendre 1 seconde avant de vérifier
                    pollCount++

                    val result = bookRepository.resultTransaction(shareId)
                    val borrowerState = if (result.borrower != null) "non-null" else "null"

                    // Pour la première tentative, mémoriser l'état initial
                    if (pollCount == 1) {
                        initialBorrowerState = borrowerState
                        Log.d("TransactionViewModel", "Poll initial state - Action: $actionString - Borrower: $borrowerState")
                    }

                    Log.d("TransactionViewModel", "Poll attempt $pollCount - Action: $actionString - Borrower: $borrowerState (initial: $initialBorrowerState) - Result book borrowerId: ${result.book.borrowerId}")

                    if (actionString == "LOAN" && result.borrower != null) {
                        // LOAN: L'emprunteur a accepté et scanné le QR code
                        _transactionResult.value = result
                        val bookToUpdate = bookRepository.getBookById(result.book.uid).first()
                        bookRepository.updateBook(bookToUpdate.copy(borrowerId = result.borrower.uid))
                        bookRepository.insertUser(result.borrower)
                        Log.d("TransactionViewModel", "LOAN transaction accepted by borrower: ${result.borrower.fullName}")
                        break
                    } else if (actionString == "RETURN") {
                        // RETURN: Vérifier que l'emprunteur a vraiment scanné et accepté
                        // L'emprunteur appelle acceptTransaction() qui appelle bookRepository.acceptTransaction()
                        // Cela fait que result change (il y aura un changement d'état)
                        // Si borrower passe de "null" (initial) à "non-null" (après acceptation), alors c'est accepté
                        if (initialBorrowerState == "null" && borrowerState == "non-null") {
                            // L'emprunteur a accepté le retour !
                            _transactionResult.value = result
                            val bookToUpdate = bookRepository.getBookById(result.book.uid).first()
                            bookRepository.deleteBook(bookToUpdate)  // Supprimer le livre de l'emprunteur
                            Log.d("TransactionViewModel", "RETURN transaction confirmed by borrower: ${result.borrower?.fullName}")
                            break
                        }
                    }
                } catch (e: Exception) {
                    // Continuer le polling même en cas d'erreur (le résultat n'est peut-être pas prêt)
                    Log.d("TransactionViewModel", "Poll attempt $pollCount failed: ${e.localizedMessage}")
                }
            }

            if (pollCount >= maxPollAttempts && _transactionResult.value == null) {
                val errorMsg = "Délai d'attente dépassé. L'autre utilisateur n'a pas confirmé la transaction à temps."
                _errorMessage.value = errorMsg
                showToast(errorMsg)
                Log.w("TransactionViewModel", "Poll timeout after $maxPollAttempts attempts")
            }
        }
    }
}
