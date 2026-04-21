package com.example.fitlink.di

import com.example.fitlink.data.repositories.*
import com.example.fitlink.data.service.FirebaseService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    // ---------------- FIREBASE INSTANCES ----------------

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    // ---------------- FIREBASE SERVICE ----------------

    @Provides
    @Singleton
    fun provideFirebaseService(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): FirebaseService {
        return FirebaseService(firestore, auth)
    }

    // ---------------- REPOSITORIES ----------------

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseService: FirebaseService
    ): AuthRepository {
        return AuthRepository(firebaseService)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        firebaseService: FirebaseService
    ): UserRepository {
        return UserRepository(firebaseService)
    }

    @Provides
    @Singleton
    fun provideWorkoutRepository(
        firebaseService: FirebaseService
    ): WorkoutRepository {
        return WorkoutRepository(firebaseService)
    }

    @Provides
    @Singleton
    fun provideTrainerRepository(
        firebaseService: FirebaseService
    ): TrainerRepository {
        return TrainerRepository(firebaseService)
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        firebaseService: FirebaseService
    ): ChatRepository {
        return ChatRepository(firebaseService)
    }

    @Provides
    @Singleton
    fun provideDailyStatsRepository(
        firebaseService: FirebaseService
    ): DailyStatsRepository {
        return DailyStatsRepository(firebaseService)
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(
        firebaseService: FirebaseService,
        auth: FirebaseAuth
    ): NotificationRepository {
        return NotificationRepository(firebaseService, auth)
    }
}