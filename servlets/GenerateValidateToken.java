package com.basfeupf.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.basfeupf.core.constants.Basf_Constant;
import com.basfeupf.core.services.AuthConfigService;
import com.basfeupf.core.services.AzureAuthService;
import com.basfeupf.core.services.EupfService;
import com.basfeupf.core.services.HttpCallerService;
import com.basfeupf.core.services.TalendServise;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@Component(service = Servlet.class)
@SlingServletResourceTypes(resourceTypes = "/bin/eupf/token/generatevalidate1", extensions = "json", methods = {
		HttpConstants.METHOD_GET })
public class GenerateValidateToken extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;

	@Reference
	HttpCallerService httpCallerService;

	@Reference
	AzureAuthService azureAuthService;

	@Reference
	AuthConfigService authConfigService;

	@Reference
	EupfService eupfService;

	@Reference
	JobManager jobManager;

	@Reference
	TalendServise talendServise;

	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {

		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		JsonObject responseJson = handlePost(request, response);
		out.println(responseJson);
	}

	private JsonObject handlePost(SlingHttpServletRequest request, SlingHttpServletResponse response) {

		JsonObject responseJson = new JsonObject();
		boolean isValid = false;
		try {
			String nonce = "123456";
			JsonObject tokenJson = eupfService.getAzureToken(request, response);

			if (tokenJson.has("error_description")
					&& tokenJson.get("error_description").getAsString().contains("AADB2C90080")) {

				responseJson.addProperty(Basf_Constant.STATUS, Basf_Constant.STATUS_FAIL);
				responseJson.addProperty(Basf_Constant.ERROR_MSG, "The provided grant has expired");
				return responseJson;
			}

			tokenJson.addProperty("nonce", nonce);
			String state = request.getParameter("state");
			if (Objects.nonNull(state)) {
				tokenJson.addProperty("state", state);
			}

			boolean isValidToken = azureAuthService.isValidToken(tokenJson);
			String id_token = tokenJson.get("id_token").getAsString();

			if (!isValidToken) {
				isValid = true;
				responseJson.addProperty("request_origin", "same");
			} else {
//				String id_token = tokenJson.get("id_token").getAsString();
				responseJson = azureAuthService.validateThirdPartyClaims(id_token, nonce, state);
				if (responseJson.has(Basf_Constant.STATUS)
						&& responseJson.get(Basf_Constant.STATUS).getAsString().equals(Basf_Constant.STATUS_SUCCESS)) {
					isValid = true;
				}
				responseJson.addProperty("request_origin", "different");
			}

			String request_origin = responseJson.get("request_origin").getAsString();

			if (isValid) {

				Cookie[] cookies = request.getCookies();
				if (Objects.nonNull(state) && state.equals(Basf_Constant.FORGOT_PASSWORD)) {
					deleteCookie(cookies, state, response);
					responseJson.addProperty(Basf_Constant.STATUS, Basf_Constant.STATUS_SUCCESS);
					return responseJson;
				}

				JsonObject payloadJson = azureAuthService.getPayloadJson(id_token);
				payloadJson.addProperty("jwt_token", id_token);
				responseJson.add("userinfo", payloadJson);

				// talend

				JsonObject talendJson =null;// talendServise.talendAPI(id_token, state);

				JsonObject userDetilsjson = new JsonObject();

				if (request_origin.equals("same")) {
					// enduser flow
					userDetilsjson = eupfService.insertIntoUserDetils(payloadJson);
				} else {
//					partner flow
					// profile_Data = payloadJson

					// user exist check
					boolean user_exist = false;

					String sub = payloadJson.has("sub") ? payloadJson.get("sub").getAsString() : "";
					JsonObject requestJson = new JsonObject();
					requestJson.addProperty("sub", sub);
					requestJson.addProperty("type", "get_user_ids");
					JsonObject userIdJson = eupfService.callAwsRestService(requestJson);

					String contactId = "";
					String account_id = "";

					JsonArray userIdArray = userIdJson.get(Basf_Constant.DATA).getAsJsonArray();

					for (JsonElement userElement : userIdArray) {
						user_exist = true;
						JsonObject userJson = userElement.getAsJsonObject();
						if (userJson.has("contactId")) {
							contactId = userJson.get("contactId").getAsString();
						}
						if (userJson.has("account_id")) {
							account_id = userJson.get("account_id").getAsString();
						}
						break;
					}

					if (user_exist && account_id.isEmpty() /* || contactId.isEmpty() */ ) {
//						update
						if (talendJson.has("Profile_Data")) {
							JsonObject profile_Data = talendJson.get("Profile_Data").getAsJsonObject();
//							account_id = profile_Data.get("Account_BASF_ID").getAsString();
//							contactId = profile_Data.get("Contact_Id").getAsString();

							// update 2 fileds Contact_Id, Account_BASF_ID
							JsonObject requestJson2 = new JsonObject();

							profile_Data.addProperty("type", "update_user_details_id");
							userDetilsjson = eupfService.callAwsRestService(profile_Data);
							if (userDetilsjson.has(Basf_Constant.ERROR_MSG) && userDetilsjson
									.get(Basf_Constant.ERROR_MSG).getAsString().equals(Basf_Constant.STATUS_SUCCESS)) {
								// 2 filed are now updated
							} else {
								// error ocuured while updating
							}
						}
					} else {
//						? payloadJson OR Profile_Data ?
						userDetilsjson = eupfService.insertIntoUserDetils(payloadJson);
					}

					differentOriginFlow(responseJson, payloadJson);
				}

//				login/reg

				// db
//				JsonObject userDetilsjson = eupfService.insertIntoUserDetils(payloadJson);

				if (Objects.nonNull(userDetilsjson)) {
					if (userDetilsjson.has(Basf_Constant.ERROR_MSG)) {

						setCookieInResponse(tokenJson, response);
						responseJson.addProperty(Basf_Constant.STATUS, Basf_Constant.STATUS_SUCCESS);

						if (userDetilsjson.get(Basf_Constant.ERROR_MSG).getAsString()
								.equals(Basf_Constant.STATUS_SUCCESS)) {
							// signup email
							sendEmail(payloadJson);
						}
					}
				}
			} else {
				deleteCookie(request.getCookies(), state, response);
				responseJson.addProperty(Basf_Constant.STATUS, Basf_Constant.STATUS_FAIL);
				if (responseJson.has(Basf_Constant.ERROR_MSG)
						&& responseJson.get(Basf_Constant.ERROR_MSG).getAsString().isEmpty()) {
					responseJson.addProperty(Basf_Constant.ERROR_MSG, "token validation failed");
				}
			}

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

	private void differentOriginFlow(JsonObject responseJson, JsonObject payloadJson) throws Exception {

		if (responseJson.has(Basf_Constant.DATA)) {

			JsonArray jsonArray = responseJson.get(Basf_Constant.DATA).getAsJsonArray();
			int app_id = 0;
			for (JsonElement jsonElement : jsonArray) {
				JsonObject jsonObject = jsonElement.getAsJsonObject();
				if (jsonObject.has("app_id")) {
					app_id = jsonObject.get("app_id").getAsInt();
					break;
				}
			}
			JsonObject awsRequestJson = new JsonObject();
			awsRequestJson.addProperty("type", "get_app_data_from_appid");
			awsRequestJson.addProperty("app_id", app_id);

			JsonObject responseJson2 = eupfService.callAwsRestService(awsRequestJson); // get_form_data

			if (responseJson2.has(Basf_Constant.STATUS)
					&& responseJson2.get(Basf_Constant.STATUS).getAsString().equals(Basf_Constant.STATUS_SUCCESS)) {

				// to get ContactId & Account_BASF_ID
				String sub = payloadJson.has("sub") ? payloadJson.get("sub").getAsString() : "";
				JsonObject requestJson = new JsonObject();
				requestJson.addProperty("sub", sub);
				requestJson.addProperty("type", "get_user_ids");
				JsonObject userIdJson = eupfService.callAwsRestService(requestJson);

				if (userIdJson.has(Basf_Constant.STATUS)
						&& userIdJson.get(Basf_Constant.STATUS).getAsString().equals(Basf_Constant.STATUS_SUCCESS)) {

					if (userIdJson.has(Basf_Constant.DATA)) {
						// for talendRequestJson
						jsonArray = responseJson2.get(Basf_Constant.DATA).getAsJsonArray();
						JsonArray userIdArray = userIdJson.get(Basf_Constant.DATA).getAsJsonArray();

						requestJson.addProperty("account_type", "retailer");
						requestJson.addProperty("request_attribute", "APP");
						requestJson.addProperty("request_type", "FETCH");
						requestJson.addProperty("segment_id", "32");

						JsonObject talendRequestJson = eupfService.getTalendRequestJson(requestJson, jsonArray,
								userIdArray);

						String url = "https://run.mocky.io/v3/328cbc21-01a7-422a-8909-e47d29839499";
						JsonObject talendResponseJson = httpCallerService.callPost(talendRequestJson, url);
						responseJson.add("talend", talendResponseJson);

						if (Objects.nonNull(talendResponseJson)) {
							// for add attr_value in response
							talendServise.add_attr_value_in_response(talendResponseJson, jsonArray, responseJson2);
							responseJson.add("merged", responseJson2);
						}
					}
				}
			}

		}

	}

	private void sendEmail(JsonObject payloadJson) {

		String firstName = payloadJson.get("firstname").getAsString();
		String lastName = payloadJson.get("lastname").getAsString();
		String email = payloadJson.get("email").getAsString();

		String body = authConfigService.getSignupEmailBody();
		body = body.replace("First Name Last Name", firstName + " " + lastName);
		String subject = authConfigService.getSignupEmailSubject();

		Map<String, Object> jobProperties = new HashMap<>();
		if (Objects.nonNull(email)) {
			jobProperties.put("toEmail", email);
		}
		if (Objects.nonNull(subject)) {
			jobProperties.put("subject", subject);
		}
		if (Objects.nonNull(body)) {
			jobProperties.put("body", body);
		}
		jobManager.addJob(Basf_Constant.JOB_TOPIC_EMAIL, jobProperties);

	}

	private void setCookieInResponse(JsonObject tokenJson, SlingHttpServletResponse response) {

		for (String key : tokenJson.keySet()) {
			if (!key.equals("scope")) {
				String value = tokenJson.get(key).toString();
				Cookie cookie = new Cookie(key, value);
				cookie.setPath("/");
				// cookie.setHttpOnly(true);
				// cookie.setSecure(true);
				// cookie.setDomain("");
				if(!authConfigService.getCookie_domain_name().equalsIgnoreCase("")) {
					cookie.setDomain(authConfigService.getCookie_domain_name());
				}
				response.addCookie(cookie);
			}
		}
	}

	private void deleteCookie(Cookie[] cookies, String state, SlingHttpServletResponse response) {

		for (Cookie cookie : cookies) {
			String cookieName = cookie.getName();
			if (cookieName.equals("access_token") || cookieName.equals("id_token") || cookieName.equals("token_type")
					|| cookieName.equals("not_before") || cookieName.equals("expires_in")
					|| cookieName.equals("expires_on") || cookieName.equals("resource") || cookieName.equals("resource")
					|| cookieName.equals("id_token_expires_in") || cookieName.equals("profile_info")
					|| cookieName.equals("refresh_token") || cookieName.equals("refresh_token_expires_in")
					|| cookieName.equals("nonce") || cookieName.equals("state")) {

				Cookie ck = new Cookie(cookieName, null);
				ck.setPath("/");
				ck.setMaxAge(0);
				if(!authConfigService.getCookie_domain_name().equalsIgnoreCase("")) {
					ck.setDomain(authConfigService.getCookie_domain_name());
				}
				response.addCookie(ck);
			}
		}
	}

}
