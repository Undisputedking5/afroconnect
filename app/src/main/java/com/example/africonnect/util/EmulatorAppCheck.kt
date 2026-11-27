package com.example.africonnect.util

import android.util.Base64
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.AppCheckToken

/**
 * Used only in CI builds that talk to the local Firebase emulators: the Database
 * emulator rejects the SDK's placeholder App Check token, so we hand it an unsigned
 * JWT it can parse. Never installed in normal builds.
 */
class EmulatorAppCheckFactory : AppCheckProviderFactory {
    override fun create(app: FirebaseApp): AppCheckProvider = AppCheckProvider {
        val now = System.currentTimeMillis()
        fun b64(s: String) = Base64.encodeToString(s.toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val jwt = b64("""{"alg":"none","typ":"JWT"}""") + "." +
            b64("""{"sub":"${app.options.applicationId}","aud":["projects/${app.options.projectId}"],"iat":${now / 1000},"exp":${now / 1000 + 3600}}""") + "."
        val token = object : AppCheckToken() {
            override fun getToken() = jwt
            override fun getExpireTimeMillis() = now + 3_600_000
        }
        Tasks.forResult(token) as Task<AppCheckToken>
    }
}
