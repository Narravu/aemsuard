package com.basfeupf.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import com.basfeupf.core.services.*;

import org.apache.commons.lang3.StringUtils;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@Component(service = Servlet.class)
@SlingServletResourceTypes(resourceTypes = "/bin/eupf/formdata", extensions = "json", methods = {
		HttpConstants.METHOD_POST })
public class GetFormDataServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;

	@Reference
	EupfService eupfService;
	
	@Reference
	EupfServiceNew eupfServiceNew;

	@Reference
	HttpCallerService httpCallerService;

	@Reference
	AzureAuthService azureAuthService;

	@Reference
	AuthConfigService authConfigService;
	
	@Reference
	TalendServise talendServise;
	
	@Reference
	AzureAuthServiceNew azureAuthServiceNew;

	@Reference
	UserDetailsService userDetailsService;

	@Reference
	AppAccessTokenService appAccessTokenService;

	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {

		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		JsonObject responseJson = handlePost(request, response);
		out.println(responseJson);
	}

	private JsonObject handlePost(SlingHttpServletRequest request, SlingHttpServletResponse response) {

		JsonObject responseJson = new JsonObject();
		try {
			JsonObject requestJson = httpCallerService.createRequest(request, response);
			String type = requestJson.get("type").getAsString();

			switch (type) {
			case "get_form_data":

				//Cookie cookie = request.getCookie("id_token"); // to get EUPF_ID (sub)
				String form_data_idToken = requestJson.get("id_token").getAsString();
				String talendApiClientId = authConfigService.getEupf_apigee_talend_client_id();
				if (!form_data_idToken.equals(null) && !form_data_idToken.equals("")) {

					responseJson = eupfService.callAwsRestService(requestJson); // get_form_data
					if (responseJson.has(Basf_Constant.STATUS) && responseJson.get(Basf_Constant.STATUS).getAsString()
							.equals(Basf_Constant.STATUS_SUCCESS)) {

						// to get ContactId & Account_BASF_ID
						//String id_token = cookie.getValue();
						JsonObject payloadJson = azureAuthService.getPayloadJson(form_data_idToken);
						String sub = payloadJson.has("sub") ? payloadJson.get("sub").getAsString() : "";
						requestJson.addProperty("sub", sub);
						requestJson.addProperty("type", "get_user_ids");
						JsonObject userIdJson = eupfService.callAwsRestService(requestJson);

						if (userIdJson.has(Basf_Constant.STATUS) && userIdJson.get(Basf_Constant.STATUS).getAsString()
								.equals(Basf_Constant.STATUS_SUCCESS)) {

							if (userIdJson.has(Basf_Constant.DATA)) {
								// for talendRequestJson
								JsonArray app_data_array = responseJson.get(Basf_Constant.DATA).getAsJsonArray();
								JsonArray userIdArray = userIdJson.get(Basf_Constant.DATA).getAsJsonArray();
								
//								JsonObject talendRequestJson = eupfService.getTalendRequestJson(requestJson,
//										app_data_array, userIdArray);
								
								JsonObject talendrequestJson = new JsonObject();
								talendrequestJson.addProperty("Request_Type", requestJson.get("Request_Type").getAsString());
								talendrequestJson.addProperty("Request_Attribute", requestJson.get("Request_Attribute").getAsString());
								talendrequestJson.addProperty("EUPF_ID",requestJson.get("EUPF_ID").getAsString());
								talendrequestJson.addProperty("Contact_Id", requestJson.get("Contact_Id").getAsString());
								talendrequestJson.addProperty("account_id", requestJson.get("Account_BASF_ID").getAsString());
								talendrequestJson.addProperty("Account_Type", requestJson.get("account_type").getAsString());
								talendrequestJson.addProperty("Business_Segment_Id", requestJson.get("segment_id").getAsString());
								
								JsonArray talendRequestAttrArray = new JsonArray();
								for (JsonElement jsonElement : app_data_array) {
									JsonObject jsonObject = jsonElement.getAsJsonObject();
									String attrib_map_id = jsonObject.has("attrib_map_id") ? jsonObject.get("attrib_map_id").getAsString() : "";
									String crm_attrib_type = jsonObject.has("crm_attrib_type") ? jsonObject.get("crm_attrib_type").getAsString() : "";
									
									JsonObject attrJson = new JsonObject();
									attrJson.addProperty("attr_key", attrib_map_id);
									attrJson.addProperty("attr_value", "");
									attrJson.addProperty("attr_type", crm_attrib_type);
									talendRequestAttrArray.add(attrJson);
								}
								
								talendrequestJson.add("Attributes", talendRequestAttrArray);
								
								 String talendxApiKey=authConfigService.getEupf_apigee_talend_key();
								 String url=authConfigService.getTalendEndpointUrl()+Basf_Constant.TALEND_SEGMENT_ENDPOINT;
								
								
								JsonObject talendResponseJson= talendServise.talendAPIGeneric(talendApiClientId,  talendxApiKey, url,talendrequestJson );

							//	String url = "https://run.mocky.io/v3/328cbc21-01a7-422a-8909-e47d29839499";
							//	JsonObject talendResponseJson = httpCallerService.callPost(talendRequestJson, url);
							//	responseJson.add("talend", talendResponseJson);

								if (Objects.nonNull(talendResponseJson)) {
									// for add attr_value in response
									talendServise.add_attr_value_in_response(talendResponseJson, app_data_array,
											responseJson);
								}
							}
						}
					}
				} else {
					responseJson.addProperty(Basf_Constant.STATUS, Basf_Constant.STATUS_FAIL);
					responseJson.addProperty(Basf_Constant.ERROR_MSG, "id_token not found");
				}
				break;

			case "get_app_id":
				responseJson = eupfService.callAwsRestService(requestJson);
				break;
				
			case "get_app_data_acknowledged_status":
				responseJson = eupfService.callAwsRestService(requestJson);
				break;


			case "get_app_data":

//				Cookie cookie2 = request.getCookie("id_token"); // to get EUPF_ID (sub) -
				String id_token = requestJson.get("id_token").getAsString();
				if (!id_token.equals(null) && !id_token.equals("")) {
					if (!requestJson.has("Contact_Id") && !requestJson.has("account_id")){
						requestJson.addProperty("type", "get_app_data_app_id");
					}

					responseJson = eupfService.callAwsRestService(requestJson); // get_app_data
					if (responseJson.has(Basf_Constant.STATUS) && responseJson.get(Basf_Constant.STATUS).getAsString()
							.equals(Basf_Constant.STATUS_SUCCESS)) {

						// to get ContactId & Account_BASF_ID
//						String id_token = cookie2.getValue();
						JsonObject payloadJson = azureAuthService.getPayloadJson(id_token);
						String sub = payloadJson.has("sub") ? payloadJson.get("sub").getAsString() : "";
						requestJson.addProperty("sub", sub);
						requestJson.addProperty("type", "get_user_id_sub");
						JsonObject userIdJson = eupfService.callAwsRestService(requestJson);

						if (userIdJson.has(Basf_Constant.STATUS) && userIdJson.get(Basf_Constant.STATUS).getAsString()
								.equals(Basf_Constant.STATUS_SUCCESS)) {

							if (userIdJson.has(Basf_Constant.DATA)) {
								// for talendRequestJson
								JsonArray app_data_array = responseJson.get(Basf_Constant.DATA).getAsJsonArray();
								JsonArray userIdArray = userIdJson.get(Basf_Constant.DATA).getAsJsonArray();
								
//								JsonObject talendRequestJson = eupfService.getTalendRequestJson(requestJson,
//										app_data_array, userIdArray);
								
								JsonObject talendrequestJson = new JsonObject();
								talendrequestJson.addProperty("EUPF_ID",requestJson.get("EUPF_ID").getAsString());
								talendrequestJson.addProperty("Request_Attribute", requestJson.get("Request_Attribute").getAsString());
								talendrequestJson.addProperty("Request_Type", requestJson.get("Request_Type").getAsString());
								talendrequestJson.addProperty("Account_Type", requestJson.get("Account_Type").getAsString());
								talendrequestJson.addProperty("Business_Segment_Id", requestJson.get("Business_Segment_Id").getAsString());
								talendrequestJson.addProperty("Email_Address", requestJson.get("Email_Address").getAsString());
								if (requestJson.has("Lead_ID") && StringUtils.isNotBlank(requestJson.get("Lead_ID").getAsString())){
									talendrequestJson.addProperty("Lead_ID", requestJson.get("Lead_ID").getAsString());									
								}
								if (requestJson.has("Contact_Id") && requestJson.has("account_id")){
									talendrequestJson.addProperty("Contact_Id", requestJson.get("Contact_Id").getAsString());
									talendrequestJson.addProperty("account_id", requestJson.get("account_id").getAsString());
								}

								JsonArray talendRequestAttrArray = new JsonArray();
								for (JsonElement jsonElement : app_data_array) {
									JsonObject jsonObject = jsonElement.getAsJsonObject();
									String attrib_map_id = jsonObject.has("attrib_map_id") ? jsonObject.get("attrib_map_id").getAsString() : "";
									String attr_type = jsonObject.has("crm_attrib_type") ? jsonObject.get("crm_attrib_type").getAsString() : "";

									JsonObject attrJson = new JsonObject();
									attrJson.addProperty("attr_key", attrib_map_id);
									attrJson.addProperty("attr_value", "");
									attrJson.addProperty("attr_type", attr_type);
									talendRequestAttrArray.add(attrJson);
								}
								
								talendrequestJson.add("Attributes", talendRequestAttrArray);

								String talendxApiKey=authConfigService.getEupf_apigee_talend_key();
								String url=authConfigService.getTalendEndpointUrl()+Basf_Constant.TALEND_APP_ENDPOINT;
								String talendApiClientIdkey = authConfigService.getEupf_apigee_talend_client_id();
								
								JsonObject talendResponseJson= talendServise.talendAPIGeneric(talendApiClientIdkey,talendxApiKey, url,talendrequestJson );

								responseJson.add("talend", talendResponseJson);

								if (Objects.nonNull(talendResponseJson)) {
									// for add attr_value in response
									talendServise.add_attr_value_in_response(talendResponseJson, app_data_array,
											responseJson);
								}
							}
						}
					}
				} else {
					responseJson.addProperty(Basf_Constant.STATUS, Basf_Constant.STATUS_FAIL);
					responseJson.addProperty(Basf_Constant.ERROR_MSG, "id_token not found");
				}
				break;

			case "get_app_segment_data":
				responseJson = eupfService.callAwsRestService(requestJson);
				break;

			case "get_status_pending_users":
				requestJson.addProperty("limit", 100);
				responseJson = eupfService.callAwsRestService(requestJson);
				break;
				
			case "talend":
				String talendInsertUrl = "https://run.mocky.io/v3/b21ce140-1617-4ffb-af46-d4be01a9fc07";
				responseJson = httpCallerService.callPost(requestJson, talendInsertUrl);
				break;
				
			case "talend-user-data":
				
				//Cookie cookie3 = request.getCookie("id_token"); // to get EUPF_ID (sub)
					requestJson.remove("type");
					String talendApiClientIds = authConfigService.getEupf_apigee_talend_client_id();
					String talendxApiKey=authConfigService.getEupf_apigee_talend_key();
					String url1=authConfigService.getTalendEndpointUrl()+Basf_Constant.TALEND_PROFILE_ENDPOINT;
					
					responseJson= talendServise.talendAPIGeneric(talendApiClientIds,  talendxApiKey, url1,requestJson );
				 
				break;
				
			case "update_account_user_type":
				responseJson = eupfService.callAwsRestService(requestJson);
				break;
				
			case "update_reset_password_cookie":
				String reqid_token = requestJson.has("id_token")? requestJson.get("id_token").getAsString():""; 
				String state = requestJson.has("state")? requestJson.get("state").getAsString():""; 
				String code = requestJson.has("code")? requestJson.get("code").getAsString():""; 
				String scope = requestJson.has("scope")? requestJson.get("scope").getAsString():""; 
				
				JsonObject tokenJson=new JsonObject();
				if(Objects.nonNull(reqid_token) && !reqid_token.equalsIgnoreCase("")) {
					tokenJson.addProperty("id_token", reqid_token);
				} else {
					request.setAttribute("code", code);
					request.setAttribute("state", state);
					request.setAttribute("scope", scope);
					tokenJson = eupfServiceNew.getAzureToken(request, response);

				}
				String nonce = authConfigService.getNonce();
				tokenJson.addProperty("nonce", nonce);
				if (Objects.nonNull(state)) {
					tokenJson.addProperty("state", state);
				}
				JsonObject isValidToken = azureAuthServiceNew.isValidToken(tokenJson);
				boolean isValid = false;
				if (isValidToken.get("isvalid").getAsBoolean()) {
					isValid = true;
					setCookieInResponse(tokenJson, response);
					
				} else {
					isValid = false;
				}
				break;

			case "update_user_linked_user":
				String user_id = requestJson.get("sub").getAsString();

				//Update Account id and Contact id to eupf
				JsonObject userDataUpdate = eupfServiceNew.callAwsRestService(requestJson);
				//Update Account id and Contact id to B2C
				String accessToken = appAccessTokenService.getAppAccessToken(requestJson).get("access_token").getAsString();
				JsonObject userUpdate = userDetailsService.updateUser(user_id, requestJson.get("userObj").getAsJsonObject(), accessToken);

				break;
			case "account_mapping":
				String accTalendApiClientId = authConfigService.getEupf_apigee_talend_client_id();
				String accTalendxApiKey = authConfigService.getEupf_apigee_talend_key();
				String accUrl = authConfigService.getTalendEndpointUrl()+Basf_Constant.TALEND_LOGIN_REG_ENDPOINT;
				requestJson.remove("type");
				responseJson = talendServise.talendAPIGeneric(accTalendApiClientId, accTalendxApiKey, accUrl,requestJson);

				break;
				
			default:
				break;
			}

		} catch (Exception e) {
			logger.debug("error"+e);
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
					logger.error(stringBuffer.toString());
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

	private void setCookieInResponse(JsonObject tokenJson, SlingHttpServletResponse response) {

		for (String key : tokenJson.keySet()) {
			if (!key.equals("scope")) {
				String value = tokenJson.get(key).toString();
				Cookie cookie = new Cookie(key, value);
				cookie.setPath("/");
				cookie.setMaxAge(3600);
				// cookie.setHttpOnly(true);
				// cookie.setSecure(true);
				if(!authConfigService.getCookie_domain_name().equalsIgnoreCase("")) {
					cookie.setDomain(authConfigService.getCookie_domain_name());
				}
				 
				response.addCookie(cookie);
			}
		}
	}
}
