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
    version       = 3,                                   // v3: ChatMessageEntity.senderName 제거 (stale 방지, senderId로 cache lookup)
    exportSchema  = false,
)
@TypeConverters(LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun folderDao(): FolderDao
}
