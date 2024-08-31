package com.mysite.core.workflows;


import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.ParticipantStepChooser;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.osgi.service.component.annotations.Component;

@Component(
        service = ParticipantStepChooser.class,
        immediate = true,
        property = {"chooser.label="+"Dynamic participant workflow"}
)
public class DynamicParticipantStep implements ParticipantStepChooser{
    @Override
    public String getParticipant(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap args) throws WorkflowException {
        WorkflowData data= workItem.getWorkflowData();
        String payload = data.getPayload().toString();
        if(payload.startsWith("/content/mysite/us")){
            return "testUser1";
        }else{
            return "admin";
        }
    }
}
