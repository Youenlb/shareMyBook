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
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import net.glxn.qrgen.android.QRCode
import dagger.hilt.android.AndroidEntryPoint
import fr.enssat.sharemybook.mitosbooking.data.remote.ReturnQrCode
import fr.enssat.sharemybook.mitosbooking.ui.theme.MitosBookingTheme
import fr.enssat.sharemybook.mitosbooking.ui.viewmodel.ConfirmReturnViewModel

@AndroidEntryPoint
class ConfirmReturnActivity : ComponentActivity() {
    private val viewModel: ConfirmReturnViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bookUid = intent.getStringExtra("bookUid")

        if (bookUid != null) {
            viewModel.loadReturnData(bookUid)
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
                            title = { Text("Confirmer le retour") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    ConfirmReturnScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ConfirmReturnScreen(viewModel: ConfirmReturnViewModel, modifier: Modifier = Modifier) {
    val book by viewModel.book.collectAsState()
    val borrower by viewModel.borrower.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val returnConfirmed by viewModel.returnConfirmed.collectAsState()

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            Text("Chargement des données de retour...")
        } else if (errorMessage != null) {
            Text("Erreur: ${errorMessage}")
        } else if (returnConfirmed) {
            Text("Retour du livre confirmé avec succès !")
            Text("Le livre est de nouveau disponible dans votre bibliothèque.", modifier = Modifier.padding(top = 8.dp))
        } else if (book != null && borrower != null) {
            Text("Livre à retourner: ${book!!.title}")
            Text("Emprunteur: ${borrower!!.fullName}")
            Text("Montrez ce QR code à l'emprunteur", modifier = Modifier.padding(top = 16.dp))

            // Générer le QR code avec les informations de retour
            val returnQrCode = ReturnQrCode(
                bookUid = book!!.uid,
                lenderUid = borrower!!.uid
            )
            val gson = Gson()
            val qrCodeJson = gson.toJson(returnQrCode)

            val qrCodeBitmap: Bitmap = QRCode.from(qrCodeJson).withSize(512, 512).bitmap()

            Image(
                bitmap = qrCodeBitmap.asImageBitmap(),
                contentDescription = "QR Code de retour",
                modifier = Modifier
                    .size(300.dp)
                    .padding(16.dp)
            )

            Text(
                "En attente que l'emprunteur scanne et confirme...",
                modifier = Modifier.padding(top = 16.dp)
            )
            Text("Le retour sera confirmé automatiquement.")
        } else {
            Text("En attente des données de retour...")
        }
    }
}

