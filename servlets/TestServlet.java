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
import com.basfeupf.core.services.EupfServiceNew;
import com.basfeupf.core.services.TalendServise;
import com.google.gson.JsonObject;

@Component(service = Servlet.class)
@SlingServletResourceTypes(resourceTypes = "/bin/eupf/batchtest", extensions = "json", methods = {
		HttpConstants.METHOD_GET, HttpConstants.METHOD_POST })
public class TestServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;

	@Reference
	TalendServise talendServise;

	@Reference
	AuthConfigService authConfigService;
	
	@Reference
	EupfServiceNew eupfService;

	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {

		try {
			JsonObject userRequestJson = new JsonObject();
			
//			userRequestJson.addProperty("type", "test");
//			JsonObject userIdJson = eupfService.callAwsRestService(userRequestJson);
//			logger.debug("userIdJson"+userIdJson.toString());
			talendServise.batchUpdate(10000000);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.debug("userIdJson error"+e);
			e.printStackTrace();
		}
	}

	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {

		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		out.println("");
	}

	

}
