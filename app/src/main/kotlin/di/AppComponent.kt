import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [CommandModule::class, DatabaseModule::class, ApiServiceModule::class])
interface AppComponent {
    fun getApplication(): App
}