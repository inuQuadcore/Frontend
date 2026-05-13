package com.everybuddy.app.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Sign-In 결과
 */
sealed class GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult()
    /** 사용자가 계정 선택 다이얼로그에서 취소함 (에러 메시지 표시 X) */
    object Cancelled                        : GoogleSignInResult()
    data class Error(val message: String)   : GoogleSignInResult()
}

/**
 * GoogleAuthManager
 *
 * Credential Manager API를 사용하여 Google ID Token을 획득.
 * 획득한 idToken을 AuthRepository.googleLogin()에 전달하면 서버 인증 완료.
 */
@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val credentialManager = CredentialManager.create(context)

    /**
     * Google 로그인 실행
     *
     * @param activityContext Activity Context (Credential Manager는 Activity 필요)
     * @param webClientId     백엔드에서 복사한 웹 클라이언트 ID
     *
     */
    suspend fun signIn(
        activityContext : Context,
        webClientId     : String,
    ): GoogleSignInResult {
        return try {
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext,
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInResult.Success(googleIdTokenCredential.idToken)
            } else {
                GoogleSignInResult.Error("지원하지 않는 인증 방식입니다.")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: GetCredentialException) {
            GoogleSignInResult.Error(e.localizedMessage ?: "Google 로그인에 실패했습니다.")
        } catch (e: GoogleIdTokenParsingException) {
            GoogleSignInResult.Error("Google ID Token 파싱 실패: ${e.localizedMessage}")
        }
    }
}
