package com.basfeupf.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.basfeupf.core.services.AuthConfigService;
import com.basfeupf.core.services.HttpCallerService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Component(service = Servlet.class)
@SlingServletResourceTypes(resourceTypes = "/apps/basfeupf/talend", extensions = "json", methods = HttpConstants.METHOD_POST )
public class TalendMockAPIServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;
	
	@Reference
	HttpCallerService httpCallerService;
	
	@Reference
	AuthConfigService authConfigService;

	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {

		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		
		JsonObject requestBody = httpCallerService.callGet(authConfigService.getTalendEndpointUrl());
//		String responseString = "{\r\n" + 
//				"    \"root\": {\r\n" + 
//				"        \"ContactData\": {\r\n" + 
//				"            \"Status\": \"Linked\",\r\n" + 
//				"			\"Request_Type\": \"Registration\",\r\n" + 
//				"			\"EUPF_ID\" : \"222\",\r\n" + 
//				"            \"Email_Address\": \"Kory@gmail.com\",\r\n" + 
//				"			\"ContactId\" : \"34567\",\r\n" + 
//				"            \"Contact_First_Name\": \"kory\",\r\n" + 
//				"            \"Contact_Last_Name\": \"Hayenga\",\r\n" + 
//				"            \"Mailing_Address\": \"123 Oak Glen\",\r\n" + 
//				"            \"City\": \"abc\",\r\n" + 
//				"            \"State\": \"CA\",\r\n" + 
//				"            \"Zipcode\": \"92618\",\r\n" + 
//				"			\"Country\" : \"US\",\r\n" + 
//				"            \"Phone_Number\": \"12345678\",\r\n" + 
//				"            \"Email_Opt_In\": \"Yes\",\r\n" + 
//				"            \"Preferred_Language\": \"English\",\r\n" + 
//				"            \"Mobile_Phone\": \"87654321\",\r\n" + 
//				"            \"Account_BASF_ID\": \"56789\",\r\n" + 
//				"			\"Business_Name\" : \"Corp\",\r\n" + 
//				"			\"Requested_Business_Segment\":\"Turf & Ornamental\",\r\n" + 
//				"			\"Requested_Account_Type\": \"End_User\",\r\n" + 
//				"			\"Account_Types\": [{\r\n" + 
//				"					\"account_type\": \"Distributor\",\r\n" + 
//				"					\"Segment\": [{\r\n" + 
//				"						\"bus_segment_id\" : \"30\",\r\n" + 
//				"						\"bus_segment_name\": \"PCS\"\r\n" + 
//				"					}, {\r\n" + 
//				"						\"bus_segment_id\" : \"32\",\r\n" + 
//				"						\"bus_segment_name\": \"Turf\"\r\n" + 
//				"					}]\r\n" + 
//				"				},\r\n" + 
//				"				{\r\n" + 
//				"					\"account_type \": \"End User\",\r\n" + 
//				"					\"Segment\": [{\r\n" + 
//				"						\"bus_segment_id\" : \"31\",\r\n" + 
//				"						\"bus_segment_name\": \"US Corps\"\r\n" + 
//				"					}, {\r\n" + 
//				"						\"bus_segment_id\" : \"32\",\r\n" + 
//				"						\"bus_segment_name\": \"Turf\"\r\n" + 
//				"					}]\r\n" + 
//				"				}\r\n" + 
//				"			]\r\n" + 
//				"        }\r\n" + 
//				"    }\r\n" + 
//				"}";

		JsonParser jsonParser = new JsonParser();
		JsonObject responseJson = requestBody;
		out.println(responseJson);
	}
}
