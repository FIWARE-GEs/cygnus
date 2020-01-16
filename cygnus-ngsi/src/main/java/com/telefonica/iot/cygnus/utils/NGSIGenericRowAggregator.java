package com.telefonica.iot.cygnus.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.telefonica.iot.cygnus.containers.NotifyContextRequest;
import com.telefonica.iot.cygnus.errors.CygnusBadConfiguration;
import com.telefonica.iot.cygnus.interceptors.NGSIEvent;
import com.telefonica.iot.cygnus.log.CygnusLogger;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * The type Ngsi generic row aggregator.
 */
public class NGSIGenericRowAggregator extends NGSIGenericAggregator{

    // Logger
    private static final CygnusLogger LOGGER = new CygnusLogger(NGSIGenericAggregator.class);

    /**
     * Instantiates a new Ngsi generic row aggregator.
     *
     * @param enableGrouping     the enable grouping flag for initialization
     * @param enableNameMappings the enable name mappings flag for initialization
     * @param enableEncoding     the enable encoding flag for initialization
     * @param enableGeoParse     the enable geo parse flag for initialization
     */
    protected NGSIGenericRowAggregator(boolean enableGrouping, boolean enableNameMappings, boolean enableEncoding, boolean enableGeoParse) {
        super(enableGrouping, enableNameMappings, enableEncoding, enableGeoParse);
    }

    @Override
    public void initialize(NGSIEvent cygnusEvent) throws CygnusBadConfiguration {
        super.initialize(cygnusEvent);
        LinkedHashMap<String, ArrayList<JsonElement>> aggregation = getAggregation();
        aggregation.put(NGSIConstants.RECV_TIME_TS, new ArrayList<JsonElement>());
        aggregation.put(NGSIConstants.RECV_TIME, new ArrayList<JsonElement>());
        aggregation.put(NGSIConstants.FIWARE_SERVICE_PATH, new ArrayList<JsonElement>());
        aggregation.put(NGSIConstants.ENTITY_ID, new ArrayList<JsonElement>());
        aggregation.put(NGSIConstants.ENTITY_TYPE, new ArrayList<JsonElement>());
        aggregation.put(NGSIConstants.ATTR_NAME, new ArrayList<JsonElement>());
        aggregation.put(NGSIConstants.ATTR_TYPE, new ArrayList<JsonElement>());
        aggregation.put(NGSIConstants.ATTR_VALUE, new ArrayList<JsonElement>());
        aggregation.put(NGSIConstants.ATTR_MD, new ArrayList<JsonElement>());
    } // initialize

    @Override
    public void aggregate(NGSIEvent event) {
        // get the getRecvTimeTs headers
        long recvTimeTs = event.getRecvTimeTs();
        String recvTime = CommonUtils.getHumanReadable(recvTimeTs, false);
        // get the getRecvTimeTs body
        NotifyContextRequest.ContextElement contextElement = event.getContextElement();
        String entityId = contextElement.getId();
        String entityType = contextElement.getType();
        LOGGER.debug("[" + getName() + "] Processing context element (id=" + entityId + ", type="
                + entityType + ")");
        // iterate on all this context element attributes, if there are attributes
        ArrayList<NotifyContextRequest.ContextAttribute> contextAttributes = contextElement.getAttributes();
        if (contextAttributes == null || contextAttributes.isEmpty()) {
            LOGGER.warn("No attributes within the notified entity, nothing is done (id=" + entityId
                    + ", type=" + entityType + ")");
            return;
        } // if
        for (NotifyContextRequest.ContextAttribute contextAttribute : contextAttributes) {
            String attrName = contextAttribute.getName();
            String attrType = contextAttribute.getType();
            JsonElement attrValue = contextAttribute.getValue();
            String attrMetadata = contextAttribute.getContextMetadata();
            LOGGER.debug("[" + getName() + "] Processing context attribute (name=" + attrName + ", type="
                    + attrType + ")");
            // aggregate the attribute information
            LinkedHashMap<String, ArrayList<JsonElement>> aggregation = getAggregation();
            aggregation.get(NGSIConstants.RECV_TIME_TS).add(new JsonPrimitive(Long.toString(recvTimeTs)));
            aggregation.get(NGSIConstants.RECV_TIME).add(new JsonPrimitive(recvTime));
            aggregation.get(NGSIConstants.FIWARE_SERVICE_PATH).add(new JsonPrimitive(getServicePathForData()));
            aggregation.get(NGSIConstants.ENTITY_ID).add(new JsonPrimitive(entityId));
            aggregation.get(NGSIConstants.ENTITY_TYPE).add(new JsonPrimitive(entityType));
            aggregation.get(NGSIConstants.ATTR_NAME).add(new JsonPrimitive(attrName));
            aggregation.get(NGSIConstants.ATTR_TYPE).add(new JsonPrimitive(attrType));
            aggregation.get(NGSIConstants.ATTR_VALUE).add(attrValue);
            aggregation.get(NGSIConstants.ATTR_MD).add(new JsonPrimitive(attrMetadata));
        } // for
    } // aggregate

    private String getName() {
        return "NGSIUtils.GenericColumnAggregator";
    }

}
