package fr.enssat.sharemybook.mitosbooking.ui.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val sharedPreferences: SharedPreferences,
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
                _errorMessage.value = "Erreur lors du chargement de la transaction: ${e.localizedMessage}"
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
                val userId = sharedPreferences.getString("user_id", null)
                if (userId != null) {
                    val user = bookRepository.getUserById(userId).first()
                    val transaction = bookRepository.acceptTransaction(shareId, user)
                    _transactionData.value = transaction
                    _transactionAccepted.value = true

                    // Add book and owner to local database
                    // borrowerId = moi (celui qui emprunte)
                    // lenderId = le propriétaire du livre
                    bookRepository.insertBook(transaction.book.copy(
                        borrowerId = userId,
                        lenderId = transaction.owner.uid
                    ))
                    bookRepository.insertUser(transaction.owner)
                } else {
                    _errorMessage.value = "User ID non trouvé."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erreur lors de l'acceptation de la transaction: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
