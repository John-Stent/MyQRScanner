package com.istentoumis.myqrscanner

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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

    private fun updateAuthState(user: FirebaseUser?) {
        _authState.value = user
    }

    fun startPhoneNumberVerification(
        activity: FragmentActivity,
        phoneNumber: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        authManager.startPhoneNumberVerification(activity, phoneNumber, callbacks)
    }

    fun signInWithPhoneAuthCredential(
        activity: FragmentActivity,
        credential: PhoneAuthCredential,
        onComplete: (Boolean, String?) -> Unit
    ) {
        authManager.signInWithPhoneAuthCredential(activity, credential) { success, message ->
            if (success) {
                val user = authManager.getCurrentUser()
                authManager.setCurrentUser(user) // Set the authenticated user globally
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
                authManager.setCurrentUser(user) // Set the authenticated user globally
                updateAuthState(user)
            }
            onComplete(success, message)
        }
    }
}
