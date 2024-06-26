/**
 * Copyright 2014-2017 Telefonica Investigación y Desarrollo, S.A.U
 *
 * This file is part of fiware-cygnus (FIWARE project).
 *
 * fiware-cygnus is free software: you can redistribute it and/or modify it under the terms of the GNU Affero
 * General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * fiware-cygnus is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with fiware-cygnus. If not, see
 * http://www.gnu.org/licenses/.
 *
 * For those usages not covered by the GNU Affero General Public License please contact with iot_support at tid dot es
 */

package com.telefonica.iot.cygnus.sinks;

import java.util.ArrayList;
import java.util.Arrays;

import com.telefonica.iot.cygnus.aggregation.NGSIGenericAggregator;
import com.telefonica.iot.cygnus.aggregation.NGSIGenericColumnAggregator;
import com.telefonica.iot.cygnus.aggregation.NGSIGenericRowAggregator;
import com.telefonica.iot.cygnus.backends.sql.SQLQueryUtils;
import com.telefonica.iot.cygnus.backends.sql.SQLBackendImpl;
import com.telefonica.iot.cygnus.backends.sql.Enum.SQLInstance;
import com.telefonica.iot.cygnus.utils.CommonConstants;
import com.telefonica.iot.cygnus.utils.NGSICharsets;
import com.telefonica.iot.cygnus.utils.NGSIConstants;
import com.telefonica.iot.cygnus.utils.NGSIUtils;
import org.apache.flume.Context;

import com.telefonica.iot.cygnus.errors.CygnusBadConfiguration;
import com.telefonica.iot.cygnus.errors.CygnusBadContextData;
import com.telefonica.iot.cygnus.errors.CygnusCappingError;
import com.telefonica.iot.cygnus.errors.CygnusExpiratingError;
import com.telefonica.iot.cygnus.errors.CygnusPersistenceError;
import com.telefonica.iot.cygnus.errors.CygnusRuntimeError;
import com.telefonica.iot.cygnus.interceptors.NGSIEvent;
import com.telefonica.iot.cygnus.log.CygnusLogger;

/**
 *
 * @author frb
 * 
 * Detailed documentation can be found at:
 * https://github.com/telefonicaid/fiware-cygnus/blob/master/doc/flume_extensions_catalogue/ngsi_oracle_sink.md
 */
public class NGSIOracleSQLSink extends NGSISink {
    
    private static final String DEFAULT_ROW_ATTR_PERSISTENCE = "row";
    private static final String DEFAULT_PASSWORD = "oracle";
    private static final String DEFAULT_PORT = "1521";
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_USER_NAME = "system";
    private static final String DEFAULT_DATABASE = "xe";
    private static final int DEFAULT_MAX_POOL_SIZE = 3;
    private static final int DEFAULT_MAX_POOL_IDLE = 2;
    private static final int DEFAULT_MIN_POOL_IDLE = 0;
    private static final int DEFAULT_MIN_POOL_IDLE_TIME_MILLIS = 10000;
    private static final String DEFAULT_ATTR_NATIVE_TYPES = "false";
    //private static final String ORACLE_DRIVER_NAME = "oracle.jdbc.OracleDriver";
    private static final String ORACLE_DRIVER_NAME = "oracle.jdbc.driver.OracleDriver";    
    private static final SQLInstance ORACLE_INSTANCE_NAME = SQLInstance.ORACLE;
    private static final String DEFAULT_FIWARE_SERVICE = "default";
    private static final String ESCAPED_DEFAULT_FIWARE_SERVICE = "default_service";
    private static final String DEFAULT_LAST_DATA_MODE = "insert";
    private static final String DEFAULT_LAST_DATA_TABLE_SUFFIX = "_last_data";
    private static final String DEFAULT_LAST_DATA_UNIQUE_KEY = NGSIConstants.ENTITY_ID;
    private static final String DEFAULT_LAST_DATA_TIMESTAMP_KEY = NGSIConstants.RECV_TIME;
    private static final String DEFAULT_LAST_DATA_SQL_TS_FORMAT = "YYYY-MM-DD HH24:MI:SS.MS";
    private static final int DEFAULT_MAX_LATEST_ERRORS = 100;
    private static final String DEFAULT_ORACLE_NLS_TIMESTAMP_FORMAT = "YYYY-MM-DD HH24:MI:SS.FF6";
    private static final String DEFAULT_ORACLE_NLS_TIMESTAMP_TZ_FORMAT = "YYYY-MM-DD\"T\"HH24:MI:SS.FF6 TZR";
    private static final String DEFAULT_ORACLE_LOCATOR = "false";
    private static final int DEFAULT_ORACLE_MAJOR_VERSION = 11;

