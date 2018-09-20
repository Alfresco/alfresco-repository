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

import org.alfresco.model.ContentModel;
import org.alfresco.model.RenditionModel;
import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.rendition.RenditionPreventionRegistry;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.util.PostTxnCallbackScheduler;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.service.cmr.rule.RuleType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.alfresco.model.ContentModel.PROP_CONTENT;
import static org.alfresco.model.RenditionModel.PROP_RENDITION_CONTENT_URL_HASH_CODE;
import static org.alfresco.service.namespace.QName.createQName;

/**
 * The Async Rendition service. Replaces the original deprecated RenditionService.
 *
 * @author adavis
 */
public class RenditionService2Impl implements RenditionService2, InitializingBean, ContentServicePolicies.OnContentUpdatePolicy
{
    public static final String TRANSFORMING_ERROR_MESSAGE = "Some error occurred during document transforming. Error message: ";

    public static final QName DEFAULT_RENDITION_CONTENT_PROP = ContentModel.PROP_CONTENT;
    public static final String DEFAULT_MIMETYPE = MimetypeMap.MIMETYPE_TEXT_PLAIN;
    public static final String DEFAULT_ENCODING = "UTF-8";

    private static Log logger = LogFactory.getLog(RenditionService2Impl.class);

    private TransactionService transactionService;
    private NodeService nodeService;
    private ContentService contentService;
    private RenditionPreventionRegistry renditionPreventionRegistry;
    private RenditionDefinitionRegistry2 renditionDefinitionRegistry2;
    private TransformClient transformClient;
    private PolicyComponent policyComponent;
    private BehaviourFilter behaviourFilter;
    private RuleService ruleService;
    private PostTxnCallbackScheduler renditionRequestSheduler;
    private boolean enabled;
    private boolean thumbnailsEnabled;

    public void setRenditionRequestSheduler(PostTxnCallbackScheduler renditionRequestSheduler)
    {
        this.renditionRequestSheduler = renditionRequestSheduler;
    }

    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    public void setRenditionPreventionRegistry(RenditionPreventionRegistry renditionPreventionRegistry)
    {
        this.renditionPreventionRegistry = renditionPreventionRegistry;
    }

    public void setRenditionDefinitionRegistry2(RenditionDefinitionRegistry2 renditionDefinitionRegistry2)
    {
        this.renditionDefinitionRegistry2 = renditionDefinitionRegistry2;
    }

    @Override
    public RenditionDefinitionRegistry2 getRenditionDefinitionRegistry2()
    {
        return renditionDefinitionRegistry2;
    }

