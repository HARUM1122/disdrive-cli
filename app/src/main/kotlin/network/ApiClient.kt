import okhttp3.OkHttpClient

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private fun buildOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
        .connectTimeout(0, java.util.concurrent.TimeUnit.SECONDS) 
        .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS)  
        .writeTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    }

    private fun buildRetrofit(): Retrofit {
        val okHttpClient: OkHttpClient = buildOkHttpClient()
        return Retrofit.Builder()
            .baseUrl("https://discord.com/api/v10/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> create(service: Class<T>): T {
        val retrofit: Retrofit = buildRetrofit()
        return retrofit.create(service)
    }
}
