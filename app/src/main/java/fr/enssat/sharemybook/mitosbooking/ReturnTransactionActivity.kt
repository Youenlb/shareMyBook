package fr.enssat.sharemybook.mitosbooking

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import fr.enssat.sharemybook.mitosbooking.data.remote.ReturnQrCode
import fr.enssat.sharemybook.mitosbooking.ui.theme.MitosBookingTheme
import fr.enssat.sharemybook.mitosbooking.ui.viewmodel.ReturnTransactionViewModel
import net.glxn.qrgen.android.QRCode

@AndroidEntryPoint
class ReturnTransactionActivity : ComponentActivity() {
    private val viewModel: ReturnTransactionViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bookId = intent.getStringExtra("bookId")
        if (bookId != null) {
            viewModel.initiateReturn(bookId)
        } else {
            finish()
            return
        }

        setContent {
            MitosBookingTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Retourner un livre") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    ReturnTransactionScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ReturnTransactionScreen(viewModel: ReturnTransactionViewModel, modifier: Modifier = Modifier) {
    val book by viewModel.book.collectAsState()
    val transactionCompleted by viewModel.transactionCompleted.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (errorMessage != null) {
            Text("Erreur: $errorMessage")
        } else if (transactionCompleted) {
            Text("Livre retourné avec succès !")
        } else if (book != null) {
            Text("Confirmer le retour du livre:")
            Text(book!!.title, modifier = Modifier.padding(top = 8.dp))

            androidx.compose.material3.Button(
                onClick = { viewModel.confirmReturn() },
                enabled = !isLoading,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Confirmer le retour")
            }
        } else {
            Text("Préparation du retour...")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReturnTransactionScreenPreview() {
    MitosBookingTheme {
        Text("Préparation du retour...")
    }
}
