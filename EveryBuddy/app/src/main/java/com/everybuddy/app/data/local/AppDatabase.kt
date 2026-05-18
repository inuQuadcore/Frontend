package com.everybuddy.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ChatMessageEntity::class,
        FolderEntity::class,
        FolderRoomEntity::class,
    ],
    version       = 4,                                   // v4: ChatMessageEntity.translatedText/sourceText 추가 (번역 영구 캐싱)
    exportSchema  = false,
)
@TypeConverters(LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun folderDao(): FolderDao
}