    private static final CygnusLogger LOGGER = new CygnusLogger(NGSIOracleSQLSink.class);
    private String oracleHost;
    private String oraclePort;
    private String oracleUsername;
    private String oraclePassword;
    private String oracleDatabase;    
    private int maxPoolSize;
    private int maxPoolIdle;
    private int minPoolIdle;
    private int minPoolIdleTimeMillis;
    private boolean rowAttrPersistence;
    private SQLBackendImpl oracleSQLPersistenceBackend;
    private boolean attrNativeTypes;
    private boolean attrMetadataStore;
    private String oracleOptions;
    private boolean persistErrors;
    private String lastDataMode;
    private String lastDataTableSuffix;
    private String lastDataUniqueKey;
    private String lastDataTimeStampKey;
    private String lastDataSQLTimestampFormat;
    private int maxLatestErrors;
    private String nlsTimestampFormat;
    private String nlsTimestampTzFormat;
    private boolean oracleLocator;
    private int oracleMajorVersion;
    private int oracleMaxNameLength;

    /**
     * Constructor.
     */
    public NGSIOracleSQLSink() {
        super();
    } // NGSIOracleSQLSink
    
    /**
     * Gets the Oracle host. It is protected due to it is only required for testing purposes.
     * @return The OracleSQL host
     */
    protected String getOracleSQLHost() {
        return oracleHost;
    } // getOracleSQLHost
    
    /**
     * Gets the OracleSQL port. It is protected due to it is only required for testing purposes.
     * @return The OracleSQL port
     */
    protected String getOracleSQLPort() {
        return oraclePort;
    } // getOracleSQLPort

    /**
     * Gets the oracle database. It is protected due to it is only required for testing purposes.
     * @return The oracle database
     */
    protected String getOracleSQLDatabase() {
        return oracleDatabase;
    } // getOracleSQLDatabase

    /**
     * Gets the OracleSQL username. It is protected due to it is only required for testing purposes.
     * @return The OracleSQL username
     */
    protected String getOracleSQLUsername() {
        return oracleUsername;
    } // getOracleSQLUsername
    
    /**
     * Gets the OracleSQL password. It is protected due to it is only required for testing purposes.
     * @return The OracleSQL password
     */
    protected String getOracleSQLPassword() {
        return oraclePassword;
    } // getOracleSQLPassword
    
    /**
     * Returns if the attribute persistence is row-based. It is protected due to it is only required for testing
     * purposes.
     * @return True if the attribute persistence is row-based, false otherwise
     */
    protected boolean getRowAttrPersistence() {
        return rowAttrPersistence;
    } // getRowAttrPersistence

    /**
     * Gets the OracleSQL options. It is protected due to it is only required for testing purposes.
     * @return The OracleSQL options
     */
    protected String getOracleSQLOptions() {
        return oracleOptions;
    } // getOracleSQLOptions

    /**
     * Returns the persistence backend. It is protected due to it is only required for testing purposes.
     * @return The persistence backend
     */
    protected SQLBackendImpl getPersistenceBackend() {
        return oracleSQLPersistenceBackend;
    } // getPersistenceBackend
    
    /**
     * Sets the persistence backend. It is protected due to it is only required for testing purposes.
     * @param persistenceBackend
     */
    protected void setPersistenceBackend(SQLBackendImpl persistenceBackend) {
        this.oracleSQLPersistenceBackend = persistenceBackend;
    } // setPersistenceBackend


   /**
     * Returns if the attribute value will be native or stringfy. It will be stringfy due to backward compatibility
     * purposes.
     * @return True if the attribute value will be native, false otherwise
     */
    protected boolean getNativeAttrTypes() {
        return attrNativeTypes;
    } // attrNativeTypes
    
