package com.beautyfinder.b2b.infrastructure.security

import com.beautyfinder.b2b.domain.client.EncryptionService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Service
class AesEncryptionService(
    @Value("\${APP_ENCRYPTION_KEY:}") private val encryptionKeyBase64: String,
) : EncryptionService {

    private val log = LoggerFactory.getLogger(AesEncryptionService::class.java)
    private val secureRandom = SecureRandom()

    private val secretKey: SecretKeySpec by lazy {
        if (encryptionKeyBase64.isBlank()) {
            log.warn("APP_ENCRYPTION_KEY not set, using dev fallback key. DO NOT USE IN PRODUCTION!")
            // 32-byte dev key (base64-encoded)
            val devKey = ByteArray(32) { it.toByte() }
            SecretKeySpec(devKey, ALGORITHM)
        } else {
            val keyBytes = Base64.getDecoder().decode(encryptionKeyBase64)
            require(keyBytes.size == 32) { "Encryption key must be 256 bits (32 bytes)" }
            SecretKeySpec(keyBytes, ALGORITHM)
        }
    }

    override fun encrypt(plaintext: String): String {
        val iv = ByteArray(IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Format: iv[12] + ciphertext + authTag[16]
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    override fun decrypt(ciphertext: String): String {
        val combined = Base64.getDecoder().decode(ciphertext)

        val iv = combined.copyOfRange(0, IV_LENGTH)
        val encryptedBytes = combined.copyOfRange(IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))

        val plainBytes = cipher.doFinal(encryptedBytes)
        return String(plainBytes, Charsets.UTF_8)
    }

    override fun hash(plaintext: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(plaintext.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH_BITS = 128
    }
}
