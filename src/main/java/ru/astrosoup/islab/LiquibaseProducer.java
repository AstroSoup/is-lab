package ru.astrosoup.islab;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import liquibase.integration.jakarta.cdi.CDILiquibaseConfig;
import liquibase.integration.jakarta.cdi.annotations.LiquibaseType;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;



import javax.sql.DataSource;

@ApplicationScoped
public class LiquibaseProducer {
    @Resource(lookup = "java:jboss/datasources/studs")
    private DataSource dataSource;

    @Produces
    @LiquibaseType
    public CDILiquibaseConfig config() {
        CDILiquibaseConfig config = new CDILiquibaseConfig();
        config.setChangeLog("db/changelog/changelog-master.yaml");
        return config;
    }

    @Produces
    @LiquibaseType
    public DataSource liquibaseDataSource() {
        return dataSource;
    }

    @Produces
    @LiquibaseType
    public ResourceAccessor resourceAccessor() {
        return new ClassLoaderResourceAccessor(getClass().getClassLoader());
    }
}