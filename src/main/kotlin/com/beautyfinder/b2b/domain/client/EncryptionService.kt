package com.beautyfinder.b2b.domain.client

interface EncryptionService {
    fun encrypt(plaintext: String): String
    fun decrypt(ciphertext: String): String
    fun hash(plaintext: String): String
}
