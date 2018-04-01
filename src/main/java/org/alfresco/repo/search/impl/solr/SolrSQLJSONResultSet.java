/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
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
package org.alfresco.repo.search.impl.solr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.search.SimpleResultSetMetaData;
import org.alfresco.repo.search.impl.lucene.JSONResult;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.BasicSearchParameters;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.PermissionEvaluationMode;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetMetaData;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SpellCheckResult;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Pojo that parses and stores solr stream response.
 * @author Michael Suzuki
 */
public class SolrSQLJSONResultSet implements ResultSet, JSONResult
{
    private static final String SOLR_STREAM_EXCEPTION = "EXCEPTION";
    private static Log logger = LogFactory.getLog(SolrSQLJSONResultSet.class);
    private Long queryTime;
    private SimpleResultSetMetaData resultSetMetaData;
    private String solrResponse;
    private int length;
    ResultSet wrapped;
    private ArrayNode docs;
    private long numberFound;
    
    public SolrSQLJSONResultSet(JsonNode json, BasicSearchParameters searchParameters)
    {
        solrResponse = json.toString();
        JsonNode res = json.get("result-set");
        docs = (ArrayNode) res.get("docs");
        /*
         * Check that there is no error, which is returned in the first object.
         */

        JsonNode obj1 = docs.get(0);
        if(obj1.has(SOLR_STREAM_EXCEPTION))
        {
            String error =  obj1.get(SOLR_STREAM_EXCEPTION).toString();
            if(error.equalsIgnoreCase("/sql handler only works in Solr Cloud mode"))
            {
                throw new RuntimeException("Unable to execute the query, this API requires InsightEngine.");
            }
            throw new RuntimeException("Unable to execute the query, error caused by: " + error);
        }
        //Check if it has an error
        this.length = docs.size();
        //Last element will contain the object that hold the solr response time.
        JsonNode time = docs.get(length -1);
        this.numberFound = length - 1;
        queryTime = time.get("RESPONSE_TIME").longValue();
        // Were hard coding this as we have a hard limit of 1000 results, any more will not be readable.
        this.resultSetMetaData = new SimpleResultSetMetaData(LimitBy.FINAL_SIZE,
                PermissionEvaluationMode.EAGER, (SearchParameters)searchParameters);
    }

    @Override
    public int length()
    {
        return length;
    }

    @Override
    public long getNumberFound()
    {
        return numberFound;
    }

    @Override
    public NodeRef getNodeRef(int n)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public float getScore(int n)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void close()
    {
        // NO OP
    }

    @Override
    public ResultSetRow getRow(int i)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NodeRef> getNodeRefs()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ChildAssociationRef> getChildAssocRefs()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ChildAssociationRef getChildAssocRef(int n)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSetMetaData getResultSetMetaData()
    {
        return resultSetMetaData;
    }

    @Override
    public int getStart()
    {
        return 0;
    }

    @Override
    public boolean hasMore()
    {
        //Hard coded to 1000 and o pagination.
        return false;
    }

    @Override
    public boolean setBulkFetch(boolean bulkFetch)
    {
        //Not applicable.
        return false;
    }

    @Override
    public boolean getBulkFetch()
    {
        //Not applicable.
        return false;
    }

    @Override
    public int setBulkFetchSize(int bulkFetchSize)
    {
        //Not applicable.
        return 0;
    }

    @Override
    public int getBulkFetchSize()
    {
        //Not applicable.
        return 0;
    }

    @Override
    public List<Pair<String, Integer>> getFieldFacet(String field)
    {
        //Not applicable.
        return null;
    }

    @Override
    public Map<String, Integer> getFacetQueries()
    {
        //Not applicable.
        return null;
    }

    @Override
    public Map<NodeRef, List<Pair<String, List<String>>>> getHighlighting()
    {
        //Not applicable.
        return null;
    }

    @Override
    public SpellCheckResult getSpellCheckResult()
    {
        //Not applicable.
        return null;
    }

    @Override
    public Iterator<ResultSetRow> iterator()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Long getQueryTime()
    {
        return queryTime;
    }

    public String getSolrResponse()
    {
        return solrResponse;
    }

    public ArrayNode getDocs()
    {
        return docs;
    }

}
