/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
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

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;

import java.util.Map;

/**
 * Request synchronous transforms. Used in refactoring deprecated code, which called Legacy transforms, so that it will
 * first try a Local transform, falling back to Legacy if not available. Class provides the switch between Local and
 * Legacy transforms.
 *
 * @author adavis
 */
@Deprecated
public class SwitchingSynchronousTransformClient implements SynchronousTransformClient
{
    private final SynchronousTransformClient primary;
    private final SynchronousTransformClient secondary;
    private ThreadLocal<SynchronousTransformClient> synchronousTransformClient = new ThreadLocal<>();

    public SwitchingSynchronousTransformClient(SynchronousTransformClient primary, SynchronousTransformClient secondary)
    {
        this.primary = primary;
        this.secondary = secondary;
    }

    @Override
    public boolean isSupported(NodeRef sourceNodeRef, String sourceMimetype, long sourceSizeInBytes, String contentUrl,
                               String targetMimetype, Map<String, String> actualOptions, String transformName)
    {
        boolean supported = true;
        if (primary.isSupported(sourceNodeRef, sourceMimetype, sourceSizeInBytes, contentUrl,
                targetMimetype, actualOptions, transformName))
        {
            synchronousTransformClient.set(primary);
        }
        else if (secondary.isSupported(sourceNodeRef, sourceMimetype, sourceSizeInBytes, contentUrl,
                targetMimetype, actualOptions, transformName))
        {
            synchronousTransformClient.set(secondary);
        }
        else
        {
            supported = false;
        }
        return supported;
    }

    @Override
    public void transform(ContentReader reader, ContentWriter writer, Map<String, String> actualOptions, String transformName, NodeRef sourceNodeRef) throws Exception
    {
        synchronousTransformClient.get().transform(reader, writer, actualOptions, transformName, sourceNodeRef);
    }
}
