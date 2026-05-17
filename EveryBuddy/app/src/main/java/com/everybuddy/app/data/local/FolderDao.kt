package com.everybuddy.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY `order` ASC")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: String)

    @Query("UPDATE folders SET `order` = :order WHERE id = :id")
    suspend fun updateOrder(id: String, order: Int)

    @Query("SELECT chatRoomId FROM folder_rooms WHERE folderId = :folderId")
    fun observeRoomsInFolder(folderId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addRoomToFolder(folderRoom: FolderRoomEntity)

    @Query("DELETE FROM folder_rooms WHERE folderId = :folderId AND chatRoomId = :chatRoomId")
    suspend fun removeRoomFromFolder(folderId: String, chatRoomId: String)

    @Query("DELETE FROM folder_rooms WHERE folderId = :folderId")
    suspend fun clearFolder(folderId: String)
}
