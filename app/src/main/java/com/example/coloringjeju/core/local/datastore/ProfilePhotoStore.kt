package com.example.coloringjeju.core.local.datastore

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * On-device profile photo — there's no Firebase Storage wired up (no backend for any user media in
 * this app, same as [SavedSpotsStore]'s local-only spots), so a photo picked from the system photo
 * picker is just copied into this app's private files dir and its path kept here instead of being
 * uploaded anywhere. FirebaseAuth's `displayName` is a real remote field
 * ([com.example.coloringjeju.core.auth.AuthRepository.updateProfile] updates it), but the photo has
 * no such field to lean on, hence this separate local store.
 */
class ProfilePhotoStore private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "profile_photo.jpg")

    private val _photoUri = MutableStateFlow(if (file.exists()) file.toCacheBustedUri() else null)

    /** The current photo, or null before one's ever been picked. */
    val photoUri: StateFlow<Uri?> = _photoUri.asStateFlow()

    /** Copies [source] (a system photo-picker result) in as the new profile photo. */
    fun save(source: Uri) {
        appContext.contentResolver.openInputStream(source)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        // The file path never changes between photos, so bump a cache-busting query param onto
        // the Uri — Coil keys its cache on the full Uri string, and without this a re-picked photo
        // would keep showing the previous one from cache.
        _photoUri.value = file.toCacheBustedUri()
    }

    private fun File.toCacheBustedUri(): Uri =
        Uri.fromFile(this).buildUpon().appendQueryParameter("t", lastModified().toString()).build()

    companion object {
        @Volatile
        private var instance: ProfilePhotoStore? = null

        /** The process-wide store. Every screen must go through this — see the class doc. */
        fun get(context: Context): ProfilePhotoStore =
            instance ?: synchronized(this) {
                instance ?: ProfilePhotoStore(context).also { instance = it }
            }
    }
}
