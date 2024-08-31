package com.mysite.core.workflows;


import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.util.Arrays;
import java.util.Base64;

@Component(
        service = WorkflowProcess.class,
        immediate = true,
        property = {"process.label "+" = custom DAM Excel creation workflow"}
)
public class CustomDAMCreationWorkflow implements WorkflowProcess{

    @Override
    public void execute(WorkItem item, WorkflowSession session, MetaDataMap args) throws WorkflowException {
        Logger log = LoggerFactory.getLogger(CustomDAMCreationWorkflow.class);

        WorkflowData data = item.getWorkflowData();
        if(data.getPayloadType().equals("JCR_PATH")){
            String path = data.getPayload().toString();
            String[] fileName = args.get("PROCESS_ARGS","string").split(":");
            String method = "";

            ResourceResolver resourceResolver = session.adaptTo(ResourceResolver.class);
            log.error(path+"/"+fileName[1]+".xlsx");
            assert resourceResolver != null;
            Resource res = resourceResolver.getResource(path+"/"+fileName[1]+".xlsx");
            if(res == null){
                log.error("inside if");
                method = "POST";
            }else{
                log.error("inside else");
                method = "PUT";
            }

//            /content/dam/mysite/workflowExcel/workflowOutput.xlsx
            String[] myPath = path.split("dam/");
            String url = "http://localhost:4502/api/assets/" + myPath[1] + "/"+ fileName[1] + ".xlsx";

            try{
//            String filename = "test" + ".xlsx";
                XSSFWorkbook workbook = new XSSFWorkbook();
                XSSFSheet sheet = workbook.createSheet("assets");
                XSSFRow rowhead = sheet.createRow((short) 0);
                rowhead.createCell(0).setCellValue("S.No.");
                rowhead.createCell(1).setCellValue("asset");
                rowhead.createCell(2).setCellValue("No of reference");
                rowhead.createCell(3).setCellValue("asset path");

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                workbook.write(bos);
                workbook.close();
                byte[] fileByte = bos.toByteArray();

                URL reqUrl = new URI(url).toURL();
                HttpURLConnection connection = (HttpURLConnection) reqUrl.openConnection();
                connection.setRequestMethod(method);
                connection.setRequestProperty("Content-Type","application/vnd.openxmlformats-mydoc.spreadsheetml.sheet");
                connection.setRequestProperty("Content-Length",String.valueOf(fileByte.length));
                connection.setDoOutput(true);
                String userCredentials = "admin:admin";
                String basicAuth = "Basic " + Base64.getEncoder().encodeToString(userCredentials.getBytes());
                connection.setRequestProperty("Authorization", basicAuth);

                try(OutputStream os = connection.getOutputStream()){
                    os.write(fileByte);
                    os.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                int responseCode = connection.getResponseCode();
                log.error("successfull with status code "+ responseCode);

            } catch (RuntimeException | URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
