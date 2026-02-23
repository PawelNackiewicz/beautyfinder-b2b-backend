package com.beautyfinder.b2b.infrastructure.storage

import com.beautyfinder.b2b.application.salon.StorageService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@Service
class LocalStorageService(
    @Value("\${app.storage.path:./uploads}") private val storagePath: String,
) : StorageService {

    private val log = LoggerFactory.getLogger(LocalStorageService::class.java)

    override fun store(inputStream: InputStream, filename: String, contentType: String): String {
        val targetPath = Paths.get(storagePath).resolve(filename)
        Files.createDirectories(targetPath.parent)
        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)
        log.info("Stored file: {}", targetPath)
        return "/files/$filename"
    }

    override fun delete(url: String) {
        val relativePath = url.removePrefix("/files/")
        val targetPath = Paths.get(storagePath).resolve(relativePath)
        if (Files.exists(targetPath)) {
            Files.delete(targetPath)
            log.info("Deleted file: {}", targetPath)
        }
    }

    override fun exists(url: String): Boolean {
        val relativePath = url.removePrefix("/files/")
        return Files.exists(Paths.get(storagePath).resolve(relativePath))
    }
}
