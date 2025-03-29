import dagger.Module
import dagger.Provides

import java.sql.Connection
import java.sql.DriverManager
import javax.inject.Singleton

@Module
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabaseConnection(): Connection {
        return DriverManager.getConnection(DB_URL)
    }
}
