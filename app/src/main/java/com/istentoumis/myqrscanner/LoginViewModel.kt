package com.istentoumis.myqrscanner

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    val authManager: AuthManager
) : ViewModel() {

    private val _authState = MutableLiveData<FirebaseUser?>()
    val authState: LiveData<FirebaseUser?> get() = _authState

    private val _verificationEvent = MutableLiveData<VerificationEvent>()
    val verificationEvent: LiveData<VerificationEvent> get() = _verificationEvent

    sealed class VerificationEvent {
        object Started : VerificationEvent()
        object CodeSent : VerificationEvent()
        data class Completed(val user: FirebaseUser?) : VerificationEvent()
        data class Failed(val error: String?) : VerificationEvent()
    }

    private fun updateAuthState(user: FirebaseUser?) {
        _authState.value = user
    }

    fun startPhoneNumberVerification(
        activity: FragmentActivity,
        phoneNumber: String
    ) {
        _verificationEvent.value = VerificationEvent.Started
        authManager.startPhoneNumberVerification(activity, phoneNumber, object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(activity, credential) { success, message ->
                    if (success) {
                        _verificationEvent.value = VerificationEvent.Completed(authManager.getCurrentUser())
                    } else {
                        _verificationEvent.value = VerificationEvent.Failed(message)
                    }
                }
            }
            override fun onVerificationFailed(e: FirebaseException) {
                _verificationEvent.value = VerificationEvent.Failed(e.message)
            }
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                authManager.setVerificationId(verificationId)

                _verificationEvent.value = VerificationEvent.CodeSent
            }
        })
    }

    fun verifyCode(
        activity: FragmentActivity,
        code: String
    ) {
        val credential = authManager.verifyPhoneNumberWithCode(code)
        createUserWithPhoneNumber(credential) { success, message ->
            if (success) {
                _verificationEvent.value = VerificationEvent.Completed(authManager.getCurrentUser())
            } else {
                _verificationEvent.value = VerificationEvent.Failed(message)
            }
        }
    }

    fun signInWithPhoneAuthCredential(
        activity: FragmentActivity,
        credential: PhoneAuthCredential,
        onComplete: (Boolean, String?) -> Unit
    ) {
        authManager.signInWithPhoneAuthCredential(activity, credential) { success, message ->
            if (success) {
                val user = authManager.getCurrentUser()
                authManager.setCurrentUser(user)
                updateAuthState(user)
            }
            onComplete(success, message)
        }
    }

    fun createUserWithPhoneNumber(
        credential: PhoneAuthCredential,
        onComplete: (Boolean, String?) -> Unit
    ) {
        authManager.createUserWithPhoneNumber(credential) { success, message ->
            if (success) {
                val user = authManager.getCurrentUser()
                updateAuthState(user)
            }
            onComplete(success, message)
        }
    }
}
