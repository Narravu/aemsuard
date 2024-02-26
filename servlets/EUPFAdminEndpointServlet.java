package com.basfeupf.core.servlets;

import com.basfeupf.core.constants.Basf_Constant;
import com.basfeupf.core.services.*;
import com.google.gson.JsonObject;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;

@Component(service = Servlet.class)
@SlingServletResourceTypes(resourceTypes = "/bin/eupf/admin", extensions = "json", methods = {HttpConstants.METHOD_GET,
        HttpConstants.METHOD_POST})
public class EUPFAdminEndpointServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    @Reference
    HttpCallerService httpCallerService;

    @Reference
    AzureAuthService azureAuthService;

    @Reference
    AuthConfigService authConfigService;

    @Reference
    EupfAdminService eupfAdminService;

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
                    case "get_attributes_list":
                        jsonObject = eupfAdminService.getAttrbutesList(request, response);
                        break;
                    case "get_segments_list":
                        jsonObject = eupfAdminService.getSegmentsList(request, response);
                        break;
                    case "get_apps_list":
                        jsonObject = eupfAdminService.getAppsList(request, response);
                        break;
                    case "insert_attribute_detail":
                        jsonObject = eupfAdminService.insertAttributeDetails(request, response);
                        break;
                    case "update_attribute_detail":
                        jsonObject = eupfAdminService.updateAttributeDetails(request, response);
                        break;
                    case "insert_segment_detail":
                        jsonObject = eupfAdminService.insertSegmentDetails(request, response);
                        break;
                    case "update_segment_detail":
                        jsonObject = eupfAdminService.updateSegmentDetails(request, response);
                        break;
                    case "get_segment_attribute_map":
                        jsonObject = eupfAdminService.getAttributeSegmentMap(request, response);
                        break;
                    case "insert_segment_attribute_map":
                        jsonObject = eupfAdminService.insertAttributeSegmentMap(request, response);
                        break;
                    case "insert_app_detail":
                        jsonObject = eupfAdminService.insertApplicationDetails(request, response);
                        break;
                    case "update_app_detail":
                        jsonObject = eupfAdminService.updateApplicationDetails(request, response);
                        break;
                    case "get_app_segment_attribute_map":
                        jsonObject = eupfAdminService.getAppSegmentAttributeMap(request, response);
                        break;
                    case "insert_app_segment_attribute_map":
                        jsonObject = eupfAdminService.insertAppSegmentAttributeMap(request, response);
                        break;
                    case "delete_segment_attribute_map":
                        jsonObject = eupfAdminService.deleteSegmentAttributeMap(request, response);
                        break;
                    case "delete_app_segment_attribute_map":
                        jsonObject = eupfAdminService.deleteAppSegmentAttributeMap(request, response);
                        break;
                    case "delete_attribute_detail":
                        jsonObject = eupfAdminService.deleteAttributeDetail(request, response);
                        break;
                    case "delete_segment_detail":
                        jsonObject = eupfAdminService.deleteSegmentDetail(request, response);
                        break;
                    case "delete_app_detail":
                        jsonObject = eupfAdminService.deleteAppDetail(request, response);
                        break;
                    case "insert_user_group":
                        jsonObject = eupfAdminService.insertUserGroup(request, response);
                        break;
                    case "delete_user_group":
                        jsonObject = eupfAdminService.deleteusergroup(request, response);
                        break;
                    case "update_user_group":
                        jsonObject = eupfAdminService.updateusergroup(request, response);
                        break;
                    case "get_user_group_list":
                        jsonObject = eupfAdminService.getUserGroupList(request, response);
                        break;
                    case "insert_attribute_locale_map":
                        jsonObject = eupfAdminService.insertAttributeLocaleMap(request, response);
                        break;
                    case "delete_attribute_locale_map":
                        jsonObject = eupfAdminService.deleteAttributeLocaleMap(request, response);
                        break;
                    case "get_attribute_locale_map":
                        jsonObject = eupfAdminService.getAttributeLocaleMap(request, response);
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
