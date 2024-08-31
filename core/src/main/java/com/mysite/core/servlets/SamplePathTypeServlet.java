package com.mysite.core.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import com.mysite.core.services.Testservice;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

@Component(service = Servlet.class)
@SlingServletPaths(value = { "/bin/test/pages", "/mysite/getPageData" })

public class SamplePathTypeServlet extends SlingAllMethodsServlet{
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(SamplePathTypeServlet.class);

	@Reference
	Testservice testservice;

    @Override
    protected void doGet(final SlingHttpServletRequest req, final SlingHttpServletResponse res)
            throws ServletException, IOException {

//        res.setHeader("Content-Type", "application/json");
//        res.setHeader("X-Content-Type-Options", "");
//        res.setHeader("Access-Control-Allow-Origin", "*");
//        final ResourceResolver resourceResolver = req.getResourceResolver();
//        JsonObject outputJson = new JsonObject();
//        getProjectAndSiteList(resourceResolver, outputJson);
//        res.setContentType("application/json");
//        res.getWriter().write(outputJson.toString());

        try {
            testservice.checkMyService(req,res);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }


    private void getProjectAndSiteList(ResourceResolver resourceResolver, JsonObject outputJson) {
	    // Get the project list
	    JsonArray projectList = new JsonArray();
	    Map<String, String> queryMap = getProjectListQuery();
	    QueryBuilder builder = resourceResolver.adaptTo(QueryBuilder.class);
	    Session session = resourceResolver.adaptTo(Session.class);
	    Query query = builder.createQuery(PredicateGroup.create(queryMap), session);
	    SearchResult result = query.getResult();
	    for (Hit hit : result.getHits()) {
	        try {
	            String projectName = hit.getResource().getName();
	            LOG.error("Page path: " + projectName);
	            projectList.add(new JsonPrimitive(projectName));
	        } catch (Exception e) {
	            LOG.error("Exception: " + e.getMessage());
	        }
	    }
	    outputJson.add("projectlist", projectList);
 
	    // Get the site list for each project
	    for (Hit hit : result.getHits()) {
	        try {
	            String projectName = hit.getResource().getName();
	            LOG.error("Page path: " + projectName);
	            JsonArray siteList = new JsonArray();
	            Page page = resourceResolver.adaptTo(PageManager.class).getPage("/content/" + projectName);
	            Iterator<Page> childPages = page.listChildren();
	            while (childPages.hasNext()) {
	                Page childPage = childPages.next();
	                String pageName = childPage.getName();
	                if (pageName != null) {
	                    siteList.add(new JsonPrimitive(pageName));
	                } else {
	                    LOG.error("Page Title is null for Page Path : " + pageName);
	                }
	            }
	            outputJson.add(projectName, siteList);
	        } catch (Exception e) {
	            LOG.error("Exception: " + e.getMessage());
	        }
	    }
	}

    private Map<String, String> getProjectListQuery() {
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("path", "/content");
        queryMap.put("type", "cq:Page");
        queryMap.put("property", "jcr:content/sling:configRef");
        queryMap.put("property.operation", "exists");
        return queryMap;
    }
}
