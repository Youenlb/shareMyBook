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
import fr.enssat.sharemybook.mitosbooking.data.remote.ShareIdQrCode
import fr.enssat.sharemybook.mitosbooking.ui.theme.MitosBookingTheme
import fr.enssat.sharemybook.mitosbooking.ui.viewmodel.TransactionViewModel
import net.glxn.qrgen.android.QRCode

@AndroidEntryPoint
class TransactionActivity : ComponentActivity() {
    private val viewModel: TransactionViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MitosBookingTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Transaction") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    TransactionScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionScreen(viewModel: TransactionViewModel, modifier: Modifier = Modifier) {
    val shareId by viewModel.shareId.collectAsState()
    val transactionResult by viewModel.transactionResult.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val action by viewModel.action.collectAsState()

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (errorMessage != null) {
            Text("Erreur: ${errorMessage}")
        } else if (transactionResult != null) {
            val successMessage = if (action == "RETURN") {
                "Retour terminé !"
            } else {
                "Transaction terminée !"
            }
            Text(successMessage)
            Text("Livre : ${transactionResult!!.book.title}")
            if (action == "RETURN") {
                Text("Livre retourné et disponible à nouveau")
            } else {
                Text("Prêté à : ${transactionResult!!.borrower?.fullName}")
            }
        } else if (shareId != null) {
            val json = Gson().toJson(ShareIdQrCode(shareId!!))
            val bitmap: Bitmap = QRCode.from(json).withSize(1024, 1024).bitmap()
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier.size(256.dp)
            )
            val waitingMessage = if (action == "RETURN") {
                "En attente du scan de l'emprunteur pour confirmer le retour..."
            } else {
                "En attente du scan de l'emprunteur..."
            }
            Text(waitingMessage)
        } else {
            Text("Génération de la transaction...")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionScreenPreview() {
    MitosBookingTheme {
        Text("Génération de la transaction...")
    }
}