    public void setTransformClient(TransformClient transformClient)
    {
        this.transformClient = transformClient;
    }

    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }

    public void setBehaviourFilter(BehaviourFilter behaviourFilter)
    {
        this.behaviourFilter = behaviourFilter;
    }

    public void setRuleService(RuleService ruleService)
    {
        this.ruleService = ruleService;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public void setThumbnailsEnabled(boolean thumbnailsEnabled)
    {
        this.thumbnailsEnabled = thumbnailsEnabled;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "transactionService", transactionService);
        PropertyCheck.mandatory(this, "nodeService", nodeService);
        PropertyCheck.mandatory(this, "contentService", contentService);
        PropertyCheck.mandatory(this, "renditionPreventionRegistry", renditionPreventionRegistry);
        PropertyCheck.mandatory(this, "renditionDefinitionRegistry2", renditionDefinitionRegistry2);
        PropertyCheck.mandatory(this, "transformClient", transformClient);
        PropertyCheck.mandatory(this, "policyComponent", policyComponent);
        PropertyCheck.mandatory(this, "behaviourFilter", behaviourFilter);
        PropertyCheck.mandatory(this, "ruleService", ruleService);

        // TODO use raw events
        policyComponent.bindClassBehaviour(
                ContentServicePolicies.OnContentUpdatePolicy.QNAME,
                RenditionModel.ASPECT_RENDITIONED,
                new JavaBehaviour(this, "onContentUpdate"));
    }

    public void render(NodeRef sourceNodeRef, String renditionName)
    {
        try
        {
            if (!isEnabled())
            {
                throw new RenditionService2Exception("Renditions are disabled (system.thumbnail.generate=false or renditionService2.enabled=false).");
            }
            checkSourceNodeForPreventionClass(sourceNodeRef);

            RenditionDefinition2 renditionDefinition = renditionDefinitionRegistry2.getRenditionDefinition(renditionName);
            if (renditionDefinition == null)
            {
                throw new IllegalArgumentException("The rendition "+renditionName+" has not been registered.");
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Request transform for rendition " + renditionName + " on " +sourceNodeRef);
            }

            transformClient.checkSupported(sourceNodeRef, renditionDefinition);

            String user = AuthenticationUtil.getRunAsUser();
            RetryingTransactionHelper.RetryingTransactionCallback callback = () ->
            {
                // Avoid doing extra transforms that have already been done.
                int sourceContentUrlHashCode = getSourceContentUrlHashCode(sourceNodeRef);
                NodeRef renditionNode = getRenditionNode(sourceNodeRef, renditionName);
                int renditionContentUrlHashCode = getRenditionContentUrlHashCode(renditionNode);
                if (renditionContentUrlHashCode == sourceContentUrlHashCode)
                {
                    throw new IllegalStateException("The rendition " + renditionName + " has already been created.");
                }
                transformClient.transform(sourceNodeRef, renditionDefinition, user, sourceContentUrlHashCode);
                return null;
            };
            renditionRequestSheduler.scheduleRendition(callback, sourceNodeRef + renditionName);
        }
        catch (Exception e)
        {
            logger.debug(e.getMessage());
            throw e;
        }
    }

    /**
     * Returns the hash code of the source node's content url. As transformations may be returned in a different
     * sequences to which they were requested, this is used work out if a rendition should be replaced.
     */
    private int getSourceContentUrlHashCode(NodeRef sourceNodeRef)
    {
        int hashCode = -1;
        ContentData contentData = DefaultTypeConverter.INSTANCE.convert(ContentData.class, nodeService.getProperty(sourceNodeRef, PROP_CONTENT));
        if (contentData != null)
        {
            String contentUrl = contentData.getContentUrl();
            if (contentUrl != null)
            {
                hashCode = contentUrl.hashCode();
            }
        }
        return hashCode;
    }

    /**
     * Returns the hash code of source node's content url on the rendition node (node may be null) if it does not exist.
     * Used work out if a rendition should be replaced. {@code -2} is returned if the rendition does not exist or was
     * not created by RenditionService2.
     */
    private int getRenditionContentUrlHashCode(NodeRef renditionNode)
    {
        return renditionNode == null || !nodeService.hasAspect(renditionNode, RenditionModel.ASPECT_RENDITION2)
                ? -2
                : (int)nodeService.getProperty(renditionNode, PROP_RENDITION_CONTENT_URL_HASH_CODE);
    }

    private NodeRef getRenditionNode(NodeRef sourceNodeRef, String renditionName)
    {
        QName renditionQName = createQName(NamespaceService.CONTENT_MODEL_1_0_URI, renditionName);
        List<ChildAssociationRef> renditionAssocs = nodeService.getChildAssocs(sourceNodeRef, RenditionModel.ASSOC_RENDITION, renditionQName);
        return renditionAssocs.isEmpty() ? null : renditionAssocs.get(0).getChildRef();
    }

    public boolean isCreatedByRenditionService2(NodeRef sourceNodeRef, String renditionName)
    {
        boolean result = false;
        if (isEnabled())
        {
            NodeRef renditionNode = getRenditionNode(sourceNodeRef, renditionName);
            if (renditionNode != null)
            {
                result = nodeService.hasAspect(renditionNode, RenditionModel.ASPECT_RENDITION2);
            }
        }
        return result;
    }

    /**
     * This method checks whether the specified source node is of a content class which has been registered for
     * rendition prevention.
     *
     * @param sourceNode the node to check.
     * @throws RenditionService2PreventedException if the source node is configured for rendition prevention.
     */
    // This code is based on the old RenditionServiceImpl.checkSourceNodeForPreventionClass(...)
    private void checkSourceNodeForPreventionClass(NodeRef sourceNode)
    {
        if (sourceNode != null && nodeService.exists(sourceNode))
        {
            // A node's content class is its type and all its aspects.
            Set<QName> nodeContentClasses = nodeService.getAspects(sourceNode);
            nodeContentClasses.add(nodeService.getType(sourceNode));

            for (QName contentClass : nodeContentClasses)
            {
                if (renditionPreventionRegistry.isContentClassRegistered(contentClass))
                {
                    String msg = "Node " + sourceNode + " cannot be renditioned as it is of class " + contentClass;
                    logger.debug(msg);
                    throw new RenditionService2PreventedException(msg);
                }
            }
        }
    }

    private List<ChildAssociationRef> getRenditionChildAssociations(NodeRef sourceNodeRef)
    {
        // Copy on code from the original RenditionService.
        List<ChildAssociationRef> result = Collections.emptyList();

        // Check that the node has the renditioned aspect applied
        if (nodeService.hasAspect(sourceNodeRef, RenditionModel.ASPECT_RENDITIONED) == true)
        {
            // Get all the renditions that match the given rendition name
            result = nodeService.getChildAssocs(sourceNodeRef, RenditionModel.ASSOC_RENDITION, RegexQNamePattern.MATCH_ALL);
        }
        return result;
    }

    @Override
    // Only returns valid renditions. These may be from RenditionService2 or original RenditionService.
    public List<ChildAssociationRef> getRenditions(NodeRef sourceNodeRef)
    {
        List<ChildAssociationRef> result = new ArrayList<>();
        List<ChildAssociationRef> childAsocs = getRenditionChildAssociations(sourceNodeRef);

        for (ChildAssociationRef childAssoc : childAsocs)
        {
            NodeRef renditionNode =  childAssoc.getChildRef();
            if (isRenditionAvailable(sourceNodeRef, renditionNode))
            {
                result.add(childAssoc);
            }
        }
        return result;
    }

    /**
     * Indicates if the rendition is available. Failed renditions (there was an error) don't have a contentUrl
     * and out of date renditions don't have a matching contentUrlHashCode.
     */
    public boolean isRenditionAvailable(NodeRef sourceNodeRef, NodeRef renditionNode)
    {
        boolean available = true;
        if (nodeService.hasAspect(renditionNode, RenditionModel.ASPECT_RENDITION2))
        {
            Serializable contentUrl = nodeService.getProperty(renditionNode, ContentModel.PROP_CONTENT);
            if (contentUrl == null)
            {
                available = false;
            }
            else
            {
                int sourceContentUrlHashCode = getSourceContentUrlHashCode(sourceNodeRef);
                int renditionContentUrlHashCode = getRenditionContentUrlHashCode(renditionNode);
                if (sourceContentUrlHashCode != renditionContentUrlHashCode)
                {
                    available = false;
                }
            }
        }
        return available;
    }

    @Override
    // Only returns a valid renditions. This may be from RenditionService2 or original RenditionService.
    public ChildAssociationRef getRenditionByName(NodeRef sourceNodeRef, String renditionName)
    {
        // Based on code from the original RenditionService. renditionName is a String rather than a QName.
        List<ChildAssociationRef> renditions = Collections.emptyList();

        // Thumbnails have a cm: prefix.
        QName renditionQName = createQName(NamespaceService.CONTENT_MODEL_1_0_URI, renditionName);

        // Check that the sourceNodeRef has the renditioned aspect applied
        if (nodeService.hasAspect(sourceNodeRef, RenditionModel.ASPECT_RENDITIONED) == true)
        {
            // Get all the renditions that match the given rendition name -
            // there should only be 1 (or 0)
            renditions = this.nodeService.getChildAssocs(sourceNodeRef, RenditionModel.ASSOC_RENDITION, renditionQName);
        }
        if (renditions.isEmpty())
        {
            return null;
        }
        else
        {
            if (renditions.size() > 1 && logger.isDebugEnabled())
            {
                logger.debug("Unexpectedly found " + renditions.size() + " renditions of name " + renditionQName + " on node " + sourceNodeRef);
            }
            ChildAssociationRef childAssoc = renditions.get(0);
            NodeRef renditionNode = childAssoc.getChildRef();
            return !isRenditionAvailable(sourceNodeRef, renditionNode) ? null: childAssoc;
        }
    }

    @Override
    public boolean isEnabled()
    {
        return enabled && thumbnailsEnabled;
    }

    /**
     *  Takes a transformation (InputStream) and attaches it as a rendition to the source node.
     *  Does nothing if there is already a newer rendition.
     *  If the transformInputStream is null, this is taken to be a transform failure.
     */
    public void consume(NodeRef sourceNodeRef, InputStream transformInputStream, RenditionDefinition2 renditionDefinition,
                 int transformContentUrlHashCode)
    {
        String renditionName = renditionDefinition.getRenditionName();
        int sourceContentUrlHashCode = getSourceContentUrlHashCode(sourceNodeRef);

        if (transformContentUrlHashCode != sourceContentUrlHashCode)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Ignore transform for rendition " + renditionName + " on " + sourceNodeRef + " as it is no longer needed");
            }
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Set the content of rendition " + renditionName + " on " + sourceNodeRef +
                        (transformInputStream == null ? " to null as the transform failed" : " to the transform result"));
            }

            AuthenticationUtil.runAsSystem((AuthenticationUtil.RunAsWork<Void>) () ->
                transactionService.getRetryingTransactionHelper().doInTransaction(() ->
                {
                    // Ensure that the creation of a rendition does not cause updates to the modified, modifier properties on the source node
                    NodeRef renditionNode = getRenditionNode(sourceNodeRef, renditionName);
                    boolean createRenditionNode = renditionNode == null;
                    Date sourceModified = (Date) nodeService.getProperty(sourceNodeRef, ContentModel.PROP_MODIFIED);
                    boolean sourceHasAspectRenditioned = nodeService.hasAspect(sourceNodeRef, RenditionModel.ASPECT_RENDITIONED);
                    boolean sourceChanges = !sourceHasAspectRenditioned || createRenditionNode || sourceModified != null || transformInputStream == null;
                    try
                    {
                        if (sourceChanges)
                        {
                            ruleService.disableRuleType(RuleType.UPDATE);
                            behaviourFilter.disableBehaviour(sourceNodeRef, ContentModel.ASPECT_AUDITABLE);
                            behaviourFilter.disableBehaviour(sourceNodeRef, ContentModel.ASPECT_VERSIONABLE);
                        }

                        // If they do not exist create the rendition association and the rendition node.
                        if (createRenditionNode)
                        {
                            renditionNode = createRenditionNode(sourceNodeRef, renditionDefinition);
                        }

                        if (createRenditionNode)
                        {
                            nodeService.addAspect(renditionNode, RenditionModel.ASPECT_RENDITION2, null);
                            nodeService.addAspect(renditionNode, RenditionModel.ASPECT_HIDDEN_RENDITION, null);
                        }
                        else if (!nodeService.hasAspect(renditionNode, RenditionModel.ASPECT_RENDITION2))
                        {
                            nodeService.addAspect(renditionNode, RenditionModel.ASPECT_RENDITION2, null);
                        }
                        nodeService.setProperty(renditionNode, RenditionModel.PROP_RENDITION_CONTENT_URL_HASH_CODE, transformContentUrlHashCode);
                        if (sourceModified != null)
                        {
                            setThumbnailLastModified(sourceNodeRef, renditionName, sourceModified);
                        }

                        if (transformInputStream != null)
                        {
                            try
                            {
                                // Set or replace rendition content
                                ContentWriter contentWriter = contentService.getWriter(renditionNode, DEFAULT_RENDITION_CONTENT_PROP, true);
                                String targetMimetype = renditionDefinition.getTargetMimetype();
                                contentWriter.setMimetype(targetMimetype);
                                contentWriter.setEncoding(DEFAULT_ENCODING);
                                ContentWriter renditionWriter = contentWriter;
                                renditionWriter.putContent(transformInputStream);
                            }
                            catch (Exception e)
                            {
                                logger.error("Failed to read transform InputStream into rendition " + renditionName + " on " + sourceNodeRef);
                                throw e;
                            }
                        }
                        else
                        {
                            Serializable content = nodeService.getProperty(renditionNode, PROP_CONTENT);
                            if (content != null)
                            {
                                nodeService.removeProperty(renditionNode, PROP_CONTENT);
                            }
                        }

                        if (!sourceHasAspectRenditioned)
                        {
                            nodeService.addAspect(sourceNodeRef, RenditionModel.ASPECT_RENDITIONED, null);
                        }
                    }
                    catch (Exception e)
                    {
                        throw new RenditionService2Exception(TRANSFORMING_ERROR_MESSAGE + e.getMessage(), e);
                    }
                    finally
                    {
                        if (sourceChanges)
                        {
                            behaviourFilter.enableBehaviour(sourceNodeRef, ContentModel.ASPECT_AUDITABLE);
                            behaviourFilter.enableBehaviour(sourceNodeRef, ContentModel.ASPECT_VERSIONABLE);
                            ruleService.enableRuleType(RuleType.UPDATE);
                        }
                    }
                    return null;
                }, false, true));
        }
    }

    // Based on code from org.alfresco.repo.thumbnail.ThumbnailServiceImpl.addThumbnailModificationData
    private void setThumbnailLastModified(NodeRef sourceNodeRef, String renditionName, Date sourceModified)
    {
        String prefix = renditionName + ':';
        final String lastModifiedValue = prefix + sourceModified.getTime();

        if (logger.isTraceEnabled())
        {
            logger.trace("Setting thumbnail last modified date to " + lastModifiedValue +" on source node: " + sourceNodeRef);
        }

        if (nodeService.hasAspect(sourceNodeRef, ContentModel.ASPECT_THUMBNAIL_MODIFICATION))
        {
            List<String> thumbnailMods = (List<String>) nodeService.getProperty(sourceNodeRef, ContentModel.PROP_LAST_THUMBNAIL_MODIFICATION_DATA);
            String target = null;
            for (String currThumbnailMod: thumbnailMods)
            {
                if (currThumbnailMod.startsWith(prefix))
                {
                    target = currThumbnailMod;
                }
            }
            if (target != null)
            {
                thumbnailMods.remove(target);
            }
            thumbnailMods.add(lastModifiedValue);
            nodeService.setProperty(sourceNodeRef, ContentModel.PROP_LAST_THUMBNAIL_MODIFICATION_DATA, (Serializable) thumbnailMods);
        }
        else
        {
            List<String> thumbnailMods = Collections.singletonList(lastModifiedValue);
            Map<QName, Serializable> properties = new HashMap<>();
            properties.put(ContentModel.PROP_LAST_THUMBNAIL_MODIFICATION_DATA, (Serializable) thumbnailMods);
            nodeService.addAspect(sourceNodeRef, ContentModel.ASPECT_THUMBNAIL_MODIFICATION, properties);
        }
    }

    // Based on original AbstractRenderingEngine.createRenditionNodeAssoc
    private NodeRef createRenditionNode(NodeRef sourceNode, RenditionDefinition2 renditionDefinition)
    {
        String renditionName = renditionDefinition.getRenditionName();

        Map<QName, Serializable> nodeProps = new HashMap<QName, Serializable>();
        nodeProps.put(ContentModel.PROP_NAME, renditionName);
        nodeProps.put(ContentModel.PROP_THUMBNAIL_NAME, renditionName);
        nodeProps.put(ContentModel.PROP_CONTENT_PROPERTY_NAME, ContentModel.PROP_CONTENT);
        nodeProps.put(ContentModel.PROP_IS_INDEXED, Boolean.FALSE);

        QName assocName = createQName(NamespaceService.CONTENT_MODEL_1_0_URI, renditionName);
        QName assocType = RenditionModel.ASSOC_RENDITION;
        QName nodeType = ContentModel.TYPE_THUMBNAIL;

        ChildAssociationRef childAssoc = nodeService.createNode(sourceNode, assocType, assocName, nodeType, nodeProps);
        NodeRef renditionNode = childAssoc.getChildRef();

        if (logger.isDebugEnabled())
        {
            logger.debug("Created " + renditionName + " rendition node " + childAssoc.getChildRef() + " as a child of " + sourceNode);
        }

        return renditionNode;
    }

    @Override
    public void onContentUpdate(NodeRef sourceNodeRef, boolean newNode)
    {
        if (!newNode)
        {
            logger.debug("onContentUpdate on " + sourceNodeRef);
            List<ChildAssociationRef> childAssocs = getRenditionChildAssociations(sourceNodeRef);
            for (ChildAssociationRef childAssoc : childAssocs)
            {
                NodeRef renditionNodeRef = childAssoc.getChildRef();
                // TODO: This check will not be needed once the original RenditionService is removed.
                if (nodeService.hasAspect(renditionNodeRef, RenditionModel.ASPECT_RENDITION2))
                {
                    QName childAssocQName = childAssoc.getQName();
                    String renditionName = childAssocQName.getLocalName();
                    render(sourceNodeRef, renditionName);
                }

                // TODO what if we have ASPECT_RENDITION2 but there is no rendition description for a new mimetype
                // with RenditionService2 and there is for RenditionService. Should we do that check in the render method?
            }
        }
    }
}
