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
    version       = 5,                                   // v5: ChatMessageEntity.localFilePath 추가 (미디어 영속 저장)
    exportSchema  = false,
)
@TypeConverters(LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun folderDao(): FolderDao
}
