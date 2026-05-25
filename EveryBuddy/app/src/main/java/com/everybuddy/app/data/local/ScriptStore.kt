package com.everybuddy.app.data.local

import android.content.Context
import com.everybuddy.app.ui.chat.ScriptFolder
import com.everybuddy.app.ui.chat.ScriptItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScriptStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val gson        = Gson()
    private val itemsFile   = File(context.filesDir, "script_items.json")
    private val foldersFile = File(context.filesDir, "script_folders.json")

    fun loadItems(): List<ScriptItem> = try {
        if (!itemsFile.exists()) emptyList()
        else {
            val type = object : TypeToken<List<ScriptItem>>() {}.type
            gson.fromJson<List<ScriptItem>>(itemsFile.readText(), type) ?: emptyList()
        }
    } catch (_: Exception) { emptyList() }

    fun loadFolders(): List<ScriptFolder> = try {
        if (!foldersFile.exists()) emptyList()
        else {
            val type = object : TypeToken<List<ScriptFolder>>() {}.type
            gson.fromJson<List<ScriptFolder>>(foldersFile.readText(), type) ?: emptyList()
        }
    } catch (_: Exception) { emptyList() }

    fun saveItems(items: List<ScriptItem>) {
        try { itemsFile.writeText(gson.toJson(items)) } catch (_: Exception) {}
    }

    fun saveFolders(folders: List<ScriptFolder>) {
        try { foldersFile.writeText(gson.toJson(folders)) } catch (_: Exception) {}
    }
}
