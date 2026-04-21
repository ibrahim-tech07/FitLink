package com.example.fitlinktrainer.di

import com.example.fitlinktrainer.data.repository.*
import com.example.fitlinktrainer.data.service.CloudinaryService
import com.example.fitlinktrainer.data.service.FirebaseService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideFirebaseService(): FirebaseService = FirebaseService()

    @Provides
    @Singleton
    fun provideAuthRepository(firebaseService: FirebaseService): AuthRepository =
        AuthRepository(firebaseService)

    @Provides
    @Singleton
    fun provideTrainerRepository(firebaseService: FirebaseService,cloudinaryService: CloudinaryService): TrainerRepository =
        TrainerRepository(firebaseService,cloudinaryService)

    @Provides
    @Singleton
    fun provideUserRepository(firebaseService: FirebaseService): UserRepository =
        UserRepository(firebaseService)

    @Provides
    @Singleton
    fun provideWorkoutRepository(firebaseService: FirebaseService): WorkoutRepository =
        WorkoutRepository(firebaseService)

    @Provides
    @Singleton
    fun provideChatRepository(firebaseService: FirebaseService): ChatRepository =
        ChatRepository(firebaseService)

    // ⭐ ADD THIS (Fix for your error)

}