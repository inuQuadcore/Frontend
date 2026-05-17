package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

data class Message(
    @SerializedName("messageId")   val messageId   : Long,
    @SerializedName("userId")      val userId      : Long,
    @SerializedName("userName")    val userName    : String,
    @SerializedName("messageType") val messageType : String,
    @SerializedName("content")     val content     : String? = null,
    @SerializedName("sendAt")      val sendAt      : String,
    @SerializedName("editedAt")    val editedAt    : String? = null,  // null이면 미수정. UI "(수정됨)" 표시 조건.
    @SerializedName("fileUrl")     val fileUrl     : String? = null,
    @SerializedName("fileName")    val fileName    : String? = null,
    @SerializedName("fileSize")    val fileSize    : Long?   = null,
    @SerializedName("mediaType")   val mediaType   : String? = null,
)

data class SyncMessagesResponse(
    @SerializedName("newMessages")     val newMessages     : List<Message> = emptyList(),
    @SerializedName("updatedMessages") val updatedMessages : List<Message> = emptyList(),
    @SerializedName("deletedIds")      val deletedIds      : List<Long>       = emptyList(),
)

// POST /api/v1/messages (multipart JSON part)
data class SendMessageRequest(
    @SerializedName("chatRoomId") val chatRoomId : Long,
    @SerializedName("content")    val content    : String,
)

data class EditMessageRequest(
    @SerializedName("content") val content : String,
)
