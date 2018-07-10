/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2017 Alfresco Software Limited
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
package org.alfresco.heartbeat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.alfresco.heartbeat.datasender.HBData;
import org.alfresco.heartbeat.jobs.HeartBeatJobScheduler;
import org.alfresco.repo.descriptor.DescriptorDAO;
import org.alfresco.service.cmr.repository.HBDataCollectorService;
import org.alfresco.service.descriptor.Descriptor;
import org.alfresco.traitextender.SpringExtensionBundle;
import org.junit.Before;
import org.junit.Test;

/**
 * @author mpopa
 */
public class ConfigurationDataCollectorTest
{
    private ConfigurationDataCollector configurationCollector;
    private HBDataCollectorService mockCollectorService;
    private SpringExtensionBundle smartFoldersBundle;
    private DescriptorDAO mockDescriptorDAO;
    private DescriptorDAO mockServerDescriptorDAO;
    private List<HBData> collectedData;
    private HeartBeatJobScheduler mockScheduler;

    @Before
    public void setUp()
    {
        smartFoldersBundle = mock(SpringExtensionBundle.class);
        mockDescriptorDAO = mock(DescriptorDAO.class);
        mockServerDescriptorDAO = mock(DescriptorDAO.class);
        mockCollectorService = mock(HBDataCollectorService.class);
        mockScheduler = mock(HeartBeatJobScheduler.class);

        Descriptor mockDescriptor = mock(Descriptor.class);
        when(mockDescriptor.getId()).thenReturn("mock_id");
        when(mockServerDescriptorDAO.getDescriptor()).thenReturn(mockDescriptor);
        when(mockDescriptorDAO.getDescriptor()).thenReturn(mockDescriptor);

        configurationCollector = new ConfigurationDataCollector("acs.repository.configuration", "1.0", "0 0 0 ? * SUN", mockScheduler);
        configurationCollector.setHbDataCollectorService(mockCollectorService);
        configurationCollector.setCurrentRepoDescriptorDAO(mockDescriptorDAO);
        configurationCollector.setSmartFoldersBundle(smartFoldersBundle);
        collectedData = configurationCollector.collectData();
    }

    @Test
    public void testHBDataFields()
    {
        for(HBData data : this.collectedData)
        {
            assertNotNull(data.getCollectorId());
            assertNotNull(data.getCollectorVersion());
            assertNotNull(data.getSchemaVersion());
            assertNotNull(data.getSystemId());
            assertNotNull(data.getTimestamp());
            assertNotNull(data.getData());
        }
    }

    @Test
    public void testConfigurationDataIsCollected()
    {
        HBData repoInfo = grabDataByCollectorId(configurationCollector.getCollectorId());
        assertNotNull("Repository configuration data missing.", repoInfo);

        Map<String,Object> data = repoInfo.getData();
        assertTrue(data.containsKey("smartFoldersEnabled"));
    }

    private HBData grabDataByCollectorId(String collectorId)
    {
        for (HBData d : this.collectedData)
        {
            if(d.getCollectorId()!=null && d.getCollectorId().equals(collectorId))
            {
                return d;
            }
        }
        return null;
    }

}
