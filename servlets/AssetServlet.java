package com.basfeupf.core.servlets;

import com.basfeupf.core.models.AssetModel;
import com.basfeupf.core.services.impl.AssetDownloadPdfServiceImpl;
import java.io.IOException;
import java.util.List;
import com.google.gson.Gson;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Servlet.class, immediate = true, enabled = true, name = "com.basfeupf.core.servlets.AssetServlet",
property = {
        "sling.servlet.paths=/bin/basf/eupfportal/portal-programs/program-documents",
        "sling.servlet.paths=/bin/basf/eupfportal/portal-promotions/promotion-documents",
        "sling.servlet.paths=/bin/basf/eupfportal/portal-supply-reports/supply-reports-documents",
        "sling.servlet.paths=/bin/basf/eupfportal/portal-price-sheets/price-sheets-documents",
        "sling.servlet.methods=GET"
})
public class AssetServlet extends SlingSafeMethodsServlet {
    private static final long serialVersionUID = -8000220961976170686L;
    private String defaultSearchPath;

    // Default logger
    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // Instance of ResourceResolver
    private ResourceResolver resourceResolver;

//	// JCR Session instance
//	private Session session;
//
//	@Reference
//	private QueryBuilder builder;
//
//	private String json = "";

    private static final Gson gson = new Gson();

    @Reference
    private AssetDownloadPdfServiceImpl assetDownloadPdfServiceImpl;

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.
     * sling.api.SlingHttpServletRequest,
     * org.apache.sling.api.SlingHttpServletResponse)
     */

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        resourceResolver = request.getResourceResolver();

        String businessSegmentId = request.getParameter("busSegId");

        String servletPath = request.getPathInfo();

        log.info("bus segment {}", businessSegmentId);

        List<AssetModel> assetModel = assetDownloadPdfServiceImpl.getAssetList(resourceResolver, businessSegmentId, servletPath);

        String jsonStr = gson.toJson(assetModel);
        response.getWriter().write(jsonStr);

    }

    public String getDefaultSearchPath() {
        return defaultSearchPath;
    }
}
