package com.everybuddy.app.data.dto

import com.google.gson.annotations.SerializedName

data class UserPublicProfileResponse(
    @SerializedName("profileImageUrl") val profileImageUrl : String? = null,
    @SerializedName("country")         val country         : String  = "",
    @SerializedName("name")            val name            : String,
    @SerializedName("age")             val age             : Int     = 0,
    // 본인이 자기 자신을 조회할 때만 값이 채워짐. 타인 조회 시 null.
    @SerializedName("birthday")        val birthday        : String? = null,
    @SerializedName("gender")          val gender          : String  = "",
    @SerializedName("bio")             val bio             : String  = "",
    @SerializedName("consecutiveDays") val consecutiveDays : Int     = 0,
)

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

data class UserLanguagesResponse(
    @SerializedName("languages") val languages : List<LanguageLevel>,
    @SerializedName("isOwner")   val isOwner   : Boolean,
)

data class UpdateLanguageLevelRequest(
    @SerializedName("language") val language : String,
    @SerializedName("level")    val level    : Int,
)

data class UpdateTagsRequest(
    @SerializedName("tags") val tags : List<String>,
)
