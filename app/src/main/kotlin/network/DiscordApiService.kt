import retrofit2.Call

import retrofit2.http.GET
import retrofit2.http.Url
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Streaming

import okhttp3.RequestBody
import okhttp3.ResponseBody

interface DiscordApiService {
    @POST("channels/{channel_id}/messages")
    fun uploadFile(
        @Header("Authorization") token: String,
        @Path("channel_id") channelId: String,
        @Body body: RequestBody
    ): Call<Map<String, Any>>

    @GET("channels/{channel_id}/messages/{message_id}")
    fun fetchMessage(
        @Header("Authorization") token: String,
        @Path("channel_id") channelId: String,
        @Path("message_id") messageId: String
    ): Call<Map<String, Any>>

    @GET
    @Streaming
    fun fetchData(
        @Url fileUrl: String,
        @Header("Range") range: String
    ): Call<ResponseBody>
}