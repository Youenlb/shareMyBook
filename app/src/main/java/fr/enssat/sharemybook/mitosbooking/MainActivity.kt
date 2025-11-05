package fr.enssat.sharemybook.mitosbooking

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import dagger.hilt.android.AndroidEntryPoint
import fr.enssat.sharemybook.mitosbooking.data.entity.Book
import fr.enssat.sharemybook.mitosbooking.ui.theme.MitosBookingTheme
import fr.enssat.sharemybook.mitosbooking.ui.viewmodel.MyLibraryViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MyLibraryViewModel by viewModels()

    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val barcode = result.data?.getStringExtra("scanned_barcode")
            if (barcode != null) {
                viewModel.onIsbnScanned(barcode)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var currentScreen by remember { mutableStateOf("Ma bibliothèque") }

            MitosBookingTheme {
                LaunchedEffect(Unit) {
                    viewModel.toastMessages.collect { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text(currentScreen) },
                            actions = {
                                IconButton(onClick = { context.startActivity(Intent(context, ProfileActivity::class.java)) }) {
                                    Icon(Icons.Filled.Person, contentDescription = "Profil")
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Book, contentDescription = "Ma bibliothèque") },
                                label = { Text("Ma bibliothèque") },
                                selected = currentScreen == "Ma bibliothèque",
                                onClick = { currentScreen = "Ma bibliothèque" }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.People, contentDescription = "Mes prêts") },
                                label = { Text("Mes prêts") },
                                selected = currentScreen == "Mes prêts",
                                onClick = { currentScreen = "Mes prêts" }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.AutoStories, contentDescription = "Mes emprunts") },
                                label = { Text("Mes emprunts") },
                                selected = currentScreen == "Mes emprunts",
                                onClick = { currentScreen = "Mes emprunts" }
                            )
                        }
                    },
                    floatingActionButton = {
                        if (currentScreen == "Ma bibliothèque") {
                            FloatingActionButton(onClick = {
                                scanLauncher.launch(Intent(context, ScannerActivity::class.java))
                            }) {
                                Icon(Icons.Filled.Add, contentDescription = "Ajouter un livre")
                            }
                        } else if (currentScreen == "Mes emprunts") {
                            FloatingActionButton(onClick = {
                                scanLauncher.launch(Intent(context, ScannerActivity::class.java))
                            }) {
                                Icon(Icons.Filled.Add, contentDescription = "Emprunter un livre")
                            }
                        }
                    }
                ) { innerPadding ->
                    when (currentScreen) {
                        "Ma bibliothèque" -> MyLibraryScreen(viewModel, Modifier.padding(innerPadding))
                        "Mes prêts" -> MyLoansScreen(viewModel, Modifier.padding(innerPadding))
                        "Mes emprunts" -> MyBorrowsScreen(viewModel, Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyLibraryScreen(viewModel: MyLibraryViewModel, modifier: Modifier = Modifier) {
    val books by viewModel.myOwnedBooks.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        items(books, key = { it.uid }) { book ->
            val borrowerName by produceState<String?>(initialValue = null, key1 = book.borrowerId) {
                if (book.borrowerId != null) {
                    value = viewModel.getUserFullName(book.borrowerId)
                }
            }

            BookItem(
                book = book,
                borrowerName = borrowerName,
                lenderName = null,
                onLoanClick = {
                    val intent = Intent(context, TransactionActivity::class.java).apply {
                        putExtra("bookId", book.uid)
                        putExtra("action", "LOAN")
                    }
                    context.startActivity(intent)
                },
                onReturnClick = { },
                modifier = Modifier.animateItemPlacement(tween(durationMillis = 300))
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyLoansScreen(viewModel: MyLibraryViewModel, modifier: Modifier = Modifier) {
    val books by viewModel.myLoans.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        items(books, key = { it.uid }) { book ->
            val borrowerName by produceState<String?>(initialValue = null, key1 = book.borrowerId) {
                if (book.borrowerId != null) {
                    value = viewModel.getUserFullName(book.borrowerId)
                }
            }

            BookItem(
                book = book,
                borrowerName = borrowerName,
                lenderName = null,
                onLoanClick = { },
                onReturnClick = {
                    // Le prêteur génère le QR code pour le retour
                    val intent = Intent(context, TransactionActivity::class.java).apply {
                        putExtra("bookId", book.uid)
                        putExtra("action", "RETURN")
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.animateItemPlacement(tween(durationMillis = 300))
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyBorrowsScreen(viewModel: MyLibraryViewModel, modifier: Modifier = Modifier) {
    val books by viewModel.myBorrows.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        items(books, key = { it.uid }) { book ->
            val lenderName by produceState<String?>(initialValue = null, key1 = book.lenderId) {
                if (book.lenderId != null) {
                    value = viewModel.getUserFullName(book.lenderId)
                }
            }

            BookItem(
                book = book,
                borrowerName = null,
                lenderName = lenderName,
                onLoanClick = { },
                onReturnClick = {
                    // L'emprunteur scanne le QR code généré par le prêteur
                    val intent = Intent(context, ScannerActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.animateItemPlacement(tween(durationMillis = 300))
            )
        }
    }
}

@Composable
fun BookItem(
    book: Book,
    borrowerName: String?,
    lenderName: String?,
    onLoanClick: () -> Unit,
    onReturnClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Card(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(120.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(book.covers),
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (book.authors != null) {
                    Text(
                        text = book.authors,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val (statusText, statusColor) = when {
                    book.lenderId == null && book.borrowerId == null ->
                        "Disponible" to fr.enssat.sharemybook.mitosbooking.ui.theme.StatusAvailable
                    book.borrowerId != null && book.lenderId == null ->
                        "Prêté à ${borrowerName ?: "inconnu"}" to fr.enssat.sharemybook.mitosbooking.ui.theme.StatusLoaned
                    book.lenderId != null ->
                        "Prêté par ${lenderName ?: "inconnu"}" to fr.enssat.sharemybook.mitosbooking.ui.theme.StatusBorrowed
                    else ->
                        "Non disponible" to fr.enssat.sharemybook.mitosbooking.ui.theme.StatusUnavailable
                }

                androidx.compose.material3.Surface(
                    shape = MaterialTheme.shapes.small,
                    color = statusColor.copy(alpha = 0.15f),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (book.lenderId == null && book.borrowerId == null) {
                        // Livre disponible - on peut le prêter
                        Button(
                            onClick = onLoanClick,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Prêter")
                        }
                    } else if (book.lenderId != null) {
                        // J'ai emprunté ce livre - je peux scanner le QR code du prêteur
                        Button(
                            onClick = onReturnClick,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Scanner le QR code")
                        }
                    } else {
                        // J'ai prêté ce livre - je génère le QR code pour le retour
                        Button(
                            onClick = onReturnClick,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Générer le QR code")
                        }
                    }
                }
            }
        }
    }
}

