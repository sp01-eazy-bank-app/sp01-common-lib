package com.bank.iolog.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.bank.iolog.repository",
        entityManagerFactoryRef = "ioLoggerEntityManagerFactory",
        transactionManagerRef = "ioLoggerTransactionManager"
)
public class IOLoggerDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.iologger-datasource")
    public DataSourceProperties ioLoggerDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "ioLoggerDataSource")
    public DataSource dataSource(@Qualifier("ioLoggerDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "ioLoggerEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean ioLoggerEntityManagerFactory(
            @Qualifier("ioLoggerDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("com.bank.iolog.entity");
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        emf.setJpaPropertyMap(props);

        return emf;
    }

    @Bean(name = "ioLoggerTransactionManager")
    public JpaTransactionManager ioLoggerTransactionManager(
            @Qualifier("ioLoggerEntityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }
}
