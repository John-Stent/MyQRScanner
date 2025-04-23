package com.istentoumis.myqrscanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    private val _authState = MutableLiveData<FirebaseUser?>()
    val authState: LiveData<FirebaseUser?> get() = _authState

    private fun updateAuthState(user: FirebaseUser?) {
        _authState.value = user
    }

    fun startPhoneNumberVerification(activity: MainActivity, phoneNumber: String, callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks) {
        authManager.startPhoneNumberVerification(activity, phoneNumber, callbacks)
    }

    fun signInWithPhoneAuthCredential(activity: MainActivity, credential: PhoneAuthCredential, onComplete: (Boolean, String?) -> Unit) {
        authManager.signInWithPhoneAuthCredential(activity, credential) { success, message ->
            if (success) {
                updateAuthState(authManager.getCurrentUser())
            }
            onComplete(success, message)
        }
    }

    fun createUserWithPhoneNumber(credential: PhoneAuthCredential, onComplete: (Boolean, String?) -> Unit) {
        authManager.createUserWithPhoneNumber(credential) { success, message ->
            if (success) {
                updateAuthState(authManager.getCurrentUser())
            }
            onComplete(success, message)
        }
    }
}