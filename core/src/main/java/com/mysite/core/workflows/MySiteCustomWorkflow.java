package com.mysite.core.workflows;


import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Component(
        service = WorkflowProcess.class,
        immediate = true,
        property= {"process.label"+ " = mysite workflow process"}
)
public class MySiteCustomWorkflow implements WorkflowProcess{

    Logger log = LoggerFactory.getLogger(MySiteCustomWorkflow.class);

    @Override
    public void execute(WorkItem item, WorkflowSession session, MetaDataMap args) throws WorkflowException {

        log.error("-----------------------inside workflow--------------------");
        WorkflowData data = item.getWorkflowData();
        if(data.getPayloadType().equals("JCR_PATH")){
            Session mySession = session.getSession();
            String pagePath = data.getPayload().toString()+ "/jcr:content";

            try {
                Node node = (Node)mySession.getNode(pagePath);
                String[] props = args.get("PROCESS_ARGS","string").split(":");
                node.setProperty(props[0],props[1]);
                log.error(node.getPath());
                log.error((String) args.get("PROCESS_ARGS","string"));

            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }


        }
    }
}
