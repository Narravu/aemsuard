package com.basfeupf.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.Collectors;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import com.google.gson.JsonArray;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.basfeupf.core.constants.Basf_Constant;
import com.basfeupf.core.services.AppAccessTokenService;
import com.basfeupf.core.services.AuthConfigService;
import com.basfeupf.core.services.EupfServiceNew;
import com.basfeupf.core.services.TalendServise;
import com.basfeupf.core.services.UserDetailsService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

@Component(service = Servlet.class)
@SlingServletResourceTypes(resourceTypes = "/apps/basfeupf/userupdate", extensions = "json", methods = HttpConstants.METHOD_POST )
public class UserUpdateAPIServlet extends SlingAllMethodsServlet {

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

		Date currentDateInEpoch = new Date();
		JsonObject userObject = requestBodyJsonObj.get("userObject").getAsJsonObject();
		JsonObject profileObject = requestBodyJsonObj.get("profile").getAsJsonObject();
		requestBodyJsonObj.remove("userObject");
		requestBodyJsonObj.remove("profile");
		JsonObject responseJson = appAccessTokenService.getAppAccessToken(requestBodyJsonObj);

		long timeInMillis = Calendar.getInstance().getTimeInMillis();
		long expiresIn_epoch = new Date(timeInMillis + responseJson.get("expires_in").getAsLong()).getTime();
		String mdmBusinessSegment[] = authConfigService.getBusinessSegment();
		responseJson.addProperty("initTimeInEpoch", currentDateInEpoch.getTime());
		responseJson.addProperty("expiresInEpoch", expiresIn_epoch);
		JsonObject talendUserObject = requestBodyJsonObj.get("talendUserObject").getAsJsonObject();
		String extension_bs = talendUserObject.get("extension_bs").getAsString();
		talendUserObject.remove("extension_bs");
		JsonArray array = talendUserObject.getAsJsonArray("Attributes").getAsJsonArray();/*

		/***********************update DB***********************/

		profileObject.addProperty("type", "update_user_details");
		try {
			JsonObject userIdJson = eupfServiceNew.callAwsRestService(profileObject);
		} catch(Exception e) {
			//logger.debug("exception"+e);
		}

		/******************************************************/

		String userId = requestBodyJsonObj.get("sub").getAsString();
		requestBodyJsonObj.remove("sub");
		if(userObject.has("country")) {
			if(userObject.get("country").getAsString().equals("")) {
				userObject.remove("country");
			}
			}
			if(userObject.has("preferredLanguage")) {
			if(userObject.get("preferredLanguage").getAsString().equals("")) {
				   userObject.remove("preferredLanguage");
			}
			}
			if(userObject.has("mobilePhone")) {
			if(userObject.get("mobilePhone").getAsString().equals("")) {
				userObject.remove("mobilePhone");
				//userObject.
			}
			}
			if(userObject.has("surname")) {
			if(userObject.get("surname").getAsString().equals("")) {
				userObject.remove("surname");
			}
			}
			if(userObject.has("givenName")) {
			if(userObject.get("givenName").getAsString().equals("")) {
				userObject.remove("givenName");
			}
			}
			if(userObject.has("telephoneNumber")) {
			if(userObject.get("telephoneNumber").getAsString().equals("")) {
				userObject.remove("telephoneNumber");
			}
			}
		JsonObject jsonObject = userDetailsService.updateUser(userId, userObject, responseJson.get("access_token").getAsString());

		JsonObject headerJson = new JsonObject();
		String xApiKey = authConfigService.getEupf_apigee_talend_key();// config
		String talendApiClientIds = authConfigService.getEupf_apigee_talend_client_id();
		headerJson.addProperty("client_secret", xApiKey);
		headerJson.addProperty("client_id", talendApiClientIds);
		JsonObject talendresponseJson=null;
		try {

			JsonObject business_seg = new JsonObject();
			business_seg.addProperty("attr_key", "Business_Segment");
			business_seg.addProperty("attr_value", talendServise.getMdmValue(mdmBusinessSegment, extension_bs));
			array.add(business_seg);

			String url = authConfigService.getTalendEndpointUrl()+Basf_Constant.TALEND_PROFILE_ENDPOINT;
			talendresponseJson = talendServise.callPost(talendUserObject, url, headerJson);

		} catch(Exception e) {

		}


		respnseJson.add("azureresponse", jsonObject);
		respnseJson.add("talendresponse", talendresponseJson);

		out.println(respnseJson);
	}
}
