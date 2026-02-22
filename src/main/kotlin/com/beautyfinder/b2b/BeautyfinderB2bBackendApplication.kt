package com.beautyfinder.b2b

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class BeautyfinderB2bBackendApplication

fun main(args: Array<String>) {
    runApplication<BeautyfinderB2bBackendApplication>(*args)
}
