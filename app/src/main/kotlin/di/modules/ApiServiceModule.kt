import dagger.Module
import dagger.Provides

@Module
object ApiServiceModule {
    @Provides
    fun provideDiscordApiService(): DiscordApiService {
        return ApiClient.create(DiscordApiService::class.java)
    }
}