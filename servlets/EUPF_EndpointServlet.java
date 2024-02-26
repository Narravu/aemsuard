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
import com.basfeupf.core.services.AuthConfigService;
import com.basfeupf.core.services.AzureAuthService;
import com.basfeupf.core.services.EupfService;
import com.basfeupf.core.services.HttpCallerService;
import com.google.gson.JsonObject;

@Component(service = Servlet.class)
@SlingServletResourceTypes(resourceTypes = "/bin/basf/eupf", extensions = "json", methods = { HttpConstants.METHOD_GET,
		HttpConstants.METHOD_POST })
public class EUPF_EndpointServlet extends SlingAllMethodsServlet {

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
			String[] selectors = request.getRequestPathInfo().getSelectors();
			if (selectors.length > 0) {
				switch (selectors[0]) {
				case "testlambda":
					jsonObject = eupfService.testLambda(request, response);
					break;

				case "testemail":
					jsonObject = eupfService.callSignUpEmail(request, response);
					break;

				case "getazuretoken":

//					String nonce = request.getParameter("nonce");
					String nonce = "123456";
					
					JsonObject tokenJson = eupfService.getAzureToken(request, response);
					tokenJson.addProperty("nonce", nonce);

//					String token_type = jsonObject.get("token_type").getAsString();
//					String resource = jsonObject.get("resource").getAsString();
//					String access_token = jsonObject.get("access_token").getAsString();
//					String not_before = jsonObject.get("not_before").getAsString();
//					String expires_in = jsonObject.get("expires_in").getAsString();
//					String expires_on = jsonObject.get("expires_on").getAsString();
//					String id_token = jsonObject.get("id_token").getAsString();
//					String id_token_expires_in = jsonObject.get("id_token_expires_in").getAsString();
//					String scope = jsonObject.get("scope").getAsString();
//					String refresh_token = jsonObject.get("refresh_token").getAsString();
//					String refresh_token_expires_in = jsonObject.get("refresh_token_expires_in").getAsString();

					boolean status = azureAuthService.isValidToken(tokenJson);
					jsonObject.addProperty(Basf_Constant.STATUS, status);
					break;

				default:
					jsonObject.addProperty(Basf_Constant.ERROR_MSG, selectors[0] + " - no service found");
					break;
				}
			} else {
				jsonObject.addProperty(Basf_Constant.ERROR_MSG, "Please select a valid service");
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
					jsonObject.addProperty(Basf_Constant.ERROR_MSG, stringBuffer.toString());
					break;
				}
			}
		}
		return jsonObject;
	}

}
