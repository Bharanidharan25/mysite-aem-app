package com.mysite.core.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.commons.ReferenceSearch;

@Component(service = Servlet.class)
@SlingServletPaths(value = {"/bin/assetReferrence"})
public class AssetReferenceServlet extends SlingAllMethodsServlet {

    Logger log = LoggerFactory.getLogger(AssetReferenceServlet.class);

    @Override
    protected void doPost(final SlingHttpServletRequest req, final SlingHttpServletResponse res) throws ServletException, IOException {

        String fileName = req.getParameter("reportName");
        String path = req.getParameter("path");
        ResourceResolver resourceResolver = req.getResourceResolver();
        List<String> assetList = getAllDamAsset(resourceResolver, path);

        ReferenceSearch referenceSearch = new ReferenceSearch();
        referenceSearch.setExact(true);
        referenceSearch.setHollow(true);
        referenceSearch.setMaxReferencesPerPage(-1);

        Map<String, Collection<ReferenceSearch.Info>> referencesAllAssets = new HashMap<>();

        for (String asset : assetList) {
            Collection<ReferenceSearch.Info> result = referenceSearch.search(resourceResolver, asset).values();
            referencesAllAssets.put(asset, result);
        }

        createExcel(referencesAllAssets, res, fileName);

    }

    private void createExcel(Map<String, Collection<ReferenceSearch.Info>> referencesAllAssets, SlingHttpServletResponse res, String fileName) throws IOException{
        try {
            String filename = fileName + ".xlsx";
            XSSFWorkbook workbook = new XSSFWorkbook(); 
            XSSFSheet sheet = workbook.createSheet("assets");
            XSSFRow rowhead = sheet.createRow((short) 0);
            rowhead.createCell(0).setCellValue("S.No.");
            rowhead.createCell(1).setCellValue("asset");
            rowhead.createCell(2).setCellValue("No of reference");
            rowhead.createCell(3).setCellValue("asset path");
            CellStyle cs = workbook.createCellStyle();
            cs.setWrapText(true);

            Integer rowNum = 1;
            for(Map.Entry<String, Collection<ReferenceSearch.Info>> entry : referencesAllAssets.entrySet()){
                XSSFRow myRow = sheet.createRow(rowNum);
                myRow.createCell(0).setCellValue(rowNum);
                myRow.createCell(1).setCellValue(entry.getKey());
                myRow.createCell(2).setCellValue(entry.getValue().size());

                XSSFCell mCell = myRow.createCell(3);
                mCell.setCellStyle(cs);
                
                String cellVal = "";
                for(ReferenceSearch.Info assetRefer : entry.getValue()){
                    cellVal = cellVal+ " \n "+ assetRefer.getPagePath();
                }
                mCell.setCellValue(cellVal);
                rowNum+=1;
            }

            res.setHeader("Content-disposition", "attachment;filename=" + filename);
            res.setHeader("charset", "iso-8859-1");
            res.setContentType("application/octet-stream");

            try (ServletOutputStream out = res.getOutputStream()) {
                workbook.write(out);
                out.flush();
            }
        }
        catch(IOException e){
            log.error(e.getMessage());
        }
    }

    private List<String> getAllDamAsset(ResourceResolver resourceResolver, String path) {
        List<String> damList = new ArrayList<>();
        try {
            Session session;
            session = resourceResolver.adaptTo(Session.class);
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            String queryString = "SELECT * FROM [dam:Asset] AS asset WHERE ISDESCENDANTNODE(asset,'" + path + "')";
            Query query = queryManager.createQuery(queryString, "JCR-SQL2");
            QueryResult queryResult = query.execute();
            RowIterator rowIterator = queryResult.getRows();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.nextRow();
                String rowString = row.toString();

                damList.add(rowString.substring(rowString.indexOf("/"), rowString.indexOf(" asset.jcr")));
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return damList;

    }

}