    @Override
    public void configure(Context context) {
        oracleHost = context.getString("oracle_host", DEFAULT_HOST);
        LOGGER.debug("[" + this.getName() + "] Reading configuration (oracle_host=" + oracleHost + ")");
        oraclePort = context.getString("oracle_port", DEFAULT_PORT);
        int intPort = Integer.parseInt(oraclePort);

        if ((intPort <= 0) || (intPort > 65535)) {
            invalidConfiguration = true;
            LOGGER.warn("[" + this.getName() + "] Invalid configuration (oracle_port=" + oraclePort + ") "
                    + "must be between 0 and 65535");
        } else {
            LOGGER.debug("[" + this.getName() + "] Reading configuration (oracle_port=" + oraclePort + ")");
        }  // if else

        oracleDatabase = context.getString("oracle_database", DEFAULT_DATABASE);
        LOGGER.debug("[" + this.getName() + "] Reading configuration (oracle_database=" + oracleDatabase + ")");
        oracleUsername = context.getString("oracle_username", DEFAULT_USER_NAME);
        LOGGER.debug("[" + this.getName() + "] Reading configuration (oracle_username=" + oracleUsername + ")");
        // FIXME: oraclePassword should be read encrypted and decoded here
        oraclePassword = context.getString("oracle_password", DEFAULT_PASSWORD);
        LOGGER.debug("[" + this.getName() + "] Reading configuration (oracle_password=" + oraclePassword + ")");

        maxPoolSize = context.getInteger("oracle_maxPoolSize", DEFAULT_MAX_POOL_SIZE);
        LOGGER.debug("[" + this.getName() + "] Reading configuration (oracle_maxPoolSize=" + maxPoolSize + ")");

        maxPoolIdle = context.getInteger("oracle_maxPoolIdle", DEFAULT_MAX_POOL_IDLE);
        LOGGER.debug("[" + this.getName() + "] Reading configuration (oracle_maxPoolIdle=" + maxPoolIdle + ")");

        minPoolIdle = context.getInteger("oracle_minPoolIdle", DEFAULT_MIN_POOL_IDLE);
        LOGGER.debug("[" + this.getName() + "] Reading configuration (oracle_minPoolIdle=" + minPoolIdle + ")");

        minPoolIdleTimeMillis = context.getInteger("oracle_minPoolIdleTimeMillis", DEFAULT_MIN_POOL_IDLE_TIME_MILLIS);
        LOGGER.debug("[" + this.getName() + "] Reading configuration (oracle_minPoolIdleTimeMillis=" + minPoolIdleTimeMillis + ")");

        rowAttrPersistence = context.getString("attr_persistence", DEFAULT_ROW_ATTR_PERSISTENCE).equals("row");
        String persistence = context.getString("attr_persistence", DEFAULT_ROW_ATTR_PERSISTENCE);

        if (persistence.equals("row") || persistence.equals("column")) {
            LOGGER.debug("[" + this.getName() + "] Reading configuration (attr_persistence="
                + persistence + ")");
        } else {
            invalidConfiguration = true;
            LOGGER.warn("[" + this.getName() + "] Invalid configuration (attr_persistence="
                + persistence + ") must be 'row' or 'column'");
        }  // if else

        String attrNativeTypesStr = context.getString("attr_native_types", DEFAULT_ATTR_NATIVE_TYPES);
        if (attrNativeTypesStr.equals("true") || attrNativeTypesStr.equals("false")) {
            attrNativeTypes = Boolean.valueOf(attrNativeTypesStr);
            LOGGER.debug("[" + this.getName() + "] Reading configuration (attr_native_types=" + attrNativeTypesStr + ")");
        } else {
            invalidConfiguration = true;
            LOGGER.debug("[" + this.getName() + "] Invalid configuration (attr_native_types="
                + attrNativeTypesStr + ") -- Must be 'true' or 'false'");
        } // if else

        String attrMetadataStoreStr = context.getString("attr_metadata_store", "true");

        if (attrMetadataStoreStr.equals("true") || attrMetadataStoreStr.equals("false")) {
            attrMetadataStore = Boolean.parseBoolean(attrMetadataStoreStr);
            LOGGER.debug("[" + this.getName() + "] Reading configuration (attr_metadata_store="
                    + attrMetadataStore + ")");
        } else {
            invalidConfiguration = true;
            LOGGER.debug("[" + this.getName() + "] Invalid configuration (attr_metadata_store="
                    + attrMetadataStoreStr + ") -- Must be 'true' or 'false'");
        } // if else

        oracleOptions = context.getString("oracle_options", null);
        LOGGER.debug("[" + this.getName() + "] Reading configuration (oracle_options=" + oracleOptions + ")");

        String persistErrorsStr = context.getString("persist_errors", "true");

        if (persistErrorsStr.equals("true") || persistErrorsStr.equals("false")) {
            persistErrors = Boolean.parseBoolean(persistErrorsStr);
            LOGGER.debug("[" + this.getName() + "] Reading configuration (persist_errors="
                    + persistErrors + ")");
        } else {
            invalidConfiguration = true;
            LOGGER.debug("[" + this.getName() + "] Invalid configuration (persist_errors="
                    + persistErrorsStr + ") -- Must be 'true' or 'false'");
        } // if else

        lastDataMode = context.getString("last_data_mode", DEFAULT_LAST_DATA_MODE);

        if (lastDataMode.equals("upsert") || lastDataMode.equals("insert") || lastDataMode.equals("both")) {
            LOGGER.debug("[" + this.getName() + "] Reading configuration (last_data_mode="
                    + lastDataMode + ")");
        } else {
            invalidConfiguration = true;
            LOGGER.debug("[" + this.getName() + "] Invalid configuration (last_data_mode="
                    + lastDataMode + ") -- Must be 'upsert', 'insert' or 'both'");
        } // if else

        lastDataTableSuffix = context.getString("last_data_table_suffix", DEFAULT_LAST_DATA_TABLE_SUFFIX);
        LOGGER.debug("[" + this.getName() + "] Reading configuration (last_data_table_suffix="
                + lastDataTableSuffix + ")");

        lastDataUniqueKey = context.getString("last_data_unique_key", DEFAULT_LAST_DATA_UNIQUE_KEY);
        LOGGER.debug("[" + this.getName() + "] Reading configuration (last_data_unique_key="
                + lastDataUniqueKey + ")");

        lastDataTimeStampKey = context.getString("last_data_timestamp_key", DEFAULT_LAST_DATA_TIMESTAMP_KEY);
        LOGGER.debug("[" + this.getName() + "] Reading configuration (last_data_timestamp_key="
                + lastDataTimeStampKey + ")");

        lastDataSQLTimestampFormat = context.getString("last_data_sql_timestamp_format", DEFAULT_LAST_DATA_SQL_TS_FORMAT);
        LOGGER.debug("[" + this.getName() + "] Reading configuration (last_data_sql_timestamp_format="
                + lastDataSQLTimestampFormat + ")");

        nlsTimestampFormat = context.getString("nls_timestamp_format", DEFAULT_ORACLE_NLS_TIMESTAMP_FORMAT);
        LOGGER.debug("[" + this.getName() + "] Reading configuration (nls_timestamp_format="
                + nlsTimestampFormat + ")");

        nlsTimestampTzFormat = context.getString("nls_timestamp_tz_format", DEFAULT_ORACLE_NLS_TIMESTAMP_TZ_FORMAT);
        LOGGER.debug("[" + this.getName() + "] Reading configuration (nls_timestamp_tz_format="
                + nlsTimestampTzFormat + ")");

        String oracleLocatorStr = context.getString("oracle_locator", DEFAULT_ORACLE_LOCATOR);
        if (oracleLocatorStr.equals("true") || oracleLocatorStr.equals("false")) {
            oracleLocator = Boolean.valueOf(oracleLocatorStr);
            LOGGER.debug("[" + this.getName() + "] Reading configuration (oracle_locator=" + oracleLocatorStr + ")");
        } else {
            invalidConfiguration = true;
            LOGGER.debug("[" + this.getName() + "] Invalid configuration (oracle_locator="
                + oracleLocatorStr + ") -- Must be 'true' or 'false'");
        } // if else

        oracleMajorVersion = context.getInteger("oracle_major_version", DEFAULT_ORACLE_MAJOR_VERSION);
        LOGGER.debug("[" + this.getName() + "] Reading configuration (oracle_major_version=" + oracleMajorVersion + ")");
        if (oracleMajorVersion < 12) {
            oracleMaxNameLength = NGSIConstants.ORACLE11_MAX_NAME_LEN;
        } else {
            oracleMaxNameLength = NGSIConstants.ORACLE12_MAX_NAME_LEN;
        }

        maxLatestErrors = context.getInteger("max_latest_errors", DEFAULT_MAX_LATEST_ERRORS);
        LOGGER.debug("[" + this.getName() + "] Reading configuration (max_latest_errors=" + maxLatestErrors + ")");

        super.configure(context);
    } // configure

