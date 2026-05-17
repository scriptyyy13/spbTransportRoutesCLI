package scriptyyy.bd.cli.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource
import org.springframework.jdbc.datasource.DriverManagerDataSource

@Configuration
class DataSourceConfig {

    @Bean
    fun dataSource(): DataSource {
        val ds = DriverManagerDataSource()
        ds.setDriverClassName("org.postgresql.Driver")
        ds.url = EnvConfig.getDbUrl()
        ds.username = EnvConfig.getDbUser()
        ds.password = EnvConfig.getDbPassword()
        return ds
    }
}