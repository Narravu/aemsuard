package com.basfeupf.core.servlets;

import com.basfeupf.core.constants.Basf_Constant;
import com.basfeupf.core.services.AppAccessTokenService;
import com.basfeupf.core.services.AuthConfigService;
import com.basfeupf.core.services.EupfServiceNew;
import com.basfeupf.core.services.TalendServise;
import com.basfeupf.core.services.UserDetailsService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.Collectors;

@Component(service = Servlet.class)
@SlingServletResourceTypes(resourceTypes = "/apps/basfeupf/talendsavelms", extensions = "json", methods = HttpConstants.METHOD_POST )
public class CXMAPIServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;

	@Reference
	AppAccessTokenService appAccessTokenService;

	@Reference
	UserDetailsService userDetailsService;
	
	@Reference
	AuthConfigService authConfigService;
	
	@Reference
	TalendServise talendServise;
	
	@Reference
	EupfServiceNew eupfServiceNew;

	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {

		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		JsonObject respnseJson= new JsonObject(); 

		String body = request.getReader().lines().collect(Collectors.joining());
		Gson gson = new Gson();
		JsonObject requestBodyJsonObj = gson.fromJson(body, JsonObject.class);

		
		JsonObject headerJson = new JsonObject();
		String xApiKey=authConfigService.getEupf_apigee_talend_key();
		String url=authConfigService.getTalendEndpointUrl()+Basf_Constant.TALEND_LMX_ENDPOINT;
		headerJson.addProperty("x-api-key", xApiKey);
		
		JsonObject talendResponseJson =null;
		String adobeuuidreq="";
		try {
			//String jwt_token=request.getCookie("id_token").getValue();
			//String jwt_token = requestBodyJsonObj.get("id_token").getAsString();
			String talendApiClientId = authConfigService.getEupf_apigee_talend_client_id();
			/*************get adobe uuid if already exist**************/
			JsonObject userRequestJson = new JsonObject();
			userRequestJson.addProperty("sub", requestBodyJsonObj.get("ext_Portal_UUID").getAsString());
			userRequestJson.addProperty("type", "get_user_id_sub");
			JsonObject userIdJson = eupfServiceNew.callAwsRestService(userRequestJson);
			if (userIdJson.has(Basf_Constant.STATUS) && userIdJson.get(Basf_Constant.STATUS).getAsString().equals(Basf_Constant.STATUS_SUCCESS)) {
				if (userIdJson.has(Basf_Constant.DATA)) {
					JsonArray userIdArray = userIdJson.get(Basf_Constant.DATA).getAsJsonArray();
					for (JsonElement userElement : userIdArray) {
						JsonObject userJson = userElement.getAsJsonObject();
						
						if (userJson.has("adobe_uuid")) {
							adobeuuidreq=userJson.get("adobe_uuid").getAsString();
						}
						
						
					}
				}
			}
			
			if(!adobeuuidreq.equalsIgnoreCase("")) {
				requestBodyJsonObj.get("application_attributes").getAsJsonObject().get("LMX").getAsJsonObject().addProperty("adobe_uuid", adobeuuidreq);
			}
			/**********************************************************/
			
			
			 talendResponseJson= talendServise.talendAPIGeneric(talendApiClientId,  xApiKey, url,requestBodyJsonObj );
			 
			 if(talendResponseJson.has("data")) {
				 JsonObject data =talendResponseJson.get("data").getAsJsonObject();
				 if(data.has("id")) {
					 String adobeuuid=data.get("id").getAsString();
					 String sub=requestBodyJsonObj.get("ext_Portal_UUID").getAsString();//take from request
					 /***********************update adobe uuid in azure****************************/
					 JsonObject cxmRequestJson = new JsonObject();
					 
					 cxmRequestJson.addProperty("sub", sub);
					 cxmRequestJson.addProperty("adobeuuid", adobeuuid);
					 cxmRequestJson.addProperty("type", "update_adobe_uuid");
					 eupfServiceNew.callAwsRestService(cxmRequestJson);
					 /*****************************************************************************/
				 }
			 }
			 

			 
			 //update adobe uuid in database here
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//headerJson.addProperty("x-id-token", jwt_token);
		
	//	JsonObject talendresponseJson = talendServise.callPost(requestBodyJsonObj, url, headerJson);
		
		respnseJson.add("talendresponse", talendResponseJson);
		
		out.println(respnseJson);
	}
}
