//package com.mysite.core.listeners;
//
//
//import org.apache.sling.api.resource.observation.ResourceChange;
//import org.apache.sling.api.resource.observation.ResourceChangeListener;
//import org.osgi.service.component.annotations.Component;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.List;
//
//@Component(
//        service = ResourceChangeListener.class,
//        immediate = true,
//        property = {
//            ResourceChangeListener.PATHS + "=" + "/content/mysite",
//            ResourceChangeListener.CHANGES + "=" + "ADDED",
//            ResourceChangeListener.CHANGES + "=" + "CHANGED",
//            ResourceChangeListener.CHANGES + "=" + "REMOVED"
//        }
//)
//public class MyResourceChangeListeners implements ResourceChangeListener{
//
//    private final Logger logger = LoggerFactory.getLogger(MyResourceChangeListeners.class);
//
//    @Override
//    public void onChange(List<ResourceChange> changes) {
//        changes.forEach(change -> {
//            logger.info("path -> {} \n type-> {}",change.getPath(), change.getType());
//        });
//    }
//}
