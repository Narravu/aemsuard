package com.basfeupf.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;

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
import com.basfeupf.core.services.AzureAuthService;
import com.basfeupf.core.services.EupfService;
import com.google.gson.JsonObject;

@Component(service = Servlet.class)
@SlingServletResourceTypes(resourceTypes = "/bin/eupf/check/login", extensions = "json", methods = {
		HttpConstants.METHOD_POST })
public class CheckLoggedInServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;

	@Reference
	AzureAuthService azureAuthService;

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

		JsonObject responseJson = new JsonObject();
		try {
			responseJson = azureAuthService.isValidToken(request, response);
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
