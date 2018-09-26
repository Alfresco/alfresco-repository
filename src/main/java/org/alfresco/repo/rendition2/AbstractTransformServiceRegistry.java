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

import java.util.Map;

/**
 * Contains common code used in TransformServiceRegistries.
 *
 * @author adavis
 */
public abstract class AbstractTransformServiceRegistry implements TransformServiceRegistry
{
    @Override
    public boolean isSupported(String sourceMimetype, long size, String targetMimetype, String renditionName, Map<String, String> options)
    {
        Long maxSize = getMaxSize(sourceMimetype, targetMimetype, renditionName, options);
        return maxSize != null && (maxSize == -1L || maxSize > size);
    }
}