    @Override
    public void start() {
        try {
            createPersistenceBackend(oracleHost, oraclePort, oracleUsername, oraclePassword, maxPoolSize, maxPoolIdle, minPoolIdle, minPoolIdleTimeMillis, oracleOptions, persistErrors, maxLatestErrors);
            LOGGER.debug("[" + this.getName() + "] OracleSQL persistence backend created");
        } catch (Exception e) {
            String configParams = " oracleHost " + oracleHost + " oraclePort " + oraclePort + " oracleUsername " + oracleUsername + " oraclePassword " + oraclePassword + " maxPoolSize " + maxPoolSize + " maxPoolIdle " + maxPoolIdle + " minPoolIdle " + minPoolIdle + " minPoolIdleTimeMillis " + minPoolIdleTimeMillis + " oracleOptions " + oracleOptions + " persistErrors " + persistErrors + " maxLatestErrors " + maxLatestErrors;
            LOGGER.error("Error while creating the OracleSQL persistence backend. " +
                         "Config params= " + configParams +
                         "Details=" + e.getMessage() +
                         "Stack trace: " + Arrays.toString(e.getStackTrace()));
        } // try catch
        
        super.start();
    } // start

    @Override
    public void stop() {
        super.stop();
        if (oracleSQLPersistenceBackend != null) oracleSQLPersistenceBackend.close();
    } // stop

