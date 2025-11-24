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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AcceptTransactionViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val shareId: String = savedStateHandle.get<String>("shareId")!!

    private val _transactionData = MutableStateFlow<TransactionData?>(null)
    val transactionData: StateFlow<TransactionData?> = _transactionData

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _transactionAccepted = MutableStateFlow(false)
    val transactionAccepted: StateFlow<Boolean> = _transactionAccepted

    init {
        loadTransaction(shareId)
    }

    fun loadTransaction(shareId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = bookRepository.resultTransaction(shareId)
                _transactionData.value = result
            } catch (e: Exception) {
                val errorMsg = "Erreur lors du chargement de la transaction: ${e.localizedMessage}"
                _errorMessage.value = errorMsg
                showToast(errorMsg)
                Log.e("AcceptTransactionViewModel", "Erreur loadTransaction", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun acceptTransaction() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val userId = sharedPreferences.getString("user_id", null)
                if (userId != null) {
                    val user = bookRepository.getUserById(userId).first()

                    // Vérifier que l'utilisateur a complété son profil
                    if (user.fullName.isEmpty() || user.tel.isEmpty() || user.email.isEmpty()) {
                        val errorMsg = "Veuillez compléter votre profil (nom, téléphone, email) avant d'accepter une transaction"
                        _errorMessage.value = errorMsg
                        showToast(errorMsg)
                        Log.w("AcceptTransactionViewModel", "Profil utilisateur incomplet")
                        _isLoading.value = false
                        return@launch
                    }

                    val transaction = bookRepository.acceptTransaction(shareId, user)
                    _transactionData.value = transaction
                    _transactionAccepted.value = true
                    showToast("Transaction acceptée!")

                    if (transaction.action == "LOAN") {
                        // LOAN: Add book and owner to local database
                        // borrowerId = moi (celui qui emprunte)
                        // lenderId = le propriétaire du livre
                        bookRepository.insertBook(transaction.book.copy(
                            borrowerId = userId,
                            lenderId = transaction.owner.uid
                        ))
                        bookRepository.insertUser(transaction.owner)
                    } else if (transaction.action == "RETURN") {
                        // RETURN: Delete book from borrower's local database
                        // Le livre appartient au prêteur, l'emprunteur le supprime
                        try {
                            val localBook = bookRepository.getBookById(transaction.book.uid).first()
                            bookRepository.deleteBook(localBook)
                        } catch (e: Exception) {
                            // Book might not exist locally, that's OK
                            Log.d("AcceptTransactionViewModel", "Book not found locally for deletion: ${e.localizedMessage}")
                        }
                    }
                } else {
                    val errorMsg = "User ID non trouvé."
                    _errorMessage.value = errorMsg
                    showToast(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Erreur lors de l'acceptation de la transaction: ${e.localizedMessage}"
                _errorMessage.value = errorMsg
                showToast(errorMsg)
                Log.e("AcceptTransactionViewModel", "Erreur acceptTransaction", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun showToast(message: String) {
        viewModelScope.launch {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
