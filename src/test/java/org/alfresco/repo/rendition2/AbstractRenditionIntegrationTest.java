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

import junit.framework.AssertionFailedError;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.thumbnail.ThumbnailRegistry;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.rendition.RenditionService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.alfresco.util.BaseSpringTest;
import org.alfresco.util.GUID;
import org.alfresco.util.PropertyMap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;

import static java.lang.Thread.sleep;
import static org.alfresco.model.ContentModel.PROP_CONTENT;
import static org.alfresco.repo.content.MimetypeMap.EXTENSION_BINARY;

/**
 * Class unites common utility methods for {@link org.alfresco.repo.rendition2} package tests.
 */
public abstract class AbstractRenditionIntegrationTest extends BaseSpringTest
{
    @Autowired
    protected RenditionService2Impl renditionService2;

    @Autowired
    protected RenditionDefinitionRegistry2 renditionDefinitionRegistry2;

    @Autowired
    protected TransformClient transformClient;

    @Autowired
    protected RenditionService renditionService;

    @Autowired
    protected ThumbnailRegistry thumbnailRegistry;

    @Autowired
    protected MimetypeMap mimetypeMap;

    @Autowired
    protected MimetypeService mimetypeService;

    @Autowired
    protected NodeService nodeService;

    @Autowired
    protected ContentService contentService;

    @Autowired
    protected TransactionService transactionService;

    @Autowired
    protected MutableAuthenticationService authenticationService;

    @Autowired
    protected PersonService personService;

    @Autowired
    protected PermissionService permissionService;

    static String PASSWORD = "password";

    protected static final String ADMIN = "admin";
    protected static final String DOC_LIB = "doclib";

    @BeforeClass
    public static void before()
    {
        // Ensure other applications contexts are closed...
        // Multiple consumers not supported for same direct vm in different Camel contexts.
        ApplicationContextHelper.closeApplicationContext();

        // Use the docker images for transforms
        System.setProperty("alfresco-pdf-renderer.url", "http://localhost:8090/");
        System.setProperty("img.url", "http://localhost:8091");
        System.setProperty("jodconverter.url", "http://localhost:8092/");
        System.setProperty("tika.url", "http://localhost:8093/");
    }

    @Before
    public void setUp() throws Exception
    {
        assertTrue("The RenditionService2 needs to be enabled", renditionService2.isEnabled());
    }

    @After
    public void cleanUp()
    {
        AuthenticationUtil.clearCurrentSecurityContext();
    }

    @AfterClass
    public static void after()
    {
        System.clearProperty("alfresco-pdf-renderer.url");
        System.clearProperty("img.url");
        System.clearProperty("jodconverter.url");
        System.clearProperty("tika.url");
    }

    protected void checkRendition(String testFileName, String renditionName, boolean expectedToPass)
    {
        try
        {
            NodeRef sourceNodeRef = createSource(ADMIN, testFileName);
            render(ADMIN, sourceNodeRef, renditionName);
            waitForRendition(ADMIN, sourceNodeRef, renditionName);
            if (!expectedToPass)
            {
                fail("The " + renditionName + " rendition should NOT be supported for " + testFileName);
            }
        }
        catch(UnsupportedOperationException e)
        {
            if (expectedToPass)
            {
                fail("The " + renditionName + " rendition SHOULD be supported for " + testFileName);
            }
        }
    }

    // Creates a new source node as the given user in its own transaction.
    protected NodeRef createSource(String user, String testFileName)
    {
        return AuthenticationUtil.runAs(() ->
                transactionService.getRetryingTransactionHelper().doInTransaction(() ->
                        createSource(testFileName)), user);
    }

    // Creates a new source node as the current user in the current transaction.
    private NodeRef createSource(String testFileName) throws FileNotFoundException
    {
        return createContentNodeFromQuickFile(testFileName);
    }

    // Changes the content of a source node as the given user in its own transaction.
    protected void updateContent(String user, NodeRef sourceNodeRef, String testFileName)
    {
        AuthenticationUtil.runAs((AuthenticationUtil.RunAsWork<Void>) () ->
                transactionService.getRetryingTransactionHelper().doInTransaction(() ->
                {
                    updateContent(sourceNodeRef, testFileName);
                    return null;
                }), user);
    }

    // Changes the content of a source node as the current user in the current transaction.
    private NodeRef updateContent(NodeRef sourceNodeRef, String testFileName) throws FileNotFoundException
    {
        File file = ResourceUtils.getFile("classpath:quick/" + testFileName);
        nodeService.setProperty(sourceNodeRef, ContentModel.PROP_NAME, testFileName);

        ContentWriter contentWriter = contentService.getWriter(sourceNodeRef, ContentModel.PROP_CONTENT, true);
        contentWriter.setMimetype(mimetypeService.guessMimetype(testFileName));
        contentWriter.putContent(file);

        return sourceNodeRef;
    }

    // Clears the content of a source node as the given user in its own transaction.
    protected void clearContent(String user, NodeRef sourceNodeRef)
    {
        AuthenticationUtil.runAs((AuthenticationUtil.RunAsWork<Void>) () ->
                transactionService.getRetryingTransactionHelper().doInTransaction(() ->
                {
                    clearContent(sourceNodeRef);
                    return null;
                }), user);
    }

