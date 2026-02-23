package com.beautyfinder.b2b.application.salon

import java.io.InputStream

interface StorageService {
    fun store(inputStream: InputStream, filename: String, contentType: String): String
    fun delete(url: String)
    fun exists(url: String): Boolean
}
