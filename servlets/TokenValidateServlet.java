package com.basfeupf.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

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
import com.basfeupf.core.services.AzureAuthService;
import com.basfeupf.core.services.EupfService;
import com.basfeupf.core.services.HttpCallerService;
import com.google.gson.JsonObject;

@Component(service = Servlet.class)
@SlingServletResourceTypes(resourceTypes = "/bin/eupf/token/validate", extensions = "json", methods = {
		HttpConstants.METHOD_POST })
public class TokenValidateServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;

	@Reference
	HttpCallerService httpCallerService;

	@Reference
	AzureAuthService azureAuthService;

	@Reference
	AuthConfigService authConfigService;

	@Reference
	EupfService eupfService;

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

		JsonObject jsonObject = new JsonObject();
		try {
			JsonObject tokenJson = eupfService.getTokenJson(request);
			String nonce = "123456";
			tokenJson.addProperty("nonce", nonce);
			boolean isValid = azureAuthService.isValidToken(tokenJson);

			if (isValid) {
				String id_token = tokenJson.get("id_token").getAsString();
				JsonObject payloadJson = azureAuthService.getPayloadJson(id_token);
				payloadJson.addProperty("jwt_token", id_token);
				String firstName = payloadJson.get("firstname").getAsString();
				String lastName = payloadJson.get("lastname").getAsString();
				String email = payloadJson.get("email").getAsString();

				// db
				JsonObject userDetilsjson = eupfService.insertIntoUserDetils(payloadJson);
				
				if (userDetilsjson.has(Basf_Constant.ERROR_MSG)) {
					if (userDetilsjson.get(Basf_Constant.ERROR_MSG).getAsString()
							.equals(Basf_Constant.STATUS_SUCCESS)) {

						// email
						String body = authConfigService.getSignupEmailBody();
						body = body.replace("First Name Last Name", firstName + " " + lastName);
						boolean isEmailSend = eupfService.sendEmail(null, null, email, null, body, null);

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
						// add token in session response
						jsonObject.addProperty("isSignUpEmailSend", isEmailSend);
						jsonObject.add(Basf_Constant.DATA, tokenJson);
						jsonObject.addProperty(Basf_Constant.STATUS, Basf_Constant.STATUS_SUCCESS);
						//response.sendRedirect("/content/basfeupf/us/en/sign-in.html");
					} else {
						jsonObject.addProperty(Basf_Constant.STATUS, Basf_Constant.STATUS_FAIL);
						jsonObject.add(Basf_Constant.DATA, userDetilsjson);
					}
				}
			} else {
				jsonObject.addProperty(Basf_Constant.STATUS, Basf_Constant.STATUS_FAIL);
				jsonObject.addProperty(Basf_Constant.ERROR_MSG, "token validation failed");
			}
		} catch (Exception e) {
			jsonObject.addProperty(Basf_Constant.ERROR_MSG, e.getClass().getSimpleName() + " : " + e.getMessage());
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
					break;
				}
			}
		}
		return jsonObject;

	}

}
