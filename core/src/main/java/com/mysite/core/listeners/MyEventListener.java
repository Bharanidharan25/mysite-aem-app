//package com.mysite.core.listeners;
//
//import org.apache.jackrabbit.spi.Event;
//import org.apache.sling.api.resource.LoginException;
//import org.apache.sling.api.resource.ResourceResolverFactory;
//import org.apache.sling.jcr.api.SlingRepository;
//import org.osgi.service.component.annotations.Component;
//import org.osgi.service.component.annotations.Reference;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.jcr.RepositoryException;
//import javax.jcr.Session;
//import javax.jcr.observation.EventIterator;
//import javax.jcr.observation.EventListener;
//
//@Component(
//        service = EventListener.class,
//        immediate = true
//)
//public class MyEventListener implements EventListener {
//
//    @Reference
//    private SlingRepository repository;
//
//    @Reference
//    private ResourceResolverFactory resourceResolverFactory;
//
//    Logger logger = LoggerFactory.getLogger(MyEventListener.class);
//    private Session session;
//
//    void activate() throws LoginException, RepositoryException {
//        logger.info("----inside activate ------");
//        session = repository.loginService("myserviceuser",null);
//        logger.info("---- resolver------");
//        assert session != null;
//        session.getWorkspace().getObservationManager().addEventListener(this, Event.PROPERTY_ADDED|Event.NODE_ADDED, "/content/mysite",true, null, null, false);
//        logger.info("---------- event triggered --------------");
//    }
//
//    void deactivate() throws RepositoryException {
//        if(session != null){
//            session.getWorkspace().getObservationManager().removeEventListener(this);
//            logger.info("---------- event removed --------------");
//            session.logout();
//        }
//    }
//
//    @Override
//    public void onEvent(EventIterator events) {
//        logger.info("event iter {}",events.getSize());
//        while (events.hasNext()){
//            try {
//                logger.info("------------------------------------------");
//                logger.info("event type => {}",events.nextEvent().getType());
//                logger.info("event path => {}", events.nextEvent().getPath());
//                logger.info("------------------------------------------");
//            } catch (RepositoryException e) {
//                logger.error("repo exp",e);
//            }
//        }
//    }
//}
