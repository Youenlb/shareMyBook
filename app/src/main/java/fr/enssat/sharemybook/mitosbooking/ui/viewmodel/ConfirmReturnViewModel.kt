package fr.enssat.sharemybook.mitosbooking.ui.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val sharedPreferences: SharedPreferences
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
                    _errorMessage.value = "Ce livre n'a pas d'emprunteur"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erreur lors du chargement des données de retour: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun pollForReturnConfirmation(bookUid: String) {
        viewModelScope.launch {
            while (!_returnConfirmed.value) {
                try {
                    kotlinx.coroutines.delay(2000) // Attendre 2 secondes entre chaque vérification

                    // Vérifier si le livre a été mis à jour (borrowerId == null signifie retourné)
                    val updatedBook = bookRepository.getBookById(bookUid).first()

                    if (updatedBook.borrowerId == null) {
                        // L'emprunteur a confirmé le retour !
                        // On efface aussi le lenderId pour rendre le livre disponible
                        val availableBook = updatedBook.copy(lenderId = null)
                        bookRepository.updateBook(availableBook)
                        _returnConfirmed.value = true
                        break
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Erreur lors de la vérification du retour: ${e.localizedMessage}"
                    break
                }
            }
        }
    }
}
