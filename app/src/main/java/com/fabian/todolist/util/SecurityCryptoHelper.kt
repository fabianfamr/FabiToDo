package com.fabian.todolist.util

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class InvalidBackupPasswordException : GeneralSecurityException("Invalid backup passphrase")

sealed class BackupImportResult {
    data object Success : BackupImportResult()
    data object RequiresPassword : BackupImportResult()
    data object WrongPassword : BackupImportResult()
    data object InvalidFile : BackupImportResult()
}

object SecurityCryptoHelper {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val AI_KEY_ALIAS = "FabiToDoAiKeyAlias"
    private const val ENC_PREFIX = "ENC_V1:"
    private const val AES_GCM_TRANSFORM = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_BYTES = 12
    private const val PBKDF2_SALT_BYTES = 16
    private const val PBKDF2_ITERATIONS = 100_000
    private const val AES_KEY_BITS = 256
    private val BACKUP_MAGIC_HEADER = "FABITODO_ENC_V1\n".toByteArray(Charsets.UTF_8)

    private fun getOrCreateKeyStoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existingKey = (keyStore.getEntry(AI_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        if (existingKey != null) return existingKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            AI_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_BITS)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encryptApiKey(plainText: String): String {
        val trimmed = plainText.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith(ENC_PREFIX)) return trimmed

        return try {
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORM)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeyStoreKey())
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
            ENC_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            android.util.Log.e("SecurityCryptoHelper", "Failed to encrypt API key", e)
            ""
        }
    }

    fun decryptApiKey(storedValue: String?): String {
        if (storedValue.isNullOrBlank()) return ""
        if (!storedValue.startsWith(ENC_PREFIX)) {
            return storedValue.trim()
        }
        return try {
            val payload = Base64.decode(storedValue.removePrefix(ENC_PREFIX), Base64.NO_WRAP)
            if (payload.size <= GCM_IV_BYTES) return ""
            val iv = payload.copyOfRange(0, GCM_IV_BYTES)
            val cipherBytes = payload.copyOfRange(GCM_IV_BYTES, payload.size)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKeyStoreKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8).trim()
        } catch (e: Exception) {
            android.util.Log.e("SecurityCryptoHelper", "Failed to decrypt API key", e)
            ""
        }
    }

    fun readAndMigrateApiKey(prefs: SharedPreferences, keyName: String = "ai_api_key"): String {
        val raw = prefs.getString(keyName, "") ?: ""
        if (raw.isBlank()) return ""
        if (!raw.startsWith(ENC_PREFIX)) {
            val encrypted = encryptApiKey(raw)
            if (encrypted.isNotEmpty()) {
                prefs.edit().putString(keyName, encrypted).apply()
            }
            return raw.trim()
        }
        return decryptApiKey(raw)
    }

    fun isEncryptedBackup(bytes: ByteArray): Boolean {
        if (bytes.size < BACKUP_MAGIC_HEADER.size + PBKDF2_SALT_BYTES + GCM_IV_BYTES + 16) return false
        for (i in BACKUP_MAGIC_HEADER.indices) {
            if (bytes[i] != BACKUP_MAGIC_HEADER[i]) return false
        }
        return true
    }

    private fun deriveBackupKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_BITS)
        return try {
            val derived = factory.generateSecret(spec).encoded
            SecretKeySpec(derived, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    fun encryptBackupBytes(plainBytes: ByteArray, passphrase: String): ByteArray {
        val salt = ByteArray(PBKDF2_SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val key = deriveBackupKey(passphrase, salt)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainBytes)

        val output = ByteArray(BACKUP_MAGIC_HEADER.size + salt.size + iv.size + cipherBytes.size)
        var offset = 0
        System.arraycopy(BACKUP_MAGIC_HEADER, 0, output, offset, BACKUP_MAGIC_HEADER.size)
        offset += BACKUP_MAGIC_HEADER.size
        System.arraycopy(salt, 0, output, offset, salt.size)
        offset += salt.size
        System.arraycopy(iv, 0, output, offset, iv.size)
        offset += iv.size
        System.arraycopy(cipherBytes, 0, output, offset, cipherBytes.size)
        return output
    }

    @Throws(InvalidBackupPasswordException::class)
    fun decryptBackupBytes(encryptedBytes: ByteArray, passphrase: String): ByteArray {
        if (!isEncryptedBackup(encryptedBytes)) {
            throw InvalidBackupPasswordException()
        }
        val saltStart = BACKUP_MAGIC_HEADER.size
        val ivStart = saltStart + PBKDF2_SALT_BYTES
        val cipherStart = ivStart + GCM_IV_BYTES
        val salt = encryptedBytes.copyOfRange(saltStart, ivStart)
        val iv = encryptedBytes.copyOfRange(ivStart, cipherStart)
        val cipherBytes = encryptedBytes.copyOfRange(cipherStart, encryptedBytes.size)

        return try {
            val key = deriveBackupKey(passphrase, salt)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(cipherBytes)
        } catch (_: AEADBadTagException) {
            throw InvalidBackupPasswordException()
        } catch (_: GeneralSecurityException) {
            throw InvalidBackupPasswordException()
        }
    }
}
