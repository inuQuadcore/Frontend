package com.everybuddy.app.data.chat

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 채팅 미디어(음성/이미지/파일)의 로컬 영속 저장소.
 *
 * presigned URL은 만료되는 transient 자산이므로 영속화 X.
 * 대신 파일 자체를 filesDir/chat_media/{messageId}.{ext}에 저장.
 *
 * - 송신: 업로드 성공 시 cacheDir 파일을 persistFromCache로 이동.
 * - 수신: 첫 재생/표시/번역 시 downloadAndPersist로 다운로드 후 캐시.
 * - 다음 접근부터는 pathFor(messageId, ext)로 조회.
 */
@Singleton
class MediaFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("plain") private val httpClient: OkHttpClient,
) {
    private val rootDir: File by lazy {
        File(context.filesDir, "chat_media").apply { mkdirs() }
    }

    /** messageId + 확장자 → 영속 파일 경로 (존재 여부와 무관). */
    fun pathFor(messageId: Long, ext: String): File =
        File(rootDir, "$messageId.${ext.lowercase().ifBlank { "bin" }}")

    /** 영속 파일이 실제로 존재하는지. */
    fun exists(messageId: Long, ext: String): Boolean =
        pathFor(messageId, ext).let { it.exists() && it.length() > 0 }

    /**
     * cacheDir(또는 임의 위치)의 파일을 영속 위치로 이동.
     * 송신 업로드 성공 직후 호출. 원본은 제거됨.
     * 반환: 이동된 영속 파일.
     */
    suspend fun persistFromCache(source: File, messageId: Long, ext: String): File =
        withContext(Dispatchers.IO) {
            val dest = pathFor(messageId, ext)
            if (dest.exists()) dest.delete()
            // 같은 파일시스템이면 rename, 아니면 copy + delete.
            if (!source.renameTo(dest)) {
                source.copyTo(dest, overwrite = true)
                source.delete()
            }
            dest
        }

    /**
     * URL에서 다운로드 후 영속 위치에 저장.
     * plain OkHttpClient 사용 — JWT Authorization 헤더가 안 붙어 S3 presigned 통과.
     * 반환: 성공 시 영속 파일, 실패 시 null.
     */
    suspend fun downloadAndPersist(url: String, messageId: Long, ext: String): File? =
        withContext(Dispatchers.IO) {
            val dest = pathFor(messageId, ext)
            try {
                httpClient.newCall(Request.Builder().url(url).build()).execute().use { res ->
                    if (!res.isSuccessful) return@withContext null
                    val body = res.body ?: return@withContext null
                    dest.outputStream().use { out -> body.byteStream().copyTo(out) }
                    if (dest.length() == 0L) { dest.delete(); null } else dest
                }
            } catch (e: Exception) {
                if (dest.exists()) dest.delete()
                null
            }
        }

    /** 확장자 추론 — URL 경로 끝 또는 fileName에서. 비어 있으면 fallback. */
    fun extFromUrlOrName(url: String?, fileName: String?, fallback: String): String {
        val fromName = fileName?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }
        val fromUrl  = url?.substringBefore('?')?.substringAfterLast('/')
            ?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }
        return (fromName ?: fromUrl ?: fallback).lowercase()
    }
}
