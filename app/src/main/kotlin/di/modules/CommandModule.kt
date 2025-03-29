import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@Module
object CommandModule {
    @Provides
    @IntoSet
    fun provideUploadCommand(uploadCommand: UploadCommand): Command = uploadCommand 

    @Provides
    @IntoSet
    fun provideDownloadCommand(downloadCommand: DownloadCommand): Command = downloadCommand

    @Provides
    @IntoSet
    fun provideOpenCommand(openCommand: OpenCommand): Command = openCommand

    @Provides
    @IntoSet
    fun provideDeleteCommand(deleteCommand: DeleteCommand): Command = deleteCommand

    @Provides
    @IntoSet
    fun provideBackCommand(backCommand: BackCommand): Command = backCommand

    @Provides
    @IntoSet
    fun provideSetTokenCommand(setTokenCommand: SetTokenCommand): Command = setTokenCommand

    @Provides
    @IntoSet
    fun provideSetChannelIdCommand(setChannelIdCommand: SetChannelIdCommand): Command = setChannelIdCommand
}