package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

data class DiscoverResponse(
    @SerializedName("users") val users : List<DiscoverUser>,
)

data class DiscoverFilterResponse(
    @SerializedName("users")      val users      : List<DiscoverUser>,
    @SerializedName("hasNext")    val hasNext    : Boolean,
    @SerializedName("nextCursor") val nextCursor : Long? = null,
)

data class DiscoverUser(
    @SerializedName("userId")          val userId          : Long,
    @SerializedName("name")            val name            : String,
    @SerializedName("profileImageUrl") val profileImageUrl : String?              = null,
    @SerializedName("country")         val country         : String               = "",
    @SerializedName("bio")             val bio             : String               = "",
    @SerializedName("languages")       val languages       : List<LanguageLevel>  = emptyList(),
    @SerializedName("tags")            val tags            : List<UserTag>         = emptyList(),
    @SerializedName("lastSeenAt")      val lastSeenAt      : String?              = null,
)
