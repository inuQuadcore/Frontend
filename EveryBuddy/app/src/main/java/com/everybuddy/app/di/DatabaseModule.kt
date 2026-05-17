package com.everybuddy.app.di

import android.content.Context
import androidx.room.Room
import com.everybuddy.app.data.local.AppDatabase
import com.everybuddy.app.data.local.FolderDao
import com.everybuddy.app.data.local.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "everybuddy.db")
            // 개발 단계 — schema 변경 시 기존 캐시 삭제(메시지는 서버 sync로 재구성, 폴더는 사용자가 재설정).
            // 출시 전 정식 Migration 도입 필요.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideFolderDao(db: AppDatabase): FolderDao = db.folderDao()
}