    /**
     * Initialices a lazy singleton to share among instances on JVM
     */
    private void createPersistenceBackend(String sqlHost, String sqlPort, String sqlUsername, String sqlPassword, int maxPoolSize, int maxPoolIdle, int minPoolIdle, int minPoolIdleTimeMillis, String sqlOptions, boolean persistErrors, int maxLatestErrors) {
        if (oracleSQLPersistenceBackend == null) {
            oracleSQLPersistenceBackend = new SQLBackendImpl(sqlHost, sqlPort, sqlUsername, sqlPassword, maxPoolSize, maxPoolIdle, minPoolIdle, minPoolIdleTimeMillis, ORACLE_INSTANCE_NAME, ORACLE_DRIVER_NAME, sqlOptions, persistErrors, maxLatestErrors);
            oracleSQLPersistenceBackend.setNlsTimestampFormat(nlsTimestampFormat);
            oracleSQLPersistenceBackend.setNlsTimestampTzFormat(nlsTimestampTzFormat);
        }
    }

    @Override
    void persistBatch(NGSIBatch batch)
        throws CygnusBadConfiguration, CygnusPersistenceError, CygnusRuntimeError, CygnusBadContextData {
        if (batch == null) {
            LOGGER.debug("[" + this.getName() + "] Null batch, nothing to do");
            return;
        } // if
 
        // Iterate on the destinations
        batch.startIterator();
        
        while (batch.hasNext()) {
            String destination = batch.getNextDestination();
            LOGGER.debug("[" + this.getName() + "] Processing sub-batch regarding the "
                    + destination + " destination");

            // Get the events within the current sub-batch
            ArrayList<NGSIEvent> events = batch.getNextEvents();
            
            // Get an aggregator for this destination and initialize it
            NGSIGenericAggregator aggregator = getAggregator(rowAttrPersistence);
            aggregator.setService(events.get(0).getServiceForNaming(enableNameMappings));
            aggregator.setServicePathForData(events.get(0).getServicePathForData());
            aggregator.setServicePathForNaming(events.get(0).getServicePathForNaming(enableNameMappings));
            aggregator.setEntityForNaming(events.get(0).getEntityForNaming(enableNameMappings, enableEncoding));
            aggregator.setEntityType(events.get(0).getEntityTypeForNaming(enableNameMappings));
            aggregator.setAttribute(events.get(0).getAttributeForNaming(enableNameMappings));
            aggregator.setSchemeName(buildSchemaName(aggregator.getService(), aggregator.getServicePathForNaming()));
            aggregator.setDbName(buildDbName(aggregator.getService()));
            aggregator.setTableName(buildTableName(aggregator.getServicePathForNaming(), aggregator.getEntityForNaming(), aggregator.getEntityType(), aggregator.getAttribute()));
            aggregator.setAttrNativeTypes(attrNativeTypes);
            aggregator.setAttrMetadataStore(attrMetadataStore);
            aggregator.setEnableGeoParseOracle(true);
            aggregator.setEnableGeoParseOracleLocator(oracleLocator);
            aggregator.setEnableNameMappings(enableNameMappings);
            aggregator.setLastDataMode(lastDataMode);
            aggregator.setLastDataUniqueKey(lastDataUniqueKey);
            aggregator.setLastDataTimestampKey(lastDataTimeStampKey);
            aggregator.initialize(events.get(0));
            for (NGSIEvent event : events) {
                aggregator.aggregate(event);
            } // for
            LOGGER.debug("[" + getName() + "] adding event to aggregator object  (name=" +
                         SQLQueryUtils.getFieldsForInsert(aggregator.getAggregation().keySet(), SQLQueryUtils.ORACLE_FIELDS_MARK) + ", values=" +
                         SQLQueryUtils.getValuesForInsert(aggregator.getAggregation(), attrNativeTypes) + ")");
            // Persist the aggregation
            persistAggregation(aggregator);
            batch.setNextPersisted(true);
        } // for
    } // persistBatch
    
