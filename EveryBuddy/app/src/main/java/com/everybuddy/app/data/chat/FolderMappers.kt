package com.everybuddy.app.data.chat

import com.everybuddy.app.data.local.FolderEntity
import com.everybuddy.app.data.local.FolderRoomEntity

/** ChatFolder(UI) ↔ Room Entity 변환. */

fun ChatFolder.toEntity(): FolderEntity =
    FolderEntity(id = id, name = name, order = order)

fun FolderEntity.toChatFolder(roomIds: List<String>): ChatFolder =
    ChatFolder(id = id, name = name, order = order, chatRoomIds = roomIds)

fun ChatFolder.toRoomEntities(): List<FolderRoomEntity> =
    chatRoomIds.map { FolderRoomEntity(folderId = id, chatRoomId = it) }
