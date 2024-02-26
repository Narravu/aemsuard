package com.basfeupf.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.stream.Collectors;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.basfeupf.core.constants.Basf_Constant;
import com.basfeupf.core.services.AuthConfigService;
import com.basfeupf.core.services.TalendServise;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

@Component(service = Servlet.class)
@SlingServletResourceTypes(resourceTypes = "/apps/basfeupf/talendupdate", extensions = "json", methods = HttpConstants.METHOD_POST )
public class TalendAPIDataSubmit extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;
	
	Logger logger = LoggerFactory.getLogger(this.getClass());

		
	@Reference
	AuthConfigService authConfigService;
	
	@Reference
	TalendServise talendServise;

	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {

		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		JsonObject respnseJson= new JsonObject(); 

		String body = request.getReader().lines().collect(Collectors.joining());
		Gson gson = new Gson();
		JsonObject requestBodyJsonObj = gson.fromJson(body, JsonObject.class);

		JsonObject talendUserObject = requestBodyJsonObj;
		JsonObject headerJson = new JsonObject();
		String xApiKey=authConfigService.getEupf_apigee_talend_key();
		String talendApiClientIdkey = authConfigService.getEupf_apigee_talend_client_id();
		String url=authConfigService.getTalendEndpointUrl();
		if(requestBodyJsonObj.has("Request_Attribute")) {
			if(requestBodyJsonObj.get("Request_Attribute").getAsString().equalsIgnoreCase("Segment")) {
				url=url+Basf_Constant.TALEND_SEGMENT_ENDPOINT;
			}
			
			if(requestBodyJsonObj.get("Request_Attribute").getAsString().equalsIgnoreCase("APP")) {
				url=url+Basf_Constant.TALEND_APP_ENDPOINT;
			}
		}
		logger.debug("url"+url);
		JsonObject talendResponseJson =null;
		
		try {
			//String jwt_token=request.getCookie("id_token").getValue();
//			String jwt_token = requestBodyJsonObj.get("id_token").getAsString();
//			headerJson.addProperty("x-api-key", xApiKey);
			
//			headerJson.addProperty("x-id-token", jwt_token);
			
			 talendResponseJson= talendServise.talendAPIGeneric(talendApiClientIdkey,  xApiKey, url,requestBodyJsonObj );
		} catch (Exception e) {
			logger.debug("talend class error"+e);
			e.printStackTrace();
		}

	
	//	JsonObject talendresponseJson = talendServise.callPost(talendUserObject, url, headerJson);
		
		respnseJson.add("talendresponse", talendResponseJson);
		
		out.println(respnseJson);
	}
}
