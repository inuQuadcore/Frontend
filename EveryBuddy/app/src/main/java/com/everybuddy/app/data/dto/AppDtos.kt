package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

// 공통 — 여러 도메인(Auth/Friend/Discover/User)에서 공유
data class LanguageLevel(
    @SerializedName("language") val language : String,           // "ENGLISH", "KOREAN" 등
    @SerializedName("level")    val level    : Int,              // 1~5
)

// Friend
data class FriendListResponse(@SerializedName("friends") val friends: List<FriendDto>)
data class FriendDto(
    @SerializedName("userId")          val userId          : Long,
    @SerializedName("name")            val name            : String,
    @SerializedName("profileImageUrl") val profileImageUrl : String?              = null,
    @SerializedName("country")         val country         : String               = "",
    @SerializedName("bio")             val bio             : String               = "",
    @SerializedName("languages")       val languages       : List<LanguageLevel> = emptyList(),
    @SerializedName("tags")            val tags            : List<TagDto>         = emptyList(),
)

// Discover
data class DiscoverResponse(@SerializedName("users") val users: List<DiscoverUserDto>)
data class DiscoverFilterResponse(
    @SerializedName("users")      val users      : List<DiscoverUserDto>,
    @SerializedName("hasNext")    val hasNext    : Boolean,
    @SerializedName("nextCursor") val nextCursor : Long? = null,
)
data class DiscoverUserDto(
    @SerializedName("userId")          val userId          : Long,
    @SerializedName("name")            val name            : String,
    @SerializedName("profileImageUrl") val profileImageUrl : String?              = null,
    @SerializedName("country")         val country         : String               = "",
    @SerializedName("bio")             val bio             : String               = "",
    @SerializedName("languages")       val languages       : List<LanguageLevel> = emptyList(),
    @SerializedName("tags")            val tags            : List<TagDto>         = emptyList(),
    @SerializedName("lastSeenAt")      val lastSeenAt      : String?              = null,
)

// Messages
data class EditMessageRequest(@SerializedName("content") val content: String)
data class MessageDto(
    @SerializedName("messageId")   val messageId   : Long,
    @SerializedName("userId")      val userId      : Long,
    @SerializedName("userName")    val userName    : String,
    @SerializedName("messageType") val messageType : String,
    @SerializedName("content")     val content     : String? = null,
    @SerializedName("sendAt")      val sendAt      : String,
    @SerializedName("fileUrl")     val fileUrl     : String? = null,
    @SerializedName("fileName")    val fileName    : String? = null,
    @SerializedName("fileSize")    val fileSize    : Long?   = null,
    @SerializedName("mediaType")   val mediaType   : String? = null,
)
data class SyncMessagesResponse(
    @SerializedName("newMessages")     val newMessages     : List<MessageDto> = emptyList(),
    @SerializedName("updatedMessages") val updatedMessages : List<MessageDto> = emptyList(),
    @SerializedName("deletedIds")      val deletedIds      : List<Long>       = emptyList(),
)

// User
data class UserPublicProfileResponse(
    @SerializedName("profileImageUrl") val profileImageUrl : String? = null,
    @SerializedName("country")         val country         : String  = "",
    @SerializedName("name")            val name            : String,
    @SerializedName("age")             val age             : Int     = 0,
    @SerializedName("gender")          val gender          : String  = "",
    @SerializedName("bio")             val bio             : String  = "",
)
data class TagDto(
    @SerializedName("tag")      val tag      : String,
    @SerializedName("category") val category : String = "",
)
data class UserLanguagesResponse(
    @SerializedName("languages") val languages : List<LanguageLevel>,
    @SerializedName("isOwner")   val isOwner   : Boolean,
)
data class UpdateLanguageLevelRequest(
    @SerializedName("language") val language : String,
    @SerializedName("level")    val level    : Int,
)
data class UpdateTagsRequest(@SerializedName("tags") val tags: List<String>)
data class UserProfileResponse(
    @SerializedName("userId")          val userId          : Long,
    @SerializedName("name")            val name            : String,
    @SerializedName("profileImageUrl") val profileImageUrl : String? = null,
    @SerializedName("birthday")        val birthday        : String  = "",
    @SerializedName("gender")          val gender          : String  = "",
    @SerializedName("country")         val country         : String  = "",
    @SerializedName("bio")             val bio             : String  = "",
)
data class UpdateProfileRequest(
    @SerializedName("name")     val name     : String? = null,
    @SerializedName("bio")      val bio      : String? = null,
    @SerializedName("gender")   val gender   : String? = null,
    @SerializedName("country")  val country  : String? = null,
    @SerializedName("birthday") val birthday : String? = null,
)

// Chatroom
data class ChatRoomDto(
    @SerializedName("chatRoomId")     val chatRoomId     : Long,
    @SerializedName("roomName")       val roomName       : String,
    @SerializedName("createdAt")      val createdAt      : String,
    @SerializedName("participantIds") val participantIds : List<Long>,
    @SerializedName("unreadCount")    val unreadCount    : Int,
)
data class CreateChatRoomRequest(
    @SerializedName("roomName")       val roomName       : String,
    @SerializedName("participantIds") val participantIds : List<Long>,
)

// Status Message
data class StatusMessageRequest(@SerializedName("content") val content: String)
data class MyStatusMessageResponse(
    @SerializedName("statusMessageId") val statusMessageId : Long,
    @SerializedName("content")         val content         : String,
    @SerializedName("timeAgo")         val timeAgo         : String,
)
data class FriendStatusMessagesResponse(
    @SerializedName("statusMessages") val statusMessages : List<FriendStatusMessageDto>,
    @SerializedName("nextCursor")     val nextCursor     : Long?   = null,
    @SerializedName("hasNext")        val hasNext        : Boolean,
)
data class FriendStatusMessageDto(
    @SerializedName("statusMessageId") val statusMessageId : Long,
    @SerializedName("userId")          val userId          : Long,
    @SerializedName("userName")        val userName        : String,
    @SerializedName("profileImageUrl") val profileImageUrl : String? = null,
    @SerializedName("content")         val content         : String,
    @SerializedName("timeAgo")         val timeAgo         : String,
)
