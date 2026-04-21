package com.example.fitlinktrainer.di



import com.example.fitlinktrainer.data.repository.AdminRepository
import com.example.fitlinktrainer.data.repository.FirebaseAdminRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AdminRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAdminRepository(
        repo: FirebaseAdminRepository
    ): AdminRepository
}