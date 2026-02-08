package ashtein.trongate

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class TronGateApplication

fun main(args: Array<String>) {
	runApplication<TronGateApplication>(*args)
}
