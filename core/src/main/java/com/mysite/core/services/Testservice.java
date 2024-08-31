package com.mysite.core.services;

import com.day.cq.wcm.api.Page;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.io.IOException;


@Component(service = Testservice.class)
public class Testservice {
    private Logger log = LoggerFactory.getLogger(Testservice.class);

    public void checkMyService(SlingHttpServletRequest req, SlingHttpServletResponse res) throws IOException, RepositoryException {
        ResourceResolver resourceResolver = req.getResourceResolver();

        Resource Noderesource = resourceResolver.getResource("/content/mysite/us/en/mypage1");

        log.info("im inside service");
        if(Noderesource != null) {
            log.info("im inside if");
            Node myNode = Noderesource.adaptTo(Node.class);
            assert myNode != null;
                NodeIterator nodeIterator = myNode.getNodes("text");

                while (nodeIterator.hasNext()){
                    res.getWriter().write(nodeIterator.next().toString());
                }

            res.getWriter().write("done da bhaiyaaa");
        }else{
            res.getWriter().write("nulllll");
        }
   }
}
