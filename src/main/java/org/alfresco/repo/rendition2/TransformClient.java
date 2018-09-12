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

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Request rendition transforms.
 *
 * @author adavis
 */
public interface TransformClient
{
    /**
     * Requests an asynchronous transform and the subsequent linkage of that transform as a rendition.
     * @param sourceNodeRef the source node
     * @param renditionDefinition which rendition to perform
     * @param sourceContentUrlHashCode the hash code of the source node's content URL. Used to check the transform result
     *                                still matches the source node, before it is used as a rendition.
     * @throws UnsupportedOperationException if the transform is not supported.
     */
    public void transform(NodeRef sourceNodeRef, RenditionDefinition2 renditionDefinition, int sourceContentUrlHashCode);
}
