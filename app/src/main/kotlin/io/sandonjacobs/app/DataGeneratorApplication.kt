package io.sandonjacobs.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class DataGeneratorApplication

fun main(args: Array<String>) {
    runApplication<DataGeneratorApplication>(*args)
} 