//package com.mysite.core.listeners;
//
//import com.day.cq.replication.ReplicatedAction;
//import org.apache.sling.api.SlingConstants;
//import org.osgi.service.event.Event;
//import org.osgi.service.event.EventConstants;
//import org.osgi.service.event.EventHandler;
//import org.osgi.service.component.annotations.Component;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//
//@Component(
//        service = EventHandler.class,
//        immediate = true,
//        property = {EventConstants.EVENT_TOPIC + "=org/apache/sling/api/resource/Resource/*"}
//
//)
//public class MyEventHandler implements EventHandler {
//
//    private final Logger logger = LoggerFactory.getLogger(MyEventHandler.class);
//
//    @Override
//    public void handleEvent(Event event) {
//        logger.info("Event Topic = {} \n path = {}", event.getTopic(),event.getProperty(SlingConstants.PROPERTY_PATH));
//    }
//}
