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
package org.alfresco.repo.event2;

import java.util.ArrayDeque;
import java.util.Deque;

import org.alfresco.repo.event.v1.model.ChildAssociationResource;
import org.alfresco.repo.event.v1.model.EventData;
import org.alfresco.repo.event.v1.model.RepoEvent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.namespace.QName;

/**
 * Encapsulates events occurred in a single transaction.
 *
 * @author Chris Shields
 * @author Sara Aspery
 */
public class ChildAssociationEventConsolidator implements ChildAssociationEventSupportedPolicies
{
    private final Deque<EventType> eventTypes;

    private ChildAssociationRef childAssociationRef;
    private ChildAssociationResource resource;

    public ChildAssociationEventConsolidator(ChildAssociationRef childAssociationRef)
    {
        this.eventTypes = new ArrayDeque<>();
        this.childAssociationRef = childAssociationRef;
        resource = buildChildAssociationResource(this.childAssociationRef);
    }

    /**
     * Builds and returns the {@link RepoEvent} instance.
     *
     * @param eventInfo the object holding the event information
     * @return the {@link RepoEvent} instance
     */
    public RepoEvent<ChildAssociationResource> getRepoEvent(EventInfo eventInfo)
    {
        EventType eventType = getDerivedEvent();

        EventData.Builder<ChildAssociationResource> eventDataBuilder = EventData.<ChildAssociationResource>builder()
                .setEventGroupId(eventInfo.getTxnId())
                .setResource(resource);

        EventData<ChildAssociationResource> eventData = eventDataBuilder.build();
        return RepoEvent.<ChildAssociationResource>builder()
                .setId(eventInfo.getId())
                .setSource(eventInfo.getSource())
                .setTime(eventInfo.getTimestamp())
                .setType(eventType.getType())
                .setData(eventData)
                .build();
    }

    /**
     * Add child association created event on create of a child association.
     *
     * @param childAssociationRef ChildAssociationRef
     */
    @Override
    public void onCreateChildAssociation(ChildAssociationRef childAssociationRef, boolean isNewNode)
    {
        eventTypes.add(EventType.CHILD_ASSOC_CREATED);
        resource = buildChildAssociationResource(childAssociationRef);
    }

    /**
     * Add child association deleted event on delete of a child association.
     *
     * @param childAssociationRef ChildAssociationRef
     */
    @Override
    public void beforeDeleteChildAssociation(ChildAssociationRef childAssociationRef)
    {
        eventTypes.add(EventType.CHILD_ASSOC_DELETED);
        resource = buildChildAssociationResource(childAssociationRef);
    }

    private ChildAssociationResource buildChildAssociationResource(ChildAssociationRef childAssociationRef)
    {
        String parentId = childAssociationRef.getParentRef().getId();
        String childId = childAssociationRef.getChildRef().getId();
        String assocType = childAssociationRef.getTypeQName().toString();
        return new ChildAssociationResource(parentId, childId, assocType);
    }

    /**
     * @return a derived event for a transaction.
     */
    private EventType getDerivedEvent()
    {
        if (isTemporaryChildAssociation())
        {
            // This event will be filtered out, but we set the correct
            // event type anyway for debugging purposes
            return EventType.CHILD_ASSOC_DELETED;
        }
        else if (eventTypes.contains(EventType.CHILD_ASSOC_CREATED))
        {
            return EventType.CHILD_ASSOC_CREATED;
        }
        else if (eventTypes.getLast() == EventType.CHILD_ASSOC_DELETED)
        {
            return EventType.CHILD_ASSOC_DELETED;
        }
        else
        {
            // Default to first event
            return eventTypes.getFirst();
        }
    }

    /**
     * Whether or not the association has been created and then deleted, i.e. a temporary association.
     *
     * @return {@code true} if the association has been created and then deleted, otherwise false
     */
    public boolean isTemporaryChildAssociation()
    {
        return eventTypes.contains(EventType.CHILD_ASSOC_CREATED) && eventTypes.getLast() == EventType.CHILD_ASSOC_DELETED;
    }

    public QName getChildAssocType()
    {
        return childAssociationRef.getTypeQName();
    }

    public Deque<EventType> getEventTypes()
    {
        return eventTypes;
    }
}
