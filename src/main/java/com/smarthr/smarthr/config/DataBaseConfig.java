package com.smarthr.smarthr.config;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Master-Slave Read/Write Data Source Configuration.
 * 
 * Routes readOnly transactions (@Transactional(readOnly = true)) to the SLAVE database,
 * and read/write transactions (@Transactional) to the MASTER database.
 */
@Configuration
@EnableTransactionManagement
public class DataBaseConfig {

    @Value("${spring.datasource.master.url:${spring.datasource.url:jdbc:postgresql://172.24.100.101:5432/postgres}}")
    private String masterUrl;

    @Value("${spring.datasource.master.username:${spring.datasource.username:postgres}}")
    private String masterUsername;

    @Value("${spring.datasource.master.password:${spring.datasource.password:postgres}}")
    private String masterPassword;

    @Value("${spring.datasource.master.driver-class-name:org.postgresql.Driver}")
    private String masterDriverClassName;

    @Value("${spring.datasource.slave.url:${spring.datasource.master.url:${spring.datasource.url:jdbc:postgresql://172.24.100.101:5432/postgres}}}")
    private String slaveUrl;

    @Value("${spring.datasource.slave.username:${spring.datasource.master.username:${spring.datasource.username:postgres}}}")
    private String slaveUsername;

    @Value("${spring.datasource.slave.password:${spring.datasource.master.password:${spring.datasource.password:postgres}}}")
    private String slavePassword;

    @Value("${spring.datasource.slave.driver-class-name:org.postgresql.Driver}")
    private String slaveDriverClassName;

    @Bean(name = "masterDataSource")
    public DataSource masterDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(masterUrl);
        config.setUsername(masterUsername);
        config.setPassword(masterPassword);
        if (masterDriverClassName != null && !masterDriverClassName.isBlank()) {
            config.setDriverClassName(masterDriverClassName);
        }
        config.setPoolName("Master-HikariPool");
        return new HikariDataSource(config);
    }

    @Bean(name = "slaveDataSource")
    public DataSource slaveDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(slaveUrl);
        config.setUsername(slaveUsername);
        config.setPassword(slavePassword);
        if (slaveDriverClassName != null && !slaveDriverClassName.isBlank()) {
            config.setDriverClassName(slaveDriverClassName);
        }
        config.setPoolName("Slave-HikariPool");
        return new HikariDataSource(config);
    }

    @Bean(name = "routingDataSource")
    public DataSource routingDataSource(
            @Qualifier("masterDataSource") DataSource masterDataSource,
            @Qualifier("slaveDataSource") DataSource slaveDataSource) {
        TransactionRoutingDataSource routingDataSource = new TransactionRoutingDataSource();
        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put(DataSourceType.MASTER, masterDataSource);
        dataSourceMap.put(DataSourceType.SLAVE, slaveDataSource);

        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        return routingDataSource;
    }

    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource(@Qualifier("routingDataSource") DataSource routingDataSource) {
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }
}