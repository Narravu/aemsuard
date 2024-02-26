package com.basfeupf.core.servlets;

import com.basfeupf.core.constants.Basf_Constant;
import com.basfeupf.core.services.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Objects;

//@Component(service = Servlet.class)
@Component(service = Servlet.class, property = { Constants.SERVICE_DESCRIPTION + "= BASF_Eup User Group",
        "sling.servlet.methods=" + HttpConstants.METHOD_GET,
        "sling.servlet.methods=" + HttpConstants.METHOD_POST,
        "sling.servlet.resourceTypes=" + "/bin/eupf/usergroup",
        "sling.servlet.extension=json"
})
//@SlingServletResourceTypes(
  //      resourceTypes = "/bin/eupf/usergroup",
    //    extensions = "json",
      //  methods = {HttpConstants.METHOD_GET, HttpConstants.METHOD_POST})
public class UserGroupServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;


    @Reference
    private HttpClientBuilderFactory httpClientBuilderFactory;

    @Reference
    HttpCallerService httpCallerService;

    @Reference
    AzureAuthService azureAuthService;

    @Reference
    AuthConfigService authConfigService;

    @Reference
    EupfService eupfService;

    @Reference
    TalendServise talendServise;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        String responseJson = handleGet(request, response);
        //JsonObject group = new JsonObject();//Json Object
        //group.add("group", (JsonArray)responseJson);
        logger.info("user_group_response2" + responseJson.toString());
        out.println(responseJson);
        //out.println("{\"groups\":\"040db27b-50a1-499c-aed5-fe16dac1bc2d:,ded02d8a-5aca-48ba-95c4-779011493e2f:,a7ded86f-47f1-442f-a018-e9fdc8082f68:,b4b195ef-ebb3-4fee-9f25-2749c50a3fd6:,8110dcbf-8555-4ffc-8be6-c55d7c397b38:,0057acbb-65e9-467f-906b-799e8f1e73a5:,\"}");
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        PrintWriter out = response.getWriter();
        response.setContentType("text/plain");
        JsonObject responseJson = handlePost(request, response);
        out.println(responseJson.toString());
    }

    private String handleGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
    	JsonArray jsonarray = new JsonArray();
        String user_group = "";
        try {
        	String bearer_token = "";
            String user_object_id = "";
            Map params = request.getRequestParameterMap();
            boolean noparams = (params.size()>0);
            if(noparams){
                if( (!request.getParameter("bearerToken").equals(null)) && (!request.getParameter("userObjectId").equals(null)) ) {
                    bearer_token = request.getParameter("bearerToken");
                    user_object_id = request.getParameter("userObjectId");
                }
            }else {
        		bearer_token = request.getHeader("bearerToken");
                user_object_id = request.getHeader("userObjectId");
			}
            
            //responseJson = talendServise.talendAPI(jwt_token, state);
            JsonObject apiResponse = fetchResponseFromAPI(bearer_token, user_object_id);
            logger.info("user_group_response" + apiResponse.toString());
            PrintWriter out = response.getWriter();
            response.setContentType("text/plain");
            apiResponse.remove("@odata.context");
            JsonArray responsearray = apiResponse.get("value").getAsJsonArray();

            for(int i=0; i<responsearray.size(); i++){
                user_group+= responsearray.get(i).getAsJsonObject().get("id").getAsString()+":"+(Objects.nonNull(responsearray.get(i).getAsJsonObject().get("displayName")) ? responsearray.get(i).getAsJsonObject().get("displayName").getAsString() : "")+",";
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("id", responsearray.get(i).getAsJsonObject().get("id").getAsString());
                jsonObject.addProperty("displayName",Objects.nonNull(responsearray.get(i).getAsJsonObject().get("displayName")) ? "" : responsearray.get(i).getAsJsonObject().get("displayName").getAsString());
                jsonarray.add(jsonObject);
            }
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("groups", user_group);
            user_group = jsonObject.toString();
            logger.info("user_group_response1" + user_group.toString());
           // out.println(jsonarray); 

        } catch (Exception e) {
           // responseJson.addProperty(Basf_Constant.STATUS, Basf_Constant.STATUS_FAIL);
           // responseJson.addProperty(Basf_Constant.ERROR_MSG, e.getClass().getSimpleName() + " : " + e.getMessage());
            StackTraceElement[] sTElements = e.getStackTrace();
            for (StackTraceElement stackTraceEle : sTElements) {
                String corePackageName = this.getClass().getPackage().getName().split("core")[0] + "core";
                if (stackTraceEle.getClassName().contains(corePackageName)) {
                    StringBuffer stringBuffer = new StringBuffer();
                    stringBuffer.append("\n{").append("\n\t\"ClassName\" : \"" + stackTraceEle.getClassName() + "\"")
                            .append("\n\t\"MethodName\" : \"" + stackTraceEle.getMethodName() + "\",")
                            .append("\n\t\"LineNumber\" : \"" + stackTraceEle.getLineNumber() + "\",")
                            .append("\n\t\"" + e.getClass().getSimpleName() + "\" : \"" + e.getMessage() + "\"")
                            .append("\n}\n");
                    logger.info(stringBuffer.toString());
                    JsonObject errorJson = new JsonObject();
                    errorJson.addProperty("ClassName", stackTraceEle.getClassName());
                    errorJson.addProperty("MethodName", stackTraceEle.getMethodName());
                    errorJson.addProperty("LineNumber", stackTraceEle.getLineNumber());
                    errorJson.addProperty(e.getClass().getSimpleName(), e.getMessage());
                   // responseJson.add(Basf_Constant.ERROR_JSON, errorJson);
                    break;
                }
            }
        }
        return user_group;
    }

    private JsonObject fetchResponseFromAPI(String bearer_token, String user_object_id) {

        String responseJsonString = "";
        try {
            String url = authConfigService.getUser_group_url1() + user_object_id + authConfigService.getUser_group_url2();
            HttpClientBuilder builder = httpClientBuilderFactory.newBuilder();
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(5000)
                    .setSocketTimeout(5000).build();
            builder.setDefaultRequestConfig(requestConfig);
            HttpClient client = builder.build();
            // HttpClient client = httpClientBuilder.build();
            HttpGet getRequest = new HttpGet(url);
            getRequest.setHeader(HttpHeaders.AUTHORIZATION, bearer_token);
            HttpResponse httpResponse = client.execute(getRequest);

            int statusCode = httpResponse.getStatusLine().getStatusCode();

            BufferedReader br = new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));
            String output;

            while ((output = br.readLine()) != null) {
                responseJsonString = responseJsonString + output;
            }


         } catch (Exception e) {
            e.getMessage();
        }
        return new JsonParser().parse(responseJsonString).getAsJsonObject();

    }

    private JsonObject handlePost(SlingHttpServletRequest request, SlingHttpServletResponse response) {

        JsonObject responseJson = new JsonObject();
        try {
            String jwt_token = request.getParameter("jwt_token");
            String user_object_id = request.getParameter("user_object_id");
            //responseJson = talendServise.talendAPI(jwt_token, state);

        } catch (Exception e) {
            responseJson.addProperty(Basf_Constant.STATUS, Basf_Constant.STATUS_FAIL);
            responseJson.addProperty(Basf_Constant.ERROR_MSG, e.getClass().getSimpleName() + " : " + e.getMessage());
            StackTraceElement[] sTElements = e.getStackTrace();
            for (StackTraceElement stackTraceEle : sTElements) {
                String corePackageName = this.getClass().getPackage().getName().split("core")[0] + "core";
                if (stackTraceEle.getClassName().contains(corePackageName)) {
                    StringBuffer stringBuffer = new StringBuffer();
                    stringBuffer.append("\n{").append("\n\t\"ClassName\" : \"" + stackTraceEle.getClassName() + "\"")
                            .append("\n\t\"MethodName\" : \"" + stackTraceEle.getMethodName() + "\",")
                            .append("\n\t\"LineNumber\" : \"" + stackTraceEle.getLineNumber() + "\",")
                            .append("\n\t\"" + e.getClass().getSimpleName() + "\" : \"" + e.getMessage() + "\"")
                            .append("\n}\n");
                    logger.info(stringBuffer.toString());
                    JsonObject errorJson = new JsonObject();
                    errorJson.addProperty("ClassName", stackTraceEle.getClassName());
                    errorJson.addProperty("MethodName", stackTraceEle.getMethodName());
                    errorJson.addProperty("LineNumber", stackTraceEle.getLineNumber());
                    errorJson.addProperty(e.getClass().getSimpleName(), e.getMessage());
                    responseJson.add(Basf_Constant.ERROR_JSON, errorJson);
                    break;
                }
            }
        }
        return responseJson;
    }

}
