package com.mysite.core.servlets;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Base64;

@Component(service = Servlet.class)
@SlingServletPaths(value = {"/bin/createAsset"})
public class AssetAPIServlet extends SlingSafeMethodsServlet {

    Logger log = LoggerFactory.getLogger(AssetAPIServlet.class);

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        response.getWriter().write("im running \n");
        ResourceResolver resolver = request.getResourceResolver();
        Resource res = resolver.getResource("/content/dam/mysite/workflowExcel/workflowOutput.xlsx");
        String method = "";
        if(res == null){
            method = "POST";
        }else{
            method = "PUT";
        }

        String url = "http://localhost:4502/api/assets/mysite/workflowExcel/workflowOutput.xlsx";

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
            response.getWriter().write("successfully done with response code " + responseCode);

        } catch (RuntimeException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }
}
