package com.everybuddy.app.di

// NetworkModule — Hilt DI 네트워크 설정

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.everybuddy.app.data.network.ApiService
import com.everybuddy.app.data.network.AuthApi
import com.everybuddy.app.data.network.ChatApiService
import com.everybuddy.app.data.network.TokenAuthenticator
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
            level = HttpLoggingInterceptor.Level.BODY
        }

    @Provides
    @Singleton
    fun provideAuthenticator(authenticator: TokenAuthenticator): Authenticator = authenticator

    // Main OkHttpClient — JWT 인터셉터 + Authenticator (401 자동 refresh)
    // 일반 API(ApiService, ChatApiService)용
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
        .addInterceptor(loggingInterceptor)  // TODO: Release 빌드에서 제거
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

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    fun provideAuthApi(@Named("auth") retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideChatApiService(retrofit: Retrofit): ChatApiService =
        retrofit.create(ChatApiService::class.java)
}
