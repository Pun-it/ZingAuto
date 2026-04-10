package com.example.zingauto

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CredentialManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val gson = Gson()

    data class Credentials(
        val id: String = java.util.UUID.randomUUID().toString(),
        val companyCode: String,
        val employeeCode: String,
        val password: String
    )

    fun saveCredentialList(list: List<Credentials>) {
        val json = gson.toJson(list)
        sharedPreferences.edit().putString("credential_list", json).apply()
    }

    fun getCredentialList(): List<Credentials> {
        val json = sharedPreferences.getString("credential_list", null) ?: return emptyList()
        val type = object : TypeToken<List<Credentials>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addCredential(creds: Credentials) {
        val list = getCredentialList().toMutableList()
        list.add(creds)
        saveCredentialList(list)
    }

    fun removeCredential(id: String) {
        val list = getCredentialList().filter { it.id != id }
        saveCredentialList(list)
    }

    fun hasCredentials(): Boolean {
        return getCredentialList().isNotEmpty()
    }

    fun clearCredentials() {
        sharedPreferences.edit().clear().apply()
    }
}
