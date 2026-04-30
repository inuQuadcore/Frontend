// Retrofit 설정
package com.everybuddy.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.everybuddy.app.data.network.AuthApi
import com.everybuddy.app.data.network.ChatApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


// DataStore 확장 (토큰 저장소)

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

object PrefKeys {
    val ACCESS_TOKEN = stringPreferencesKey("access_token")
    val USER_ID      = stringPreferencesKey("user_id")
}


// Hilt Network Module
/**
 * NetworkModule
 *
 * OkHttpClient, Retrofit, AuthApi, ChatApiService를 SingletonComponent에 등록.
 * JWT 토큰은 OkHttpClient 인터셉터에서 DataStore에서 읽어 자동 첨부.
 *
 * data/network/NetworkModule.kt와 di/NetworkModule.kt 통합 — di/ 하나로 관리.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.everybuddy.cloud/"

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            // JWT 자동 첨부 인터셉터
            .addInterceptor { chain ->
                val token = runBlocking {
                    context.dataStore.data
                        .map { it[PrefKeys.ACCESS_TOKEN] }
                        .first()
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
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideChatApiService(retrofit: Retrofit): ChatApiService =
        retrofit.create(ChatApiService::class.java)
}