    @Override
    public void capRecords(NGSIBatch batch, long maxRecords) throws CygnusCappingError {
        if (batch == null) {
            LOGGER.debug("[" + this.getName() + "] Null batch, nothing to do");
            return;
        } // if

        // Iterate on the destinations
        batch.startIterator();
        
        while (batch.hasNext()) {
            // Get the events within the current sub-batch
            ArrayList<NGSIEvent> events = batch.getNextEvents();

            // Get a representative from the current destination sub-batch
            NGSIEvent event = events.get(0);
            
            // Do the capping
            String service = event.getServiceForNaming(enableNameMappings);
            String servicePathForNaming = event.getServicePathForNaming(enableNameMappings);
            String entity = event.getEntityForNaming(enableNameMappings, enableEncoding);
            String entityType = event.getEntityTypeForNaming(enableNameMappings);
            String attribute = event.getAttributeForNaming(enableNameMappings);
            
            try {
                String dbName = buildDbName(service);
                String tableName = buildTableName(servicePathForNaming, entity, entityType, attribute);
                LOGGER.debug("[" + this.getName() + "] Capping resource (maxRecords=" + maxRecords + ",dbName="
                        + dbName + ", tableName=" + tableName + ")");
                oracleSQLPersistenceBackend.capRecords(dbName, null, tableName, maxRecords);
            } catch (CygnusBadConfiguration e) {
                throw new CygnusCappingError("Data capping error", "CygnusBadConfiguration", e.getMessage());
            } catch (CygnusRuntimeError e) {
                throw new CygnusCappingError("Data capping error", "CygnusRuntimeError", e.getMessage());
            } catch (CygnusPersistenceError e) {
                throw new CygnusCappingError("Data capping error", "CygnusPersistenceError", e.getMessage());
            } // try catch
        } // while
    } // capRecords

    @Override
    public void expirateRecords(long expirationTime) throws CygnusExpiratingError {
        LOGGER.debug("[" + this.getName() + "] Expirating records (time=" + expirationTime + ")");
        
        try {
            oracleSQLPersistenceBackend.expirateRecordsCache(expirationTime);
        } catch (CygnusRuntimeError e) {
            throw new CygnusExpiratingError("Data expiration error", "CygnusRuntimeError", e.getMessage());
        } catch (CygnusPersistenceError e) {
            throw new CygnusExpiratingError("Data expiration error", "CygnusPersistenceError", e.getMessage());
        } // try catch
    } // expirateRecords
    
    protected NGSIGenericAggregator getAggregator(boolean rowAttrPersistence) {
        if (rowAttrPersistence) {
            return new NGSIGenericRowAggregator();
        } else {
            return new NGSIGenericColumnAggregator();
        } // if else
    } // getAggregator
    
