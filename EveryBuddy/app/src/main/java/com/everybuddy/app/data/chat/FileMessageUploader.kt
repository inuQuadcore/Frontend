package com.everybuddy.app.data.chat

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.everybuddy.app.data.dto.ApiResult
import com.everybuddy.app.data.repository.MessageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 파일/사진/음성 메시지 업로드 책임.
 * URI 또는 File을 받아 MIME 결정 + 크기 검증 + MessageRepository.sendFileMessage 호출.
 * 임시 파일 정리까지.
 */
@Singleton
class FileMessageUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageRepository: MessageRepository,
) {
    /** URI 기반 업로드 — 갤러리/카메라/SAF picker 결과. */
    suspend fun upload(
        chatRoomId : Long,
        uri        : Uri,
        content    : String = "",
    ): ApiResult<Unit> {
        val file = copyToCache(uri)
            ?: return ApiResult.Error(-1, "FILE_PREPARE_FAILED", "파일을 읽을 수 없습니다.")
        return try {
            val mime = resolveMime(uri, file)
            if (file.length() > MAX_BYTES) {
                return ApiResult.Error(-1, "FILE_TOO_LARGE", "파일이 너무 큽니다. (최대 50MB)")
            }
            messageRepository.sendFileMessage(chatRoomId, content, file, mime)
        } finally {
            file.delete()
        }
    }

    /** File 직접 전달 — 음성 녹음 결과 등 이미 cacheDir에 있는 파일. */
    suspend fun upload(
        chatRoomId : Long,
        file       : File,
        mimeType   : String,
        content    : String = "",
    ): ApiResult<Unit> {
        if (file.length() > MAX_BYTES) {
            return ApiResult.Error(-1, "FILE_TOO_LARGE", "10MB를 초과합니다.")
        }
        return messageRepository.sendFileMessage(chatRoomId, content, file, mimeType)
    }

    private fun copyToCache(uri: Uri): File? {
        return try {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri)
            val ext  = mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: "bin"
            val file = File.createTempFile("upload_", ".$ext", context.cacheDir)
            val inputStream = resolver.openInputStream(uri) ?: return null
            inputStream.use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveMime(uri: Uri, file: File): String {
        context.contentResolver.getType(uri)?.let { return it }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
            ?: "application/octet-stream"
    }

    companion object {
        private const val MAX_BYTES = 50L * 1024 * 1024
    }
}
