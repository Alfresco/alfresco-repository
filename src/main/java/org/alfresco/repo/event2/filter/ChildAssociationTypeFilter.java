/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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
package org.alfresco.repo.event2.filter;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;

/**
 * Implementation of the child association types filter.
 *
 * @author Sara Aspery
 */
public class ChildAssociationTypeFilter extends AbstractNodeEventFilter
{
    private final List<String> assocTypesBlackList;

    public ChildAssociationTypeFilter(String filteredChildAssocTypes)
    {
        this.assocTypesBlackList = parseFilterList(filteredChildAssocTypes);
    }

    @Override
    public Set<QName> getExcludedTypes()
    {
        Set<QName> result = new HashSet<>();

        // add child association types defined in repository.properties/alfresco-global.properties
        assocTypesBlackList.forEach(childAssocType -> result.addAll(expandTypeDef(childAssocType)));

        return result;
    }

}
