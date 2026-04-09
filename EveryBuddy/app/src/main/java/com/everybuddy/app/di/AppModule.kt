// Hilt DI 설정
package com.everybuddy.app.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AppModule
 *
 * NetworkModule에서 제공하는 Retrofit 인스턴스를 사용하여
 * ChatApiService, Gson 등 앱 전반의 의존성을 등록.
 *
 * SharedUserRepository는 @Singleton + @Inject constructor로 선언되어
 * 별도 @Provides 없이 Hilt가 자동 주입.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()
}
