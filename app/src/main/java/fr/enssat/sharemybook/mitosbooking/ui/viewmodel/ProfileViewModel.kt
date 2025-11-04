package fr.enssat.sharemybook.mitosbooking.ui.viewmodel

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.enssat.sharemybook.mitosbooking.data.entity.User
import fr.enssat.sharemybook.mitosbooking.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            var userId = sharedPreferences.getString("user_id", null)

            if (userId == null) {
                // Créer un nouvel utilisateur
                userId = UUID.randomUUID().toString()
                sharedPreferences.edit { putString("user_id", userId) }
            }

            // Toujours charger depuis la base
            bookRepository.getUserById(userId).collect { user ->
                _user.value = user
            }
        }
    }

    fun saveUser(fullName: String, tel: String, email: String) {
        viewModelScope.launch {
            val userId = sharedPreferences.getString("user_id", null)
            if (userId != null) {
                val updatedUser = User(userId, fullName, tel, email)
                // Utiliser insertUser avec REPLACE strategy pour insert ou update
                bookRepository.insertUser(updatedUser)
            }
        }
    }
}
