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
package org.alfresco.heartbeat;

import java.util.List;

import org.alfresco.heartbeat.datasender.HBData;
import org.alfresco.service.cmr.repository.HBDataCollectorService;
import org.alfresco.util.PropertyCheck;

/**
 *
 * This class is to be extended by HeartBeat data collectors.
 * Every new collector needs to provide details of the data it collects. As good examples use one
 * of the existing collectors {@link AuthoritiesDataCollector}, {@link ConfigurationDataCollector},
 * {@link InfoDataCollector}, {@link ModelUsageDataCollector}, {@link SystemUsageDataCollector}.
 * They are all following this layout:
 *
 * <ul>
 *  <li>Collector ID: <b>a.collector.id</b></li>
 *  <li>Data:
 *      <ul>
 *          <li><b>dataP1:</b> data type - description</li>
 *          <li><b>dataP2:</b> data type - description</li>
 *          ...
 *      </ul>
 *  </li>
 * </ul>
 *
 * @author eknizat
 */
public abstract class HBBaseDataCollector
{
    private final String collectorId;
    private final String collectorVersion;
    private final String cronExpression;

    /**
     * The collector service managing this collector.
     */
    private HBDataCollectorService hbDataCollectorService;

    /**
     *
     * @param collectorId Unique name of the collector e.g.: acs.repository.info
     * @param collectorVersion Version of the collector e.g.: 1.0
     * @param cronExpression Cron expression for frequently time scheduling e.g.: "0 0 0 ? * SUN" (Weekly)
     */
    public HBBaseDataCollector(String collectorId, String collectorVersion, String cronExpression)
    {
        PropertyCheck.mandatory(this, "collectorId", collectorId);
        PropertyCheck.mandatory(this, "collectorVersion", collectorVersion);
        PropertyCheck.mandatory(this, "cronExpression", cronExpression);

        this.collectorId = collectorId;
        this.collectorVersion = collectorVersion;
        this.cronExpression = cronExpression;
    }

    public String getCollectorId()
    {
        return collectorId;
    }

    public String getCollectorVersion()
    {
        return this.collectorVersion;
    }

    public String getCronExpression()
    {
        return this.cronExpression;
    }

    public void setHbDataCollectorService(HBDataCollectorService hbDataCollectorService)
    {
        this.hbDataCollectorService = hbDataCollectorService;
    }

    /**
     * This method is called by Spring at initialisation and will register this collector with the provided {@link HBDataCollectorService}
     */
    public void register()
    {
        PropertyCheck.mandatory(this, "hbDataCollectorService", hbDataCollectorService);
        hbDataCollectorService.registerCollector(this);
    }

    /**
     * This method is called by Spring at initialisation and will deregister this collector with the provided {@link HBDataCollectorService}
     */
    public void deregister()
    {
        PropertyCheck.mandatory(this, "hbDataCollectorService", hbDataCollectorService);
        hbDataCollectorService.deregisterCollector(this);
    }

    /**
     * This method returns data to be collected.
     * @return List of {@link HBData}
     */
    public abstract List<HBData> collectData();
}
