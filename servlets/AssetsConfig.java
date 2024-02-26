package com.basfeupf.core.servlets;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "[BASF] Digital Connect Protected Asset Configuration",
        description = "Configuration for Asset and DownloadPdf Servlets")

public @interface AssetsConfig {

    @AttributeDefinition(name = "Protected document DAM path",
            description = "Assets Path for Digital Connect protected documents",
            defaultValue = "/content/dam/basfeupf/rewards-and-programs/programs")

    public String assetSearchPath();

    @AttributeDefinition(name = "Asset thumbnail",
            description = "Asset thumbnail returned with asset list",
            defaultValue = "{Boolean}false",
            type = AttributeType.BOOLEAN)

    public boolean showAssetThumbnail();

    @AttributeDefinition(name = "Protected document DAM path",
            description = "Assets Path for Partner Portal protected documents",
            defaultValue = "/content/dam/basfeupf/rewards-and-programs/promotions")

    public String assetPromotionPath();

    @AttributeDefinition(name = "Protected document DAM path",
            description = "Assets Path for Partner Portal Supply Report protected documents",
            defaultValue = "/content/dam/basfeupf/supply-reports")

    public String assetSupplyReportPath();

    @AttributeDefinition(name = "Protected document DAM path",
            description = "Assets Path for Partner Portal Price Sheets protected documents",
            defaultValue = "/content/dam/basfeupf/price-sheets")

    public String assetPriceSheetsPath();
}
