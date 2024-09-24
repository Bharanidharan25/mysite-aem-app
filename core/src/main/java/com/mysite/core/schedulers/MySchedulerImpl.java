package com.mysite.core.schedulers;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.mysite.core.util.ResolverUtil;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;

@Component(
        service = MySchedulerImpl.class,
        immediate = true
)
@Designate(ocd = MySchedulerConfig.class)
public class MySchedulerImpl implements Runnable{

    private final Logger logger = LoggerFactory.getLogger(MySchedulerImpl.class);
    private String path;
    private ResourceResolver resourceResolver;

    @Reference
    Scheduler scheduler;

    @Reference
    private Replicator replicator;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Activate
    void activate(final MySchedulerConfig mySchedulerConfig) throws LoginException, ReplicationException {
        resourceResolver = ResolverUtil.newResolver(resourceResolverFactory);
        activateScheduler(mySchedulerConfig);


    }

    @Deactivate
    void deactivate(final MySchedulerConfig mySchedulerConfig){
        deactivateScheduler(mySchedulerConfig);
    }

    @Modified
    void modified(final MySchedulerConfig mySchedulerConfig) throws ReplicationException {
        deactivateScheduler(mySchedulerConfig);
        activateScheduler(mySchedulerConfig);
    }

    private void activateScheduler(MySchedulerConfig mySchedulerConfig) throws ReplicationException {
        logger.info("------ scheduler active -------");
        if(mySchedulerConfig.schedulerEnabled()){

            ScheduleOptions options = scheduler.EXPR(mySchedulerConfig.schedulerCronExp());
            logger.info("my cron exp {}",mySchedulerConfig.schedulerCronExp());
            options.name(mySchedulerConfig.schedulerName());
            options.canRunConcurrently(false);

            scheduler.schedule(this, options);
            path = mySchedulerConfig.schedulerAddProp();
            logger.info("{} scheduler started",mySchedulerConfig.schedulerName());
        }else{
            logger.info("scheduler is not active");
        }
    }

    private void deactivateScheduler(MySchedulerConfig mySchedulerConfig){
        logger.info("{} scheduler deactivated", mySchedulerConfig.schedulerName());
        scheduler.unschedule(mySchedulerConfig.schedulerName());
    }

    private void publishPage() throws ReplicationException {
        Session session = resourceResolver.adaptTo(Session.class);
        replicator.replicate(session, ReplicationActionType.ACTIVATE, path);
        logger.info("page is replicated successfully");
    }

    @Override
    public void run() {
        logger.info("my additional path -> {}",path);
        logger.info(" ---------> my Scheduler is running <----------");
        try {
            publishPage();
        } catch (ReplicationException e) {
            throw new RuntimeException(e);
        }
    }
}
