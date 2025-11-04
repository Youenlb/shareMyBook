package fr.enssat.sharemybook.mitosbooking.data.remote

import fr.enssat.sharemybook.mitosbooking.data.entity.Book
import fr.enssat.sharemybook.mitosbooking.data.entity.User

data class InitRequest(
    val action: String,
    val book: Book,
    val owner: User
)

data class InitResponse(
    val shareId: String
)

data class AcceptRequest(
    val borrower: User
)

data class TransactionData(
    val action: String,
    val book: Book,
    val owner: User,
    val borrower: User?
)

data class ShareIdQrCode(val shareId: String)
