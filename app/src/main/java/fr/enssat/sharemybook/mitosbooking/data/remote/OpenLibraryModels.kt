package fr.enssat.sharemybook.mitosbooking.data.remote

data class BookDetails(
    val title: String,
    val authors: List<Author>?,
    val cover: Cover?
)

data class Author(
    val name: String
)

data class Cover(
    val medium: String
)
