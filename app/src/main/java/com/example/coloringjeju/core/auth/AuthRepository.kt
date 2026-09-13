package com.example.coloringjeju.core.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Result of a sign-in/sign-up attempt — mirrors [com.example.coloringjeju.core.network.TourApiResult]'s
 * try/catch-free shape, with the failure message already localized to Korean for direct display.
 */
sealed interface AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult
    data class Error(val message: String) : AuthResult
}

/**
 * Thin wrapper around [FirebaseAuth]'s email/password sign-in — the one place the rest of the app
 * talks to Firebase Auth through. Every function runs on [Dispatchers.IO] and returns an
 * [AuthResult] rather than throwing, so [com.example.coloringjeju.presentation.Auth.AuthScreen]
 * never needs a try/catch.
 */
object AuthRepository {
    private val auth get() = FirebaseAuth.getInstance()

    /** Null until a session exists; Firebase persists a signed-in session locally on its own. */
    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun signUp(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        runCatching { auth.createUserWithEmailAndPassword(email, password).await() }
            .fold(
                onSuccess = { AuthResult.Success(it.user!!) },
                onFailure = { AuthResult.Error(it.toAuthErrorMessage()) },
            )
    }

    suspend fun signIn(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        runCatching { auth.signInWithEmailAndPassword(email, password).await() }
            .fold(
                onSuccess = { AuthResult.Success(it.user!!) },
                onFailure = { AuthResult.Error(it.toAuthErrorMessage()) },
            )
    }

    fun signOut() = auth.signOut()

    /** Updates the signed-in user's display name — 마이페이지's ✏️ edit sheet. Profile photo has no
     * such remote field (no Firebase Storage wired up), so that stays purely local; see
     * [com.example.coloringjeju.core.local.datastore.ProfilePhotoStore]. */
    suspend fun updateProfile(displayName: String): AuthResult = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) {
            AuthResult.Error("로그인이 필요해요.")
        } else {
            runCatching {
                user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(displayName).build()).await()
                user
            }.fold(
                onSuccess = { AuthResult.Success(it) },
                onFailure = { AuthResult.Error(it.toAuthErrorMessage()) },
            )
        }
    }

    private fun Throwable.toAuthErrorMessage(): String = when (this) {
        is FirebaseAuthWeakPasswordException -> "비밀번호는 6자 이상으로 입력해주세요."
        is FirebaseAuthInvalidCredentialsException -> "이메일 형식이 올바르지 않거나 비밀번호가 틀렸어요."
        is FirebaseAuthUserCollisionException -> "이미 가입된 이메일이에요."
        is FirebaseAuthInvalidUserException -> "가입되지 않은 이메일이에요."
        is FirebaseNetworkException -> "네트워크 연결을 확인해주세요."
        else -> message ?: "알 수 없는 오류가 발생했어요."
    }
}
