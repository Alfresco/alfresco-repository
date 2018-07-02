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

import org.alfresco.heartbeat.datasender.HBDataSenderService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.*;

public abstract class QuartzJobScheduler implements HeartBeatJobScheduler
{

    /** The logger. */
    private static final Log logger = LogFactory.getLog(QuartzJobScheduler.class);

    /** schedule set for all jobs scheduled with this scheduler if testMode is on */
    protected boolean testMode = false;
    protected final String testCronExpression = "0 0/1 * * * ?";

    protected HBDataSenderService hbDataSenderService;
    protected Scheduler scheduler;

    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }

    public void setHbDataSenderService(HBDataSenderService hbDataSenderService)
    {
        this.hbDataSenderService = hbDataSenderService;
    }

    public void setTestMode(boolean testMode)
    {
        this.testMode = testMode;
    }

    public String getJobName(String collectorId)
    {
        return "heartbeat-" + collectorId;
    }

    public String getTriggerName(String collectorId)
    {
        return getJobName(collectorId) + "-Trigger";
    }

    protected abstract JobDataMap getJobDetailMap(HBBaseDataCollector collector);

    protected abstract Class<? extends Job> getHeartBeatJobClass();

    @Override
    public void scheduleJob(HBBaseDataCollector collector)
    {

        final JobDetail jobDetail = JobBuilder.newJob()
                .withIdentity(getJobName(collector.getCollectorId()))
                .usingJobData(getJobDetailMap(collector))
                .ofType(getHeartBeatJobClass())
                .build();

        final String cronExpression = testMode ? testCronExpression : collector.getCronExpression();
        // Schedule job
        final CronTrigger cronTrigger = TriggerBuilder.newTrigger()
                .withIdentity(getTriggerName(collector.getCollectorId()))
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();

        try{
            // Ensure the job wasn't already scheduled in an earlier retry of this transaction
            scheduler.unscheduleJob(cronTrigger.getKey());
            scheduler.scheduleJob(jobDetail, cronTrigger);

            if (logger.isDebugEnabled())
            {
                logger.debug("HeartBeat job scheduled for collector: " +
                        collector.getCollectorId());
            }
        }
        catch (SchedulerException e)
        {
            throw new RuntimeException("Heartbeat failed to schedule job for collector: "
                    + collector.getCollectorId(), e);
        }
    }

    @Override
    public void unscheduleJob(final HBBaseDataCollector collector)
    {
        try
        {
            scheduler.unscheduleJob(new TriggerKey(getTriggerName(collector.getCollectorId())));

            if (logger.isDebugEnabled())
            {
                logger.debug("HeartBeat unscheduled job for collector: " +
                        collector.getCollectorId());
            }
        }
        catch (SchedulerException e)
        {
            throw new RuntimeException("Heartbeat failed to unschedule job for collector: "
                    + collector.getCollectorId(), e);
        }
    }
}