    // Clears the content of a source node as the current user in the current transaction.
    private void clearContent(NodeRef sourceNodeRef)
    {
        nodeService.removeProperty(sourceNodeRef, PROP_CONTENT);
    }

    // Requests a new rendition as the given user in its own transaction.
    protected void render(String user, NodeRef sourceNode, String renditionName)
    {
        AuthenticationUtil.runAs((AuthenticationUtil.RunAsWork<Void>) () ->
                transactionService.getRetryingTransactionHelper().doInTransaction(() ->
                {
                    render(sourceNode, renditionName);
                    return null;
                }), user);
    }

    // Requests a new rendition as the current user in the current transaction.
    private void render(NodeRef sourceNodeRef, String renditionName)
    {
        renditionService2.render(sourceNodeRef, renditionName);
    }

    // As a given user waitForRendition for a rendition to appear. Creates new transactions to do this.
    protected NodeRef waitForRendition(String user, NodeRef sourceNodeRef, String renditionName) throws AssertionFailedError
    {
        try
        {
            return AuthenticationUtil.runAs(() -> waitForRendition(sourceNodeRef, renditionName), user);
        }
        catch (RuntimeException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof AssertionFailedError)
            {
                throw (AssertionFailedError)cause;
            }
            throw e;
        }
    }

    // As the current user waitForRendition for a rendition to appear. Creates new transactions to do this.
    private NodeRef waitForRendition(NodeRef sourceNodeRef, String renditionName) throws InterruptedException
    {
        long maxMillis = 10000;
        ChildAssociationRef assoc = null;
        for (int i = (int)(maxMillis / 1000); i >= 0; i--)
        {
            // Must create a new transaction in order to see changes that take place after this method started.
            assoc = transactionService.getRetryingTransactionHelper().doInTransaction(() ->
                    renditionService2.getRenditionByName(sourceNodeRef, renditionName), true, true);
            if (assoc != null)
            {
                break;
            }
            logger.debug("RenditionService2.getRenditionByName(...) sleep "+i);
            sleep(1000);
        }
        assertNotNull("Rendition " + renditionName + " failed", assoc);
        return assoc.getChildRef();
    }

    protected String getTestFileName(String sourceMimetype) throws FileNotFoundException
    {
        String extension = mimetypeMap.getExtension(sourceMimetype);
        String testFileName = extension.equals(EXTENSION_BINARY) ? null : "quick."+extension;
        if (testFileName != null)
        {
            try
            {
                ResourceUtils.getFile("classpath:quick/" + testFileName);
            }
            catch (FileNotFoundException e)
            {
                testFileName = null;
            }
        }
        return testFileName;
    }

    NodeRef createContentNodeFromQuickFile(String fileName) throws FileNotFoundException
    {
        NodeRef rootNodeRef = nodeService.getRootNode(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
        NodeRef folderNodeRef = nodeService.createNode(
                rootNodeRef,
                ContentModel.ASSOC_CHILDREN,
                QName.createQName(getName() + GUID.generate()),
                ContentModel.TYPE_FOLDER).getChildRef();

        File file = ResourceUtils.getFile("classpath:quick/" + fileName);
        NodeRef contentRef = nodeService.createNode(
                folderNodeRef,
                ContentModel.ASSOC_CONTAINS,
                ContentModel.ASSOC_CONTAINS,
                ContentModel.TYPE_CONTENT,
                Collections.singletonMap(ContentModel.PROP_NAME, fileName))
                .getChildRef();
        ContentWriter contentWriter = contentService.getWriter(contentRef, ContentModel.PROP_CONTENT, true);
        contentWriter.setMimetype(mimetypeService.guessMimetype(fileName));
        contentWriter.putContent(file);

        return contentRef;
    }

    static String generateNewUsernameString()
    {
        return "user-" + GUID.generate();
    }

    String createRandomUser()
    {
        return AuthenticationUtil.runAs(() ->
        {
            String username = generateNewUsernameString();
            createUser(username);
            return username;
        }, AuthenticationUtil.getAdminUserName());
    }

    void createUser(String username)
    {
        createUser(username, "firstName", "lastName", "jobTitle", 0);
    }

    void createUser(final String username,
                            final String firstName,
                            final String lastName,
                            final String jobTitle,
                            final long quota)
    {
        RetryingTransactionHelper.RetryingTransactionCallback<Void> createUserCallback = () ->
        {
            authenticationService.createAuthentication(username, PASSWORD.toCharArray());

            PropertyMap personProperties = new PropertyMap();
            personProperties.put(ContentModel.PROP_USERNAME, username);
            personProperties.put(ContentModel.PROP_AUTHORITY_DISPLAY_NAME, "title" + username);
            personProperties.put(ContentModel.PROP_FIRSTNAME, firstName);
            personProperties.put(ContentModel.PROP_LASTNAME, lastName);
            personProperties.put(ContentModel.PROP_EMAIL, username+"@example.com");
            personProperties.put(ContentModel.PROP_JOBTITLE, jobTitle);
            if (quota > 0)
            {
                personProperties.put(ContentModel.PROP_SIZE_QUOTA, quota);
            }
            personService.createPerson(personProperties);
            return null;
        };

        RetryingTransactionHelper txnHelper = transactionService.getRetryingTransactionHelper();
        txnHelper.doInTransaction(createUserCallback);
    }
}
