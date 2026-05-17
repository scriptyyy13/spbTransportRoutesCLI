package scriptyyy.bd.cli.app

import org.springframework.beans.factory.getBean
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    val context = SpringApplication.run(Application::class.java, *args)
    val cli = context.getBean<ConsoleCli>()
    cli.run()
}