package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

/**
 * Custom Application class ensuring safe Firebase and hardware subsystem initialization
 * on cold app startup.
 */
class ZariaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d("ZariaApplication", "FirebaseApp initialized successfully on startup.")
            }
        } catch (e: Exception) {
            Log.w("ZariaApplication", "FirebaseApp auto-init warning: ${e.message}")
        }
    }
}
