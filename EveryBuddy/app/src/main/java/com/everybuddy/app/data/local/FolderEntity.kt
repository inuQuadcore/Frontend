package com.everybuddy.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 사용자가 정의한 채팅방 폴더 (로컬-only, 디바이스 종속). */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id : String,
    val name           : String,
    val order          : Int,
)

/** 폴더 ↔ 채팅방 매핑. 한 채팅방이 여러 폴더에 속할 수 있음. */
@Entity(
    tableName   = "folder_rooms",
    primaryKeys = ["folderId", "chatRoomId"],
    indices     = [Index("folderId"), Index("chatRoomId")],
)
data class FolderRoomEntity(
    val folderId   : String,
    val chatRoomId : String,
)
