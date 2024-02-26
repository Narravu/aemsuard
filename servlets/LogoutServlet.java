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
import com.basfeupf.core.services.EupfService;
import com.google.gson.JsonObject;

@Component(service = Servlet.class)
@SlingServletResourceTypes(resourceTypes = "/bin/eupf/logout", extensions = "json", methods = {
		HttpConstants.METHOD_GET, HttpConstants.METHOD_POST })
public class LogoutServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;

	@Reference
	EupfService eupfService;

	@Reference
	AuthConfigService authConfigService;

	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {

		doPost(request, response);
	}

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
			Cookie[] cookies = request.getCookies();
			for (Cookie cookie : cookies) {
				String cookieName = cookie.getName();
				if (cookieName.equals("access_token") || cookieName.equals("id_token")
						|| cookieName.equals("token_type") || cookieName.equals("not_before")
						|| cookieName.equals("expires_in") || cookieName.equals("expires_on")
						|| cookieName.equals("resource") || cookieName.equals("resource")
						|| cookieName.equals("id_token_expires_in") || cookieName.equals("profile_info")
						|| cookieName.equals("refresh_token") || cookieName.equals("refresh_token_expires_in")
						|| cookieName.equals("nonce") || cookieName.equals("state") || cookieName.equals("jwt_token")) {

					Cookie ck = new Cookie(cookieName, null);
					ck.setPath("/");
					ck.setMaxAge(0);
					if(!authConfigService.getCookie_domain_name().equalsIgnoreCase("")) {
						ck.setDomain(authConfigService.getCookie_domain_name());
					}
					response.addCookie(ck);
				}
			}
			response.sendRedirect(authConfigService.getLogoutRedirectUri());

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

}
