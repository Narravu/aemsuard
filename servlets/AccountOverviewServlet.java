package com.basfeupf.core.servlets;

import com.basfeupf.core.services.*;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import java.io.IOException;
import javax.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.Servlet;
import com.google.gson.JsonObject;

@Component(service = Servlet.class,immediate = true)
@SlingServletResourceTypes(resourceTypes = "/bin/eupf/repinfo/repdetails", extensions = "json", methods = {
		HttpConstants.METHOD_POST })
public class AccountOverviewServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;

	@Reference
	AuthConfigService authConfigService;
	@Reference
	TalendServise talendServise;
	@Reference
	private HttpClientBuilderFactory httpClientBuilderFactory;
	Logger logger = LoggerFactory.getLogger(this.getClass());
	JsonObject jsonObject = new JsonObject();

	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
		talendServise.doProcess(request, response);

	}
}