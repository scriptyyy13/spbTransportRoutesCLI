package scriptyyy.bd.cli.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = ["scriptyyy.bd.cli.app.repository"])
class JpaConfig {

    @Bean
    fun entityManagerFactory(dataSource: DataSource): LocalContainerEntityManagerFactoryBean {
        val emf = LocalContainerEntityManagerFactoryBean()
        emf.dataSource = dataSource
        emf.setPackagesToScan("scriptyyy.bd.cli.app.entity")
        emf.jpaVendorAdapter = EclipseLinkJpaVendorAdapter()

        val props = mutableMapOf<String, Any>()
        props["eclipselink.ddl-generation"] = "create-or-extend-tables"
        props["eclipselink.ddl-generation.output-mode"] = "database"
        props["eclipselink.logging.level"] = "FINE"
        props["eclipselink.logging.level.sql"] = "FINE"
        props["jakarta.persistence.schema-generation.create-source"] = "metadata"
        emf.setJpaPropertyMap(props)

        return emf
    }

    @Bean
    fun transactionManager(dataSource: DataSource): JpaTransactionManager {
        val tm = JpaTransactionManager()
        tm.entityManagerFactory = entityManagerFactory(dataSource).`object`
        return tm
    }
}