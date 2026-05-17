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
    version       = 2,                                   // v2: ChatMessageEntity.statusPreview 추가
    exportSchema  = false,
)
@TypeConverters(LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun folderDao(): FolderDao
}
