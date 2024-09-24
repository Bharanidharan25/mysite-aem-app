package com.mysite.core.util;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

import java.util.HashMap;
import java.util.Map;

public final class ResolverUtil {

    public static final String MY_SERVICE_USER = "myserviceuser";
    /**
     * @param  resourceResolverFactory factory
     * @return new resource resolver for Sony service user
     * @throws LoginException if problems
     */
    public static ResourceResolver newResolver( ResourceResolverFactory resourceResolverFactory ) throws LoginException {
        final Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put( ResourceResolverFactory.SUBSERVICE, MY_SERVICE_USER );

        // fetches the admin service resolver using service user.
        return resourceResolverFactory.getServiceResourceResolver(paramMap);
    }


}