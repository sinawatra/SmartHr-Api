package com.smarthr.smarthr.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Dynamic Routing DataSource that routes queries to Master or Slave database
 * based on Spring's current @Transactional(readOnly = ...) context.
 */
public class TransactionRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        return isReadOnly ? DataSourceType.SLAVE : DataSourceType.MASTER;
    }
}
