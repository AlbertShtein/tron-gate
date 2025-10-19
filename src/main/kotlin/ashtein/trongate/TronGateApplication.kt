package ashtein.trongate

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TronGateApplication

fun main(args: Array<String>) {
	runApplication<TronGateApplication>(*args)
}
