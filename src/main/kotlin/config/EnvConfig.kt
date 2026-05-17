package scriptyyy.bd.cli.app.config

import io.github.cdimascio.dotenv.dotenv
import org.springframework.context.annotation.Configuration

@Configuration
class EnvConfig {
    companion object {
        private val env = dotenv {
            ignoreIfMissing = true
        }

        fun getDbUrl(): String = env["DB_URL"] ?: "jdbc:postgresql://localhost:5432/spb_transport"
        fun getDbUser(): String = env["DB_USER"] ?: "postgres"
        fun getDbPassword(): String = env["DB_PASSWORD"] ?: "admin"
        fun getApiToken(): String = env["API_TOKEN"] ?: ""
        fun getApiUrl(): String = env["API_URL"] ?: "https://data.gov.spb.ru/api/v2/datasets/159/versions/latest/data/643/"
    }
}