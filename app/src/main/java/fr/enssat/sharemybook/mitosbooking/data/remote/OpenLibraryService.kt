package fr.enssat.sharemybook.mitosbooking.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenLibraryService {

    @GET("api/books")
    suspend fun getBookByIsbn(
        @Query("bibkeys") bibkeys: String,
        @Query("jscmd") jscmd: String = "data",
        @Query("format") format: String = "json"
    ): Map<String, BookDetails>
}
