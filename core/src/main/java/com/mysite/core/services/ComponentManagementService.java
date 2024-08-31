package com.mysite.core.services;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysite.core.util.ResolverUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@Component(service = ComponentManagementService.class)
public class ComponentManagementService {

    private static final Logger log = LoggerFactory.getLogger(ComponentManagementService.class);
    // private static final String LOCAL_DIRECTORY = "C:\\AEM_Training\\AEM
    // 2023\\test\\Carry_Bag_AEM_Project\\ui.apps\\src\\main\\content\\jcr_root\\apps\\carry-bag\\components";
    private static final String templateFilePath = "/apps/components-builder/templates/model.template";
    private static final String htlTemplatePath = "/apps/components-builder/templates/htl.template";
    private static final String junitTemplatePath = "/apps/components-builder/templates/junit.template";
    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    public void manageComponent(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                JsonObject jsonObject) throws Exception {
        ResourceResolver resourceResolver = request.getResourceResolver();
        Gson gson = new Gson();
        log.error("Manage Init successfully");
        JsonObject componentObject = jsonObject.getAsJsonObject("component");
        log.error("Component JSON Object: {}", componentObject);
        // String rootPath =
        // componentObject.get("componentPath").getAsString().split("jcr_root")[1].replace("\\",
        // "/");
        String rootPath = componentObject.get("rootpath").getAsString();
        log.error("RootPath : " + rootPath);
        String componentName = componentObject.get("name").getAsString();
        String componentPath = rootPath + "/" + componentName;
        Resource componentResource = resourceResolver.getResource(componentPath);

        if (componentResource == null) {
            log.error("Inside if condition");
            createComponent(resourceResolver, componentObject, componentPath);
            log.error("Component created successfully");
            componentResource = resourceResolver.getResource(componentPath);
            if (componentResource == null) {
                throw new Exception("Component resource could not be found after creation.");
            }
        } else {
            updateComponent(resourceResolver, componentObject, componentResource);
        }

        String modelName = jsonObject.getAsJsonObject("component").get("name").getAsString();
        // String resourceType = "cognizant-componentgenerator/components/" + modelName;
        JsonArray tabs = jsonObject.getAsJsonObject("dialog").getAsJsonArray("tabs");
        JsonArray itemNames = extractItemNames(tabs, "name");

        String propertiesString = generatePropertiesString(itemNames);
        String gettersAndSetters = generateGettersAndSetters(itemNames);
        generateHTLFile(request, jsonObject, componentName);

        createDialog(resourceResolver, componentObject, componentResource, jsonObject);
        log.error("Dialog created successfully");
        createEditConfigListener(resourceResolver, componentObject, componentResource);
        createLocalEditConfig(componentObject, componentName);
        createClientlibs(componentObject, componentResource);
        createLocalClientlibs(componentName, componentObject);
        generateComponentContentXML(componentResource, componentName, jsonObject);
        generateDialogContentXML(request, componentResource, jsonObject);
        // generateSlingModel(request, modelName, propertiesString, gettersAndSetters,
        // componentPath, jsonObject);
        String slingModelFlagg = componentObject.get("generateSlingModel").getAsString();
        if (slingModelFlagg.equalsIgnoreCase("true")) {
            generateSlingModel(request, modelName, propertiesString, gettersAndSetters, componentPath, jsonObject, itemNames);
        }
        log.error("Manage Component method executed successfully");
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(jsonObject));
    }

    private void createEditConfigListener(ResourceResolver resourceResolver, JsonObject componentObject,
                                          Resource componentResource) throws PersistenceException {
        String cqEditPath = componentResource.getPath() + "/cq:editConfig";
        Resource cqEditNodeResource = resourceResolver.getResource(cqEditPath);
        if (cqEditNodeResource == null) {
            Map<String, Object> cqEditNodeProperties = new HashMap<>();
            cqEditNodeProperties.put("jcr:primaryType", "cq:EditConfig");
            cqEditNodeResource = resourceResolver.create(componentResource, "cq:editConfig", cqEditNodeProperties);
        }
        String listenerPath = cqEditPath + "/cq:listeners";
        Resource listenerPathResource = resourceResolver.getResource(listenerPath);
        if (listenerPathResource == null) {
            Map<String, Object> listenerNodeProperties = new HashMap<>();
            listenerNodeProperties.put("afterdelete", "REFRESH_PAGE");
            listenerNodeProperties.put("afteredit", "REFRESH_PAGE");
            listenerNodeProperties.put("afterinsert", "REFRESH_PAGE");
            listenerNodeProperties.put("aftermove", "REFRESH_PAGE");
            listenerNodeProperties.put("jcr:primaryType", "cq:EditListenersConfig");
            listenerPathResource = resourceResolver.create(cqEditNodeResource, "cq:listeners", listenerNodeProperties);
            log.error("Edit Config method executed successfully");
        }
        resourceResolver.commit();
    }

    private void createLocalEditConfig(JsonObject componentObject, String componentName) {
        try {
            String LOCAL_DIRECTORY = componentObject.get("componentPath").getAsString();
            File componentDir = new File(LOCAL_DIRECTORY+ "/" + componentName, "_cq_editConfig.xml");
            if (!componentDir.exists()) {
                try (FileWriter writer = new FileWriter(componentDir)) {
                    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                    writer.write("<jcr:root xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" xmlns:cq=\"http://www.day.com/jcr/cq/1.0\"\n");
                    writer.write("    cq:actions=\"[text: Link List: ,edit,delete,insert,copymove]\"\n");
                    writer.write("    cq:dialogMode=\"floating\"\n");
                    writer.write("    cq:layout=\"editbar\"\n");
                    writer.write("    jcr:primaryType=\"cq:EditConfig\">\n");
                    writer.write("    <cq:listeners\n");
                    writer.write("        jcr:primaryType=\"cq:EditListenersConfig\"\n");
                    writer.write("        afterdelete=\"REFRESH_PAGE\"\n");
                    writer.write("        afteredit=\"REFRESH_PAGE\"\n");
                    writer.write("        afterinsert=\"REFRESH_PAGE\"\n");
                    writer.write("        aftermove=\"REFRESH_PAGE\"/>\n");
                    writer.write("</jcr:root>");
                }
                log.error("Local _cq_editConfig.xml file created at: {}", componentDir.getAbsolutePath());
            }

        } catch (IOException e) {
            log.error("Error in creating local edit config: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void createClientlibs(JsonObject componentObject,
                                  Resource componentResource) throws PersistenceException {
        try {
            ResourceResolver resourceResolver = ResolverUtil.newResolver(resourceResolverFactory);
            Session session = resourceResolver.adaptTo(Session.class);

            String clientlibs = componentResource.getPath() + "/clientlibs";
            Resource cqEditNodeResource = resourceResolver.getResource(clientlibs);
            String componentName = componentObject.get("name").getAsString();
            if (cqEditNodeResource == null) {
                Map<String, Object> cqEditNodeProperties = new HashMap<>();
                cqEditNodeProperties.put("jcr:primaryType", "cq:ClientLibraryFolder");
                cqEditNodeProperties.put("categories", componentName);
                cqEditNodeResource = resourceResolver.create(componentResource, "clientlibs", cqEditNodeProperties);
            }
            String jsPath = clientlibs + "/js";
            Resource jsPathResource = resourceResolver.getResource(jsPath);
            if (jsPathResource == null) {
                Map<String, Object> listenerNodeProperties = new HashMap<>();
                listenerNodeProperties.put("jcr:primaryType", "nt:folder");
                jsPathResource = resourceResolver.create(cqEditNodeResource, "js", listenerNodeProperties);
                log.error("Clientlib /JS method executed successfully");
            }
            try {
                if (session != null) {
                    InputStream inputStream = new ByteArrayInputStream(new byte[0]);
                    Node jsfilePath = session.getNode(jsPath);
                    Node jsfileNode = jsfilePath.addNode(componentName + ".js", JcrConstants.NT_FILE);
                    Node contentNode = jsfileNode.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
                    ValueFactory valueFactory = session.getValueFactory();
                    Binary binary = valueFactory.createBinary(inputStream);
                    contentNode.setProperty(JcrConstants.JCR_DATA, binary);
                    contentNode.setProperty(JcrConstants.JCR_MIMETYPE, "application/javascript");
                    log.error("JS file created at: {}", contentNode.getPath());
                    session.save();
                }
            } catch (Exception e) {
                log.error("Error in creating JS file : {}", e.getMessage());
                e.printStackTrace();
            }
            String cssPath = clientlibs + "/css";
            Resource cssPathResource = resourceResolver.getResource(cssPath);
            if (cssPathResource == null) {
                Map<String, Object> listenerNodeProperties = new HashMap<>();
                listenerNodeProperties.put("jcr:primaryType", "nt:folder");
                cssPathResource = resourceResolver.create(cqEditNodeResource, "css", listenerNodeProperties);
                log.error("Clientlib /CSS method executed successfully");
            }
            try {
                if (session != null) {
                    InputStream inputStream = new ByteArrayInputStream(new byte[0]);
                    Node cssfilePath = session.getNode(cssPath);
                    Node cssfileNode = cssfilePath.addNode(componentName + ".css", JcrConstants.NT_FILE);
                    Node contentNode = cssfileNode.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
                    ValueFactory valueFactory = session.getValueFactory();
                    Binary binary = valueFactory.createBinary(inputStream);
                    contentNode.setProperty(JcrConstants.JCR_DATA, binary);
                    contentNode.setProperty(JcrConstants.JCR_MIMETYPE, "text/css");
                    log.error("JS file created at: {}", contentNode.getPath());
                    session.save();
                }
            } catch (Exception e) {
                log.error("Error in creating CSS file : {}", e.getMessage());
                e.printStackTrace();
            }
            String cssFilePath = clientlibs + "/css.txt";
            Resource cssFilePathResource = resourceResolver.getResource(cssFilePath);
            log.error("CSS File Path : {}", cssFilePathResource);
            if (cssFilePathResource == null) {
                String content = "#base=css\n" + componentName + ".js";
                InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
                Node cssfilePath = session.getNode(clientlibs);
                Node cssfileNode = cssfilePath.addNode("css.txt", JcrConstants.NT_FILE);
                Node contentNode = cssfileNode.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
                ValueFactory valueFactory = session.getValueFactory();
                Binary binary = valueFactory.createBinary(inputStream);
                contentNode.setProperty(JcrConstants.JCR_DATA, binary);
                contentNode.setProperty(JcrConstants.JCR_MIMETYPE, "text/css");
                log.error("Clientlib CSS.TXT method executed successfully");
                session.save();
            }
            String jsFilePath = clientlibs + "/js.txt";
            Resource jsFilePathResource = resourceResolver.getResource(jsFilePath);
            if (jsFilePathResource == null) {
                String content = "#base=js\n" + componentName + ".js";
                InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
                Node jsfilePath = session.getNode(clientlibs);
                Node jsfileNode = jsfilePath.addNode("js.txt", JcrConstants.NT_FILE);
                Node contentNode = jsfileNode.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
                ValueFactory valueFactory = session.getValueFactory();
                Binary binary = valueFactory.createBinary(inputStream);
                contentNode.setProperty(JcrConstants.JCR_DATA, binary);
                contentNode.setProperty(JcrConstants.JCR_MIMETYPE, "text/js");
                log.error("Clientlib JS.TXT method executed successfully");
            }
            resourceResolver.commit();
        } catch (Exception e) {
            log.error("Error in creating Clientlibs : {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void createLocalClientlibs(String componentName, JsonObject componentObject) {
        try {
            String LOCAL_DIRECTORY = componentObject.get("componentPath").getAsString();
            String clientlibpath = LOCAL_DIRECTORY  + componentName + "\\clientlibs";
            log.error("Clientlib Path : {}", clientlibpath);
            File clientlibsDir = new File(LOCAL_DIRECTORY + "/" + componentName, "clientlibs");
            if (!clientlibsDir.exists()) {
                clientlibsDir.mkdirs();
            }

            File jsDir = new File(clientlibsDir, "js");
            if (!jsDir.exists()) {
                jsDir.mkdirs();
            }

            File cssDir = new File(clientlibsDir, "css");
            if (!cssDir.exists()) {
                cssDir.mkdirs();
            }

            // Create JS file
            File jsFile = new File(jsDir, componentName + ".js");
            if (!jsFile.exists()) {
                try (FileWriter writer = new FileWriter(jsFile)) {
                    writer.write(""); // Write empty content
                }
                log.error("Local JS file created at: {}", jsFile.getAbsolutePath());
            }

            // Create CSS file
            File cssFile = new File(cssDir, componentName + ".css");
            if (!cssFile.exists()) {
                try (FileWriter writer = new FileWriter(cssFile)) {
                    writer.write(""); // Write empty content
                }
                log.error("Local CSS file created at: {}", cssFile.getAbsolutePath());
            }

            // Create css.txt file
            File cssTxtFile = new File(clientlibsDir, "css.txt");
            if (!cssTxtFile.exists()) {
                try (FileWriter writer = new FileWriter(cssTxtFile)) {
                    writer.write("#base=css\n" + componentName + ".css");
                }
                log.error("Local css.txt file created at: {}", cssTxtFile.getAbsolutePath());
            }

            // Create js.txt file
            File jsTxtFile = new File(clientlibsDir, "js.txt");
            if (!jsTxtFile.exists()) {
                try (FileWriter writer = new FileWriter(jsTxtFile)) {
                    writer.write("#base=js\n" + componentName + ".js");
                }
                log.error("Local js.txt file created at: {}", jsTxtFile.getAbsolutePath());
            }

        } catch (IOException e) {
            log.error("Error in creating local clientlibs: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void createComponent(ResourceResolver resourceResolver, JsonObject componentObject, String componentPath)
            throws PersistenceException {

        String rootPath = componentObject.get("rootpath").getAsString();
        Map<String, Object> componentProperties = new HashMap<>();
        componentProperties.put("jcr:primaryType", "cq:Component");
        componentProperties.put("componentGroup", componentObject.get("group").getAsString());
        componentProperties.put("jcr:title", componentObject.get("title").getAsString());
        componentProperties.put("jcr:description", componentObject.get("description").getAsString());
        if (componentObject.has("supertype") && !componentObject.get("supertype").isJsonNull()) {
            componentProperties.put("sling:resourceSuperType", componentObject.get("supertype").getAsString());
        }
        log.error("Component path : " + componentPath);
        resourceResolver.create(resourceResolver.getResource(rootPath),
                componentObject.get("name").getAsString(), componentProperties);
        log.error("Create Component method executed successfully");
        resourceResolver.commit();
    }

    private void updateComponent(ResourceResolver resourceResolver, JsonObject componentObject,
                                 Resource componentResource) throws PersistenceException {
        ModifiableValueMap valueMap = componentResource.adaptTo(ModifiableValueMap.class);
        valueMap.put("jcr:title", componentObject.get("title").getAsString());
        valueMap.put("jcr:description", componentObject.get("description").getAsString());
        log.error("Update Component method executed successfully");
        resourceResolver.commit();
    }

    private void createDialog(ResourceResolver resourceResolver, JsonObject componentObject, Resource componentResource,
                              JsonObject jsonObject) throws PersistenceException {
        String dialogPath = componentResource.getPath() + "/cq:dialog";
        Resource dialogResource = resourceResolver.getResource(dialogPath);

        if (dialogResource == null) {
            Map<String, Object> dialogProperties = new HashMap<>();
            dialogProperties.put("jcr:primaryType", "nt:unstructured");
            dialogProperties.put("jcr:title", componentObject.get("title").getAsString());
            dialogProperties.put("extraClientlibs", componentObject.get("title").getAsString());
            dialogProperties.put("sling:resourceType", "cq/gui/components/authoring/dialog");
            dialogResource = resourceResolver.create(componentResource, "cq:dialog", dialogProperties);
            log.error("Created Component Dialog executed successfully");
        }

        // Create content node
        String contentPath = dialogPath + "/content";
        Resource contentResource = resourceResolver.getResource(contentPath);
        if (contentResource == null) {
            Map<String, Object> contentProperties = new HashMap<>();
            contentProperties.put("jcr:primaryType", "nt:unstructured");
            contentProperties.put("sling:resourceType", "granite/ui/components/coral/foundation/fixedcolumns");
            contentResource = resourceResolver.create(dialogResource, "content", contentProperties);
            log.error("Created Dialog fixedcolumns executed successfully");
        }

        // Create items node under content
        String itemsPath = contentPath + "/items";
        Resource itemsResource = resourceResolver.getResource(itemsPath);
        if (itemsResource == null) {
            itemsResource = resourceResolver.create(contentResource, "items", new HashMap<>());
        }

        // Create tabs node
        String tabsPath = itemsPath + "/tabs";
        Resource tabsResource = resourceResolver.getResource(tabsPath);
        if (tabsResource == null) {
            Map<String, Object> tabsProperties = new HashMap<>();
            tabsProperties.put("jcr:primaryType", "nt:unstructured");
            tabsProperties.put("sling:resourceType", "granite/ui/components/coral/foundation/tabs");
            tabsResource = resourceResolver.create(itemsResource, "tabs", tabsProperties);
            log.error("Created Dialog tab executed successfully");
        }

        // Create tabs items node
        String tabsItemsPath = tabsPath + "/items";
        Resource tabsItemsResource = resourceResolver.getResource(tabsItemsPath);
        if (tabsItemsResource == null) {
            tabsItemsResource = resourceResolver.create(tabsResource, "items", new HashMap<>());
        }

        // Create child nodes under tabs items dynamically
        JsonArray tabsArray = jsonObject.getAsJsonObject("dialog").getAsJsonArray("tabs");
        for (JsonElement tabElement : tabsArray) {
            JsonObject tabObject = tabElement.getAsJsonObject();
            String tabName = tabObject.get("name").getAsString();
            String tabItemPath = tabsItemsPath + "/" + tabName;
            Resource tabItemResource = resourceResolver.getResource(tabItemPath);
            if (tabItemResource == null) {
                Map<String, Object> tabItemProperties = new HashMap<>();
                tabItemProperties.put("jcr:primaryType", "nt:unstructured");
                tabItemProperties.put("sling:resourceType", "granite/ui/components/coral/foundation/container");
                tabItemProperties.put("jcr:title", tabName);
                tabItemResource = resourceResolver.create(tabsItemsResource, tabName, tabItemProperties);
                log.error("Created Dialog container executed successfully");
                // Create items node under tab item
                // String itemsNodePath = tabItemPath + "/items";
                Resource itemsNodeResource = resourceResolver.create(tabItemResource, "items", new HashMap<>());
                // String checkboxText = componentObject.get("fieldLabel").getAsString();
                // Create child nodes under items dynamically
                JsonArray itemsArray = tabObject.getAsJsonArray("items");
                for (JsonElement itemElement : itemsArray) {
                    JsonObject itemObject = itemElement.getAsJsonObject();
                    String type = itemObject.get("type").getAsString();
                    String checkboxText = itemObject.get("fieldLabel").getAsString();
                    Map<String, Object> itemProperties = new HashMap<>();
                    for (Map.Entry<String, JsonElement> itemEntry : itemObject.entrySet()) {
                        if (itemEntry.getKey().equals("name")) {
                            itemProperties.put(itemEntry.getKey(), "./" + itemEntry.getValue().getAsString());
                        } else {
                            itemProperties.put(itemEntry.getKey(), itemEntry.getValue().getAsString());
                        }
                    }
                    itemProperties.put("jcr:primaryType", "nt:unstructured");
                    // itemProperties.put("sling:resourceType",
                    // "granite/ui/components/coral/foundation/form/" + type);
                    switch (type) {
                        case "textfield":
                            itemProperties.put("sling:resourceType",
                                    "granite/ui/components/coral/foundation/form/textfield");
                            itemProperties.put("required", itemObject.get("required").getAsBoolean());
                            itemProperties.put("placeholder", "Enter text here"); // Example static property
                            break;
                        case "checkbox":

                            itemProperties.put("sling:resourceType",
                                    "granite/ui/components/coral/foundation/form/checkbox");
                            itemProperties.put("checkValue", true); // Boolean value for checkbox
                            itemProperties.put("text", checkboxText);
                            itemProperties.put("required", itemObject.get("required").getAsBoolean());
                            break;
                        case "richText":
                            itemProperties.put("sling:resourceType",
                                    "cq/gui/components/authoring/dialog/richtext");
                            itemProperties.put("useFixedInlineToolbar", true); // Boolean property
                            itemProperties.put("required", itemObject.get("required").getAsBoolean());
                            break;
                        case "image":
                            itemProperties.put("sling:resourceType", "cq/gui/components/authoring/dialog/fileupload");
                            itemProperties.put("fileNameParameter", "./fileName");
                            itemProperties.put("fileReferenceParameter", "./filereference");
                            itemProperties.put("mimeTypes",
                                    new String[] { "image/gif", "image/jpeg", "image/png", "image/tiff",
                                            "image/svg+xml" });
                            itemProperties.put("multiple", false); // Boolean
                            itemProperties.put("uploadUrl", "${suffix.path}");
                            itemProperties.put("useHTML5", true); // Boolean
                            itemProperties.put("required", itemObject.get("required").getAsBoolean());
                            break;
                        default:
                            log.warn("Unknown type: {}", type);
                            break;
                    }
                    resourceResolver.create(itemsNodeResource, type, itemProperties);
                }
            }
        }
        log.error("Create Dialog method executed successfully");
        resourceResolver.commit();
    }

    private void generateComponentContentXML(Resource componentResource, String name, JsonObject jsonObject)
            throws IOException, RepositoryException {
        ModifiableValueMap componentProperties = componentResource.adaptTo(ModifiableValueMap.class);
        String componentName = componentProperties.get("jcr:title", String.class);
        String componentDescription = componentProperties.get("jcr:description", String.class);
        JsonObject componentObject = jsonObject.getAsJsonObject("component");
        String LOCAL_DIRECTORY = componentObject.get("componentPath").getAsString();
        String componentGroup = componentObject.get("group").getAsString();
        File componentDir = new File(LOCAL_DIRECTORY, componentName);
        String resourceSuperType = componentProperties.get("sling:resourceSuperType", String.class);

        componentDir.mkdirs();
        File componentXML = new File(componentDir, ".content.xml");

        try (FileWriter writer = new FileWriter(componentXML)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write(
                    "<jcr:root xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" xmlns:cq=\"http://www.day.com/jcr/cq/1.0\" xmlns:sling=\"http://sling.apache.org/jcr/sling/1.0\" jcr:primaryType=\"cq:Component\" jcr:title=\""
                            + componentName + "\" jcr:description=\"" + componentDescription + "\" componentGroup=\""
                            + componentGroup + "\"");
            if (resourceSuperType != null) {
                writer.write(" sling:resourceSuperType=\"" + resourceSuperType + "\"");
            }
            writer.write(" />");
        }
        log.error("Generate Component Content XML method executed successfully");

    }

    private void generateDialogContentXML(SlingHttpServletRequest req, Resource componentResource,
                                          JsonObject jsonObject) throws IOException {
        // Get the dialog resource
        Resource dialogResource = componentResource.getChild("cq:dialog");
        if (dialogResource != null) {
            // Build the URL to fetch the dialog XML structure using the request object
            String componentPath = componentResource.getPath(); // Get the path of the component
            String dialogURL = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + componentPath
                    + "/cq:dialog.xml";
            log.info("dialogURL: {}", dialogURL);
            log.info("Fetching dialog XML from URL: {}", dialogURL); // Log the URL

            // Use the component name for the dialog directory
            ModifiableValueMap componentProperties = componentResource.adaptTo(ModifiableValueMap.class);
            String componentName = componentProperties.get("jcr:title", String.class);
            JsonObject componentObject = jsonObject.getAsJsonObject("component");
            String LOCAL_DIRECTORY = componentObject.get("componentPath").getAsString();
            File dialogDir = new File(LOCAL_DIRECTORY, componentName + "/_cq_dialog"); // Use componentName here
            dialogDir.mkdirs();
            File dialogXML = new File(dialogDir, ".content.xml");
            log.error("Generate Dialog XML into local directory executed successfully");
            // Fetch the XML structure from the URL and write it to the file
            try (FileWriter writer = new FileWriter(dialogXML)) {
                // Open a connection to the URL
                URL url = new URL(dialogURL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                // Set Basic Authentication
                String userCredentials = "admin:admin"; // Replace with your actual username and password
                String basicAuth = "Basic " + Base64.getEncoder().encodeToString(userCredentials.getBytes());
                conn.setRequestProperty("Authorization", basicAuth);

                int responseCode = conn.getResponseCode();
                log.info("Response Code: {}", responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;

                    // Read the response
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine).append("\n");
                    }
                    in.close();

                    // Write the response to the content.xml file
                    writer.write(response.toString());
                } else {
                    log.error("Failed to fetch dialog XML: HTTP error code " + responseCode);
                }
            }
        } else {
            log.error("Dialog resource not found under component: {}", componentResource.getPath());
        }
    }

    private void writeToCRXDE(SlingHttpServletRequest req, String componentPath, String content, JsonObject jsonObject)
            throws IOException, RepositoryException {
        try (ResourceResolver resourceResolver = ResolverUtil.newResolver(resourceResolverFactory)) {
            Session session = resourceResolver.adaptTo(Session.class);
            JsonObject componentObject = jsonObject.getAsJsonObject("component");
            String fileName = componentObject.get("name").getAsString() + ".html";
            log.error("Filename : {} ", fileName);
            // String fileContent = "<html><body><h1>Hello, World!</h1></body></html>";
            String htlPath = componentPath;
            if (session != null) {
                // Create the file node
                Node fileNode = session.getNode(htlPath);
                Node htmlfileNode = fileNode.addNode(fileName, JcrConstants.NT_FILE);
                Node contentNode = htmlfileNode.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);

                ValueFactory valueFactory = session.getValueFactory();
                InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
                Binary binary = valueFactory.createBinary(stream);
                contentNode.setProperty(JcrConstants.JCR_DATA, binary);
                contentNode.setProperty(JcrConstants.JCR_MIMETYPE, "text/html");
                log.error("HTML file created at: {}", contentNode.getPath());
                // Save the session
                session.save();
                log.error("writeToCRXDE method executed successfully");
            }
        } catch (Exception e) {
            log.error("Error in creating HTL file : {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private String readTemplateFromCRXDE(SlingHttpServletRequest req, String templatePath) throws IOException {
        ResourceResolver resourceResolver = req.getResourceResolver();
        Resource templateResource = resourceResolver.getResource(templatePath);

        if (templateResource != null) {
            StringBuilder templateContent = new StringBuilder();
            try (InputStreamReader reader = new InputStreamReader(
                    Objects.requireNonNull(templateResource.adaptTo(InputStream.class)))) {
                BufferedReader bufferedReader = new BufferedReader(reader);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    templateContent.append(line).append("\n");
                }
            }
            log.error("readTemplateFromCRXDE method executed successfully");
            return templateContent.toString();
        } else {
            throw new IOException("Template resource not found at: " + templatePath);
        }
    }

    private void generateHTLFile(SlingHttpServletRequest request, JsonObject jsonObject, String componentName)
            throws IOException {
        StringBuilder textBuilder = new StringBuilder();
        StringBuilder toggleBuilder = new StringBuilder();
        StringBuilder richTextBuilder = new StringBuilder(); // For rich text
        StringBuilder imageBuilder = new StringBuilder(); // For image
        String modelClassName = capitalize(componentName) + "Model";
        String textFieldProperties = "";
        String toggleFieldProperties = "";
        JsonObject componentObject = jsonObject.getAsJsonObject("component");
        String basePath = componentObject.get("corePath").getAsString().split("java")[1].replaceFirst("\\\\", "");
        String packagePath = basePath.replace("\\", ".");
        String slingModelFlag = componentObject.get("generateSlingModel").getAsString();
        String model = "model";
        String properties = "properties";
        String htlValue = "";
        String modelValue = "";
        String placeholder = "";
        StringBuilder existsConditionBuilder = new StringBuilder(); // For dynamic existence checks
        //String placeholder = "<sly data-sly-call=\"${template.placeholder @ isEmpty=!{{dialogObject}}, classAppend='cmp-{{componentName}}'}\" />";
        String rootPath = componentObject.get("rootpath").getAsString();
        String componentPath = rootPath + "/" + componentName;
        // Parse the JSON string
        JsonObject dialogObject = jsonObject.getAsJsonObject("dialog");
        JsonArray tabsArray = dialogObject.getAsJsonArray("tabs");
        int value = tabsArray.size();
        // if (value >= 1) {
        //     placeholder = "<sly data-sly-call=\"${template.placeholder @ isEmpty=!{{dialogObject}}, classAppend='cmp-{{componentName}}'}\" <\\sly>";
        // }
        // Iterate through tabs
        for (JsonElement tabElement : tabsArray) {
            JsonObject tabObject = tabElement.getAsJsonObject();
            JsonArray itemsArray = tabObject.getAsJsonArray("items");

            // Iterate through items
            for (JsonElement itemElement : itemsArray) {
                JsonObject itemObject = itemElement.getAsJsonObject();
                String name = itemObject.get("name").getAsString();
                String type = itemObject.get("type").getAsString();
                if (slingModelFlag.equalsIgnoreCase("true")) {
                    htlValue = model + "." + toCamelCase(name);
                    modelValue = "data-sly-use.model =" + "\"" + packagePath + ".models" + "." + modelClassName + "\"";
                } else {
                    htlValue = properties + "." + toCamelCase(name);
                }
                // Build the exists condition
                if (existsConditionBuilder.length() > 0) {
                    existsConditionBuilder.append(" || ");
                }
                existsConditionBuilder.append(htlValue);
                log.error("existsConditionBuilder: {}", existsConditionBuilder);
                if ("textfield".equals(type)) {
                    // placeholder = "data-sly-call=\"${template.placeholder @ isEmpty=!"+ htlValue+
                    // ", classAppend='cmp-{{componentName}}'}\"";
                    // Build HTML for text field
                    textBuilder.append("<div class=\"cmp-textfield\" data-sly-test=\"${")
                            .append(htlValue)
                            .append("}\">")
                            .append("<label>Entered Text:</label>")
                            .append("<p>${")
                            .append(htlValue)
                            .append("}</p></div>\n");
                } else if ("checkbox".equals(type)) {
                    // Build HTML for checkbox
                    toggleBuilder.append("<div class=\"cmp-textfield\" data-sly-test=\"${")
                            .append(htlValue)
                            .append(" != null}\">")
                            .append("<label>Checkbox Status:</label>")
                            .append("<p>${")
                            .append(htlValue)
                            .append(" ? 'Checked' : 'Unchecked'}</p></div>\n");
                } else if ("richText".equals(type)) {
                    // Build HTML for rich text
                    richTextBuilder.append("<div class=\"cmp-richtext\" data-sly-test=\"${")
                            .append(htlValue)
                            .append("}\"><label>Rich Text Content:</label><div>${")
                            .append(htlValue)
                            .append("}</div></div>\n");
                } else if ("image".equals(type)) {
                    // Build HTML for image with fileReference
                    imageBuilder.append("<div class=\"cmp-image\" data-sly-test=\"${")
                            .append(htlValue)
                            .append("}\"><label>Uploaded Image:</label><img src=\"${")
                            .append(htlValue)
                            .append("}\" alt=\"Image not found\" /></div>\n");
                }
            }
        }
        // Replace placeholders in the HTL template
        if (textBuilder != null) {
            textFieldProperties = textBuilder.toString();
        }
        if (toggleBuilder != null) {
            toggleFieldProperties = toggleBuilder.toString();
        }

        String htlTemplate = readTemplateFromCRXDE(request, htlTemplatePath);
        String htlContent = htlTemplate
                .replace("{{componentName}}", componentName)
                .replace("{{templatePlaceholder}}", placeholder)
                .replace("{{componentModelValue}}", modelValue)
                .replace("{{textField}}", textFieldProperties)
                .replace("{{checkbox}}", toggleFieldProperties)
                .replace("{{richText}}", richTextBuilder.toString()) // Replace rich text content
                .replace("{{image}}", imageBuilder.toString()) // Replace image content
                .replace("{{existsCondition}}", existsConditionBuilder.toString()); // Add the exists condition

        // Create the HTL directory if it doesn't exist
        // JsonObject componentObject = jsonObject.getAsJsonObject("component");
        String LOCAL_DIRECTORY = componentObject.get("componentPath").getAsString();
        File componentDir = new File(LOCAL_DIRECTORY, componentName);
        File componentHtl = new File(componentDir, componentName + ".html");

        componentDir.mkdirs();
        try (FileWriter writer = new FileWriter(componentHtl)) {
            writer.write(htlContent);
        }
        try {
            writeToCRXDE(request, componentPath, htlContent, jsonObject);
        } catch (Exception e) {
            log.error("Error in creating HTL file: {}", e.getMessage());
            e.printStackTrace();
        }
        log.error("generateHTLFile method executed successfully");
    }

    private String generatePropertiesString(JsonArray itemNames) {
        StringBuilder propertiesBuilder = new StringBuilder();
        for (int i = 0; i < itemNames.size(); i++) {
            String key = itemNames.get(i).getAsString();
            propertiesBuilder.append("    @ValueMapValue\n");
            propertiesBuilder.append("    private String ").append(toCamelCase(key)).append(";\n\n");
        }
        log.error("generatePropertiesString method executed successfully");
        return propertiesBuilder.toString();
    }

    private String generateGettersAndSetters(JsonArray itemNames) {
        StringBuilder methodsBuilder = new StringBuilder();
        for (int i = 0; i < itemNames.size(); i++) {
            String key = itemNames.get(i).getAsString();
            // Getter
            methodsBuilder.append("    public String get").append(capitalize(key)).append("() {\n");
            methodsBuilder.append("        return ").append(toCamelCase(key)).append(";\n");
            methodsBuilder.append("    }\n\n");

            // Setter
            methodsBuilder.append("    public void set").append(capitalize(key)).append("(String ").append(key)
                    .append(") {\n");
            methodsBuilder.append("        this.").append(toCamelCase(key)).append(" = ").append(toCamelCase(key))
                    .append(";\n");
            methodsBuilder.append("    }\n\n");
        }
        log.error("generateGettersAndSetters method executed successfully");
        return methodsBuilder.toString();
    }

    private String generateJunitBody(JsonArray itemNames, String slingModelName){
        StringBuilder junitBodyBuilder = new StringBuilder();
        junitBodyBuilder.append("   modelObj = new ").append(slingModelName).append("(); \n");

        for(int i = 0; i< itemNames.size(); i++){
            String key = itemNames.get(i).getAsString();
            junitBodyBuilder.append("       modelObj").append(".set").append(capitalize(key)).append("(\"testValue\"); \n");
            junitBodyBuilder.append("       assertEquals(\"testValue\",").append("modelObj.get").append(capitalize(key)).append("()); \n");
        }
        return  junitBodyBuilder.toString();
    }

    private void generteJunitFile(SlingHttpServletRequest req, String slingModelName, JsonArray itemNames, String packagePath, String testFilePath) throws IOException{
        String junitClassName =  capitalize(slingModelName) + "Test";
        String modelObj = slingModelName + " modelObj;";
        String junitTemplate = readTemplateFromCRXDE(req, junitTemplatePath);

        //generating junit body content
        String junitBody = generateJunitBody(itemNames, slingModelName);

        //replacing content in templates
        String junitContent = junitTemplate
                .replace("{{packageName}}", packagePath+ ".models")
                .replace("{{junitClassName}}", junitClassName)
                .replace("{{modelObj}}", modelObj)
                .replace("{{testBody}}", junitBody);

        log.info("junit path -> "+ testFilePath);
        //generating junit file in local directory
        File junit = new File(testFilePath, junitClassName+".java");
        try(FileWriter writer = new FileWriter(junit)){
            writer.write(junitContent);
        }

    }

    private void generateSlingModel(SlingHttpServletRequest req, String componentName, String propertiesString,
                                    String gettersAndSetters, String resourceType, JsonObject jsonObject, JsonArray itemNames) throws IOException {
        String modelClassName = capitalize(componentName) + "Model";
        //String modelPackage = "com.aem.componentGenerator.models";
        JsonObject componentObject = jsonObject.getAsJsonObject("component");
        String basePath = componentObject.get("corePath").getAsString().split("java")[1].replaceFirst("\\\\", "");
        log.error("BasePath : " + basePath);
        String packagePath = basePath.replace("\\", ".");
        log.error("packagePath : "+packagePath);
        String packagePaths = basePath.replace("\\", "");
        log.error("packagePath : "+packagePaths);
        String modelFilePath = componentObject.get("corePath").getAsString()+ "\\\\" + "models" ;
        String testFilePath = componentObject.get("testPath").getAsString()+ "\\" + "models";

        // Read the template from CRXDE
        String template = readTemplateFromCRXDE(req, templateFilePath);

        // Replace placeholders in the template
        String modelContent = template
                .replace("{{packageName}}", packagePath + ".models")
                .replace("{{resourceType}}", resourceType)
                .replace("{{modelClassName}}", modelClassName)
                .replace("{{properties}}", propertiesString)
                .replace("{{gettersAndSetters}}", gettersAndSetters);


        // Create the model directory if it doesn't exist
        //JsonObject componentObject = jsonObject.getAsJsonObject("component");
        String LOCAL_DIRECTORY = componentObject.get("corePath").getAsString();
        File componentDir = new File(LOCAL_DIRECTORY, "\\" +"models");
        File model = new File(modelFilePath, modelClassName + ".java");
        componentDir.mkdirs();

        // Write the Sling Model class to a file
        try (FileWriter writer = new FileWriter(model)) {
            writer.write(modelContent);
        }

        generteJunitFile(req, modelClassName, itemNames, packagePath, testFilePath);
        log.error("generateSlingModel method executed successfully");
    }

    public JsonArray extractItemNames(JsonArray tabs, String jsonValue) {
        JsonArray itemNames = new JsonArray();

        for (int i = 0; i < tabs.size(); i++) {
            JsonObject tab = tabs.get(i).getAsJsonObject();
            JsonArray items = tab.getAsJsonArray("items");
            for (int j = 0; j < items.size(); j++) {
                JsonObject item = items.get(j).getAsJsonObject();
                String itemName = item.get(jsonValue).getAsString();
                itemNames.add(itemName);
            }
        }
        log.error("extractItemNames method executed successfully");
        return itemNames;
    }

    private String generateHtlProperties(JsonArray itemNames) {
        StringBuilder propertiesBuilder = new StringBuilder();
        for (int i = 0; i < itemNames.size(); i++) {
            String key = itemNames.get(i).getAsString();
            propertiesBuilder.append("    @ValueMapValue\n");
            propertiesBuilder.append("    private String ").append(key).append(";\n\n");
        }
        return propertiesBuilder.toString();
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String toCamelCase(String str) {
        if (str == null || str.trim().isEmpty())
            return "";

        String[] words = str.trim().split("\\s+");
        return words[0].toLowerCase() +
                Arrays.stream(words, 1, words.length)
                        .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                        .collect(Collectors.joining());
    }

}