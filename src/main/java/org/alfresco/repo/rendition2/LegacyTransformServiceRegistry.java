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
package org.alfresco.repo.rendition2;

import org.alfresco.repo.content.transform.TransformerDebug;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.transform.client.model.config.TransformServiceRegistry;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;

/**
 * Implements {@link TransformServiceRegistry} providing a mechanism of validating if a legacy transformation
 * (based on {@link org.alfresco.repo.content.transform.AbstractContentTransformer2} request is supported.
 *
 * @author adavis
 */
@Deprecated
public class LegacyTransformServiceRegistry extends AbstractTransformServiceRegistry implements InitializingBean
{
    private ContentService contentService;
    private TransformationOptionsConverter converter;
    private boolean enabled = true;
    private boolean firstTime = true;
    private TransformerDebug transformerDebug;

    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    public void setConverter(TransformationOptionsConverter converter)
    {
        this.converter = converter;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        firstTime = true;
    }

    public void setTransformerDebug(TransformerDebug transformerDebug)
    {
        this.transformerDebug = transformerDebug;
    }

    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "contentService", contentService);
        PropertyCheck.mandatory(this, "converter", converter);
        PropertyCheck.mandatory(this, "transformerDebug", transformerDebug);
    }

    @Override
    public long getMaxSize(String sourceMimetype, String targetMimetype, Map<String, String> options, String renditionName)
    {
        // This message is not logged if placed in afterPropertiesSet
        if (firstTime)
        {
            firstTime = false;
            transformerDebug.debug("Legacy transforms are " + (enabled ? "enabled" : "disabled"));
        }

        long maxSize = 0;
        if (enabled)
        {
            try
            {
                TransformationOptions transformationOptions = converter.getTransformationOptions(renditionName, options);
                maxSize = contentService.getMaxSourceSizeBytes(sourceMimetype, targetMimetype, transformationOptions);
            }
            catch (IllegalArgumentException ignore)
            {
                // Typically if the mimetype is invalid.
            }
        }
        return maxSize;
    }
}