    private void persistAggregation(NGSIGenericAggregator aggregator)
        throws CygnusPersistenceError, CygnusRuntimeError, CygnusBadContextData {

        String schemaName = aggregator.getSchemeName(enableLowercase);
        String dbName = aggregator.getDbName(enableLowercase);        
        String tableName = aggregator.getTableName(enableLowercase);

        // Escape a syntax error in SQL
        if (schemaName.equals(DEFAULT_FIWARE_SERVICE)) {
            schemaName = ESCAPED_DEFAULT_FIWARE_SERVICE;
        }

        if (lastDataMode.equals("upsert") || lastDataMode.equals("both")) {
            if (rowAttrPersistence) {
                LOGGER.warn("[" + this.getName() + "] no upsert due to row mode");
            } else {
                LOGGER.warn("[" + this.getName() + "] no upsert or both mode avaiable for oracle");
            }
        }

        if (lastDataMode.equals("insert")) {
            try {
                // Try to insert without create database and table before
                oracleSQLPersistenceBackend.insertTransaction(aggregator.getAggregationToPersist(),
                                                              dbName,
                                                              schemaName,
                                                              tableName,
                                                              attrNativeTypes);
            } catch (CygnusPersistenceError | CygnusBadContextData | CygnusRuntimeError ex) {
                // creating the database and the table has only sense if working in row mode, in column node
                // everything must be provisioned in advance
                if (rowAttrPersistence) {
                    // This case will create a false error entry in error table
                    String fieldsForCreate = SQLQueryUtils.getFieldsForCreate(aggregator.getAggregationToPersist(),
                                                                              ORACLE_INSTANCE_NAME);
                    try {
                        // Try to insert without create database before
                        oracleSQLPersistenceBackend.createTable(dbName, schemaName, tableName, fieldsForCreate);
                    } catch (CygnusRuntimeError | CygnusPersistenceError ex2) {
                        oracleSQLPersistenceBackend.createDestination(schemaName);
                        oracleSQLPersistenceBackend.createTable(dbName, schemaName, tableName, fieldsForCreate);
                    } // catch
                    oracleSQLPersistenceBackend.insertTransaction(aggregator.getAggregationToPersist(),
                                                                  dbName,
                                                                  schemaName,
                                                                  tableName,
                                                                  attrNativeTypes);
                } else {
                    // column
                    throw ex;
                }
            } // catch
        }
    } // persistAggregation
    
    /**
     * Creates a OracleSQL DB name given the FIWARE service.
     * @param service
     * @return The OracleSQL DB name
     * @throws CygnusBadConfiguration
     */
    protected String buildDbName(String service) throws CygnusBadConfiguration {
        String name = null;

        if (enableEncoding) {
            switch(dataModel) {
                case DMBYENTITYDATABASE:
                case DMBYENTITYDATABASESCHEMA:
                case DMBYENTITYTYPEDATABASE:
                case DMBYENTITYTYPEDATABASESCHEMA:
                case DMBYFIXEDENTITYTYPEDATABASE:
                case DMBYFIXEDENTITYTYPEDATABASESCHEMA:
                    if (service != null)
                        name = NGSICharsets.encodeOracleSQL(service);
                    break;
                default:
                    name = oracleDatabase;
            }
        } else {
            switch(dataModel) {
                case DMBYENTITYDATABASE:
                case DMBYENTITYDATABASESCHEMA:
                case DMBYENTITYTYPEDATABASE:
                case DMBYENTITYTYPEDATABASESCHEMA:
                case DMBYFIXEDENTITYTYPEDATABASE:
                case DMBYFIXEDENTITYTYPEDATABASESCHEMA:
                    if (service != null)
                        name = NGSIUtils.encode(service, false, true);
                    break;
                default:
                    name = oracleDatabase;
            }
        } // if else
        if (name.length() > this.oracleMaxNameLength) {
            throw new CygnusBadConfiguration("Building database name '" + name
                    + "' and its length is greater than " + this.oracleMaxNameLength);
        } // if

        return name;
    } // buildDbName

