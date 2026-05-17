package com.everybuddy.app.di

// NetworkModule — Hilt DI 네트워크 설정

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.everybuddy.app.BuildConfig
import com.everybuddy.app.data.network.AuthApi
import com.everybuddy.app.data.network.BlockApi
import com.everybuddy.app.data.network.ChatRoomApi
import com.everybuddy.app.data.network.DiscoverApi
import com.everybuddy.app.data.network.FriendApi
import com.everybuddy.app.data.network.MessageApi
import com.everybuddy.app.data.network.StatusMessageApi
import com.everybuddy.app.data.network.TokenAuthenticator
import com.everybuddy.app.data.network.UserApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "everybuddy_token")

object TokenKeys {
    val ACCESS_TOKEN              = stringPreferencesKey("access_token")
    val REFRESH_TOKEN             = stringPreferencesKey("refresh_token")
    val ACCESS_TOKEN_EXPIRES_AT   = stringPreferencesKey("access_token_expires_at")
    val REFRESH_TOKEN_EXPIRES_AT  = stringPreferencesKey("refresh_token_expires_at")
    val USER_ID                   = stringPreferencesKey("user_id")
    val LAST_ATTENDANCE_DATE      = stringPreferencesKey("last_attendance_date")  // "yyyy-MM-dd" — 출석 API 마지막 호출일
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.everybuddy.cloud/"

    @Provides
    @Singleton
    fun provideTokenDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.tokenDataStore

    @Provides
    @Singleton
    @Named("jwt")
    fun provideJwtInterceptor(
        dataStore: DataStore<Preferences>,
    ): Interceptor = Interceptor { chain ->
        val token = runBlocking {
            dataStore.data.firstOrNull()?.get(TokenKeys.ACCESS_TOKEN)
        }

        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    @Provides
    @Singleton
    @Named("logging")
    fun provideLoggingInterceptor(): Interceptor =
        HttpLoggingInterceptor().apply {
            // Release 빌드에선 NONE — 비밀번호·토큰이 logcat에 평문 출력되는 보안 이슈 방지
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else                   HttpLoggingInterceptor.Level.NONE
        }

    @Provides
    @Singleton
    fun provideAuthenticator(authenticator: TokenAuthenticator): Authenticator = authenticator

    // Main OkHttpClient — JWT 인터셉터 + Authenticator (401 자동 refresh)
    // 일반 API(도메인별 *Api 인터페이스들)용
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @Named("jwt")     jwtInterceptor     : Interceptor,
        @Named("logging") loggingInterceptor : Interceptor,
        authenticator                        : Authenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(jwtInterceptor)
        .addInterceptor(loggingInterceptor)  // TODO: Release 빌드에서 제거
        .authenticator(authenticator)        // 401 시 자동 refresh
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Auth 전용 OkHttpClient — JWT 인터셉터만, Authenticator 없음
    // refresh API가 401 받아도 또 refresh 시도하는 무한 루프 방지 + 순환 의존성 차단
    @Provides
    @Singleton
    @Named("auth")
    fun provideAuthOkHttpClient(
        @Named("jwt")     jwtInterceptor     : Interceptor,
        @Named("logging") loggingInterceptor : Interceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(jwtInterceptor)
        .apply { if (BuildConfig.DEBUG) addInterceptor(loggingInterceptor) }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
        .create()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient : OkHttpClient,
        gson         : Gson,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    @Named("auth")
    fun provideAuthRetrofit(
        @Named("auth") okHttpClient : OkHttpClient,
        gson                        : Gson,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides @Singleton
    fun provideAuthApi(@Named("auth") retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides @Singleton
    fun provideFriendApi(retrofit: Retrofit): FriendApi =
        retrofit.create(FriendApi::class.java)

    @Provides @Singleton
    fun provideBlockApi(retrofit: Retrofit): BlockApi =
        retrofit.create(BlockApi::class.java)

    @Provides @Singleton
    fun provideDiscoverApi(retrofit: Retrofit): DiscoverApi =
        retrofit.create(DiscoverApi::class.java)

    @Provides @Singleton
    fun provideMessageApi(retrofit: Retrofit): MessageApi =
        retrofit.create(MessageApi::class.java)

    @Provides @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi =
        retrofit.create(UserApi::class.java)

    @Provides @Singleton
    fun provideChatRoomApi(retrofit: Retrofit): ChatRoomApi =
        retrofit.create(ChatRoomApi::class.java)

    @Provides @Singleton
    fun provideStatusMessageApi(retrofit: Retrofit): StatusMessageApi =
        retrofit.create(StatusMessageApi::class.java)
}
