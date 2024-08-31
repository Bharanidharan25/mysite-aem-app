package com.mysite.core.models;

import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.api.resource.Resource;

@Model(adaptables = Resource.class, resourceType = "/apps/mysite/components/testComponent6")
public class TestComponent6Model {

    @ValueMapValue
    private String bharani;


    public String getBharani() {
        return bharani;
    }

    public void setBharani(String bharani) {
        this.bharani = bharani;
    }



}
