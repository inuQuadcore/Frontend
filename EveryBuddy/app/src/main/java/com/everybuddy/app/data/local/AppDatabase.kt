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
    version       = 6,                                   // v6: ChatMessageEntity.voiceDuration 추가 (음성 길이 DB 영속)
    exportSchema  = false,
)
@TypeConverters(LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun folderDao(): FolderDao
}
