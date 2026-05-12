package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

data class FriendListResponse(
    @SerializedName("friends") val friends : List<Friend>,
)

data class Friend(
    @SerializedName("userId")          val userId          : Long,
    @SerializedName("name")            val name            : String,
    @SerializedName("profileImageUrl") val profileImageUrl : String?              = null,
    @SerializedName("country")         val country         : String               = "",
    @SerializedName("bio")             val bio             : String               = "",
    @SerializedName("languages")       val languages       : List<LanguageLevel>  = emptyList(),
    @SerializedName("tags")            val tags            : List<UserTag>         = emptyList(),
)