    /**
     * Creates a OracleSQL scheme name given the FIWARE service.
     * @param service
     * @return The oracleSQL scheme name
     * @throws CygnusBadConfiguration
     */
    public String buildSchemaName(String service, String subService) throws CygnusBadConfiguration {
        String name;

        if (enableEncoding) {
            switch(dataModel) {
                case DMBYENTITYDATABASESCHEMA:
                case DMBYENTITYTYPEDATABASESCHEMA:
                case DMBYFIXEDENTITYTYPEDATABASESCHEMA:
                    name = NGSICharsets.encodeOracleSQL(subService);
                    break;
                default:
                    name = NGSICharsets.encodeOracleSQL(service);
            }
        } else {
            switch(dataModel) {
                case DMBYENTITYDATABASESCHEMA:
                case DMBYENTITYTYPEDATABASESCHEMA:
                case DMBYFIXEDENTITYTYPEDATABASESCHEMA:
                    name = NGSIUtils.encode(subService, false, true);
                    break;
                default:
                    name = NGSIUtils.encode(service, false, true);
            }
        } // if else

        if (name.length() > this.oracleMaxNameLength) {
            throw new CygnusBadConfiguration("Building schema name '" + name
                    + "' and its length is greater than " + this.oracleMaxNameLength);
        } // if

        return name;
    } // buildSchemaName
    
    /**
     * Creates a OracleSQL table name given the FIWARE service path, the entity and the attribute.
     * @param servicePath
     * @param entity
     * @param attribute
     * @return The OracleSQL table name
     * @throws CygnusBadConfiguration
     */
    protected String buildTableName(String servicePath, String entity, String entityType, String attribute)
            throws CygnusBadConfiguration {
        String name;

        if (enableEncoding) {
            switch (dataModel) {
                case DMBYSERVICEPATH:
                    name = NGSICharsets.encodeOracleSQL(servicePath);
                    break;
                case DMBYENTITY:
                case DMBYENTITYDATABASE:
                    name = NGSICharsets.encodeOracleSQL(servicePath)
                            + CommonConstants.CONCATENATOR
                            + NGSICharsets.encodeOracleSQL(entity);
                    break;
                case DMBYENTITYTYPE:
                case DMBYENTITYTYPEDATABASE:
                    name = NGSICharsets.encodeOracleSQL(servicePath)
                            + CommonConstants.CONCATENATOR
                            + NGSICharsets.encodeOracleSQL(entityType);
                    break;
                case DMBYATTRIBUTE:
                    name = NGSICharsets.encodeOracleSQL(servicePath)
                            + CommonConstants.CONCATENATOR
                            + NGSICharsets.encodeOracleSQL(entity)
                            + CommonConstants.CONCATENATOR
                            + NGSICharsets.encodeOracleSQL(attribute);
                    break;
                default:
                    throw new CygnusBadConfiguration("Unknown data model '" + dataModel.toString()
                            + "'. Please, use dm-by-service-path, dm-by-entity or dm-by-attribute");
            } // switch
        } else {
            switch (dataModel) {
                case DMBYSERVICEPATH:
                    if (servicePath.equals("/")) {
                        throw new CygnusBadConfiguration("Default service path '/' cannot be used with "
                                + "dm-by-service-path data model");
                    } // if
                    
                    name = NGSIUtils.encode(servicePath, true, false);
                    break;
                case DMBYENTITYDATABASE:
                case DMBYENTITY:
                    String truncatedServicePath = NGSIUtils.encode(servicePath, true, false);
                    name = (truncatedServicePath.isEmpty() ? "" : truncatedServicePath + '_')
                            + NGSIUtils.encode(entity, false, true);
                    break;
                case DMBYENTITYTYPEDATABASE:
                case DMBYENTITYTYPE:
                    truncatedServicePath = NGSIUtils.encode(servicePath, true, false);
                    name = (truncatedServicePath.isEmpty() ? "" : truncatedServicePath + '_')
                            + NGSIUtils.encode(entityType, false, true);
                    break;
                case DMBYATTRIBUTE:
                    truncatedServicePath = NGSIUtils.encode(servicePath, true, false);
                    name = (truncatedServicePath.isEmpty() ? "" : truncatedServicePath + '_')
                            + NGSIUtils.encode(entity, false, true)
                            + '_' + NGSIUtils.encode(attribute, false, true);
                    break;
                default:
                    throw new CygnusBadConfiguration("Unknown data model '" + dataModel.toString()
                            + "'. Please, use DMBYSERVICEPATH, DMBYENTITY, DMBYENTITYTYPE or DMBYATTRIBUTE");
            } // switch
        } // if else

        if (name.length() > this.oracleMaxNameLength) {
            throw new CygnusBadConfiguration("Building table name '" + name
                    + "' and its length is greater than " + this.oracleMaxNameLength);
        } // if

        return name;
    } // buildTableName

} // NGSIOracleSQLSink
