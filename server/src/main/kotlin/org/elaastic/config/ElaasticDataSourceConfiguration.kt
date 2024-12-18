/*
 *
 * Elaastic - formative assessment system
 * Copyright (C) 2019. University Toulouse 1 Capitole, University Toulouse 3 Paul Sabatier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
package org.elaastic.config

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

/**
 * Configuration of the Elaastic data source.
 * Implementation note: Spring Boot autoconfigure the datasource based on the application.properties file in the standard setup.
 * When we need to configure multiple data sources, we need to do setup all the data source related beans manually.
 * @author John Tranier
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    // We need temporary this list of packages to distinguish writer entities from the rest of Elaastic entities.
    // To avoid to maintain this list, we should introduce two main packages: writer and player (or something else).
    basePackages = [
        "org.elaastic.activity",
        "org.elaastic.ai",
        "org.elaastic.analytics",
        "org.elaastic.assignment",
        "org.elaastic.auth",
        "org.elaastic.bootstrap",
        "org.elaastic.common",
        "org.elaastic.config",
        "org.elaastic.consolidation",
        "org.elaastic.filestore",
        "org.elaastic.moderation",
        "org.elaastic.player",
        "org.elaastic.security",
        "org.elaastic.sequence",
        "org.elaastic.test",
        "org.elaastic.user",
    ],
    entityManagerFactoryRef = "elaasticEntityManagerFactory",
    transactionManagerRef = "elaasticTransactionManager"
)
class ElaasticDataSourceConfiguration {

    /**
     * Extract the properties from the application.properties file
     */
    @Bean
    @ConfigurationProperties("spring.datasource.elaastic")
    fun elaasticDataSourceProperties() = DataSourceProperties()

    /**
     * Create the data source bean
     *
     * Implementation note:
     * The @Primary annotation is used to tell Spring Boot to use this data source as the default one.
     * This is required for Spring boot autoconfiguration to work properly; I haven't investigated why...
     * TODO: Investigate why the @Primary annotation is required ; we do not want different setup for elaasticDataSource and writerDataSource so it should probably be removed; what would require to initialise the missing beans.
     */
    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    fun elaasticDataSource() =
        elaasticDataSourceProperties()
            .initializeDataSourceBuilder()
            .build()

    /**
     * Create the JdbcTemplate bean for Elaastic data source
     * TODO: Check if we need this bean
     */
    @Bean
    fun elaasticJdbcTemplate(@Qualifier("elaasticDataSource") dataSource: DataSource) =
        JdbcTemplate(dataSource)


    /**
     * Create the entity manager factory bean for Elaastic data source
     * It is required to load the JPA properties from the application.properties file, in particular the hibernate
     * naming strategies.
     * We used here the default jpaProperties bean provided by Spring Boot, assuming that we will use the same
     * configuration for both data sources.
     * TODO: Check the role of "packages" in the builder ; can we use "org.elaastic" or shall we be more specific to avoid collision with the writer data source?
     */
    @Bean
    fun elaasticEntityManagerFactory(
        @Qualifier("elaasticDataSource") dataSource: DataSource,
        jpaProperties: JpaProperties,
        builder: EntityManagerFactoryBuilder
    ): LocalContainerEntityManagerFactoryBean {
        return builder.dataSource(dataSource)
            .packages("org.elaastic")
            .persistenceUnit("elaastic")
            .properties(jpaProperties.properties)
            .build()
    }

    @Bean
    fun elaasticTransactionManager(
        @Qualifier("elaasticEntityManagerFactory") elaasticEntityManagerFactory: LocalContainerEntityManagerFactoryBean
    ) = JpaTransactionManager(
        elaasticEntityManagerFactory.`object` ?: throw IllegalArgumentException("Factory must not be null")
    )

    @Bean(initMethod = "migrate")
    @Value("\${spring.flyway.elaastic.locations}")
    fun elaasticFlyway(locations: String): Flyway {
        return Flyway.configure()
            .dataSource(elaasticDataSource())
            .locations(*locations.split(",").toTypedArray())
            .load()
    }

}

