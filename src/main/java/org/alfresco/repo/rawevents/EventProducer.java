/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.repo.rawevents;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.apache.activemq.transport.amqp.message.AmqpMessageSupport;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class EventProducer
{
    protected static final String ERROR_SENDING = "Could not send event";

    protected ProducerTemplate producer;
    protected String endpoint;
    protected ObjectMapper objectMapper;

    public void setProducer(ProducerTemplate producer)
    {
        this.producer = producer;
    }

    public void setEndpoint(String endpoint)
    {
        this.endpoint = endpoint;
    }

    public void setObjectMapper(ObjectMapper objectMapper)
    {
        this.objectMapper = objectMapper;
    }

    private Map<String, Object> addHeaders(Map<String, Object> origHeaders)
    {
        if (origHeaders == null)
        {
            origHeaders = new HashMap<>();
        }

        origHeaders.put(AmqpMessageSupport.JMS_AMQP_MESSAGE_FORMAT, AmqpMessageSupport.AMQP_UNKNOWN);
        return origHeaders;
    }

    public void send(String endpointUri, Object event)
    {
        send(endpointUri, event, null);
    }

    public void send(String endpointUri, Object event, Map<String, Object> headers)
    {
        try
        {
            if (StringUtils.isEmpty(endpointUri))
            {
                endpointUri = this.endpoint;
            }
            else if (this.objectMapper != null && !(event instanceof String))
            {
                event = this.objectMapper.writeValueAsString(event);
            }

            this.producer.sendBodyAndHeaders(endpointUri, event, this.addHeaders(headers));
        }
        catch (Exception e)
        {
            throw new AlfrescoRuntimeException(ERROR_SENDING, e);
        }
    }
}
