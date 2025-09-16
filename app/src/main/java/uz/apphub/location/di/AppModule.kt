package uz.apphub.location.di

//* Shokirov Begzod  16.09.2025 *//

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uz.apphub.location.repo.FirebaseRepository
import uz.apphub.location.repo.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseRepository(firestore: FirebaseFirestore): FirebaseRepository =
        FirebaseRepository(firestore)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context
    ): SettingsRepository = SettingsRepository(firestore, context)
}