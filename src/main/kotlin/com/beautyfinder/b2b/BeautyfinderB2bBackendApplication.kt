package com.beautyfinder.b2b

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class BeautyfinderB2bBackendApplication

fun main(args: Array<String>) {
    runApplication<BeautyfinderB2bBackendApplication>(*args)
}
