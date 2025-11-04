package fr.enssat.sharemybook.mitosbooking.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TransactionService {

    @POST("shareMyBook/init")
    suspend fun init(@Body request: InitRequest): InitResponse

    @POST("shareMyBook/accept/{shareId}")
    suspend fun accept(@Path("shareId") shareId: String, @Body request: AcceptRequest): TransactionData

    @GET("shareMyBook/result/{shareId}")
    suspend fun result(@Path("shareId") shareId: String): TransactionData
}
