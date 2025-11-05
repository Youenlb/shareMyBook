package fr.enssat.sharemybook.mitosbooking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import fr.enssat.sharemybook.mitosbooking.ui.theme.MitosBookingTheme
import fr.enssat.sharemybook.mitosbooking.ui.viewmodel.AcceptTransactionViewModel

@AndroidEntryPoint
class AcceptTransactionActivity : ComponentActivity() {

    private val viewModel: AcceptTransactionViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shareId = intent.getStringExtra("shareId")
        if (shareId != null) {
            viewModel.loadTransaction(shareId)
        } else {
            // The previous Toast message was removed because it was redundant with the errorMessage state.
            finish()
            return
        }

        setContent {
            MitosBookingTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Accepter la transaction") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    AcceptTransactionScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun AcceptTransactionScreen(viewModel: AcceptTransactionViewModel, modifier: Modifier = Modifier) {
    val transactionData by viewModel.transactionData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val transactionAccepted by viewModel.transactionAccepted.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            Text("Chargement en cours...")
        } else if (errorMessage != null) {
            Text("Erreur: ${errorMessage}")
        } else if (transactionAccepted) {
            val message = if (transactionData?.action == "RETURN") {
                "Retour confirmé avec succès !"
            } else {
                "Transaction acceptée avec succès !"
            }
            Text(message)
        } else if (transactionData != null) {
            val book = transactionData!!.book
            val owner = transactionData!!.owner
            val action = transactionData!!.action

            Text("Livre : ${book.title}")
            Text("Propriétaire : ${owner.fullName}")
            Text("ISBN : ${book.isbn}")

            val buttonText = if (action == "RETURN") {
                "Confirmer le retour"
            } else {
                "Confirmer l'emprunt"
            }

            Button(
                onClick = { viewModel.acceptTransaction() },
                modifier = Modifier.padding(top = 16.dp),
                enabled = !isLoading
            ) {
                Text(buttonText)
            }
        } else {
            Text("Initialisation de la transaction...")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AcceptTransactionScreenPreview() {
    MitosBookingTheme {
        Text("Initialisation de la transaction...")
    }
}
