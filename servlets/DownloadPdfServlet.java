package com.basfeupf.core.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.asset.api.AssetManager;
import com.adobe.granite.asset.api.Rendition;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.basfeupf.core.services.impl.AssetDownloadPdfServiceImpl;

import static com.basfeupf.core.constants.Constants.METADATA_PROPERTY_DAM_SHA1;

@Component(service = Servlet.class, immediate = true, enabled = true, name = "com.basfeupf.core.servlets.DownloadPdfServlet",
property = {
        "sling.servlet.paths=/bin/basf/basfeupf/portal-programs/downloadpdf",
        "sling.servlet.paths=/bin/basf/basfeupf/portal-programs/previewpdf",
        "sling.servlet.methods=GET"
})
public class DownloadPdfServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = -2038313532299222111L;
    private String defaultSearchPath;
    private static final Logger LOG = LoggerFactory.getLogger(DownloadPdfServlet.class);
    private ResourceResolver resourceResolver;
    @Reference
    private QueryBuilder builder;
    @Reference
    private AssetDownloadPdfServiceImpl assetDownloadPdfServiceImpl;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        resourceResolver = request.getResourceResolver();
        String assetPath = request.getParameter("assetPath");
        Map<String, String> map = new HashMap<>();
        map.put("path", assetPath);
        map.put("1_property", METADATA_PROPERTY_DAM_SHA1);
        map.put("1_property.value", request.getParameter("sha"));

        @SuppressWarnings("unchecked")
        Hit downloadAsset = assetDownloadPdfServiceImpl.getDownloadAsset(resourceResolver, request.getParameterMap(), assetPath);

        if (downloadAsset != null) {

            try {
                String path = downloadAsset.getPath();

                downloadAsset.getResource().getParent();

                AssetManager assetMgr = resourceResolver.adaptTo(AssetManager.class);

                path =  path.substring(path.indexOf("/"), path.indexOf("/jcr:content/metadata"));

                Asset myAsset = assetMgr.getAsset(path);

                Rendition myRen = myAsset.getRendition("original");

                InputStream is = myRen.getStream();

                byte[] byteArray = IOUtils.toByteArray(is);

                ValueMap hitValueMap = downloadAsset.getResource().getValueMap();

                if(request.getPathInfo().endsWith("downloadpdf")) {
                    response.setHeader("Content-disposition", "attachment;filename= "+ myAsset.getName());
                }
                else {
                    response.setHeader("Content-disposition", "inline;filename= "+ myAsset.getName());
                }

                response.setContentType(hitValueMap.get("dc:format", String.class));

                response.getOutputStream().write(byteArray);



            } catch (RepositoryException e) {
                LOG.error("Exception while writing json data for AssetServlet. Message: {}", e.getMessage());
                throw new IOException(e);
            }
            finally {
                response.getOutputStream().close();
            }

//			}

        }
    }

    public String getDefaultSearchPath() {
        return defaultSearchPath;
    }
}
