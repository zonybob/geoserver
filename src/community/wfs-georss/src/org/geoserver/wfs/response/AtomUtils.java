/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.opengis.feature.simple.SimpleFeature;

/**
 * The AtomUtils class provides some static methods useful in producing atom metadata related to 
 * GeoServer features.
 *
 * @author David Winslow
 * @author Jonathan Meyer, Applied Information Sciences, jon@gisjedi.com
 */
public final class AtomUtils {
	
	protected static Logger LOGGER = org.geotools.util.logging.Logging.getLogger(AtomUtils.class);

    /**
     * A date formatting object that does most of the formatting work for RFC3339.  Note that since 
     * Java's SimpleDateFormat does not provide all the facilities needed for RFC3339 there is still
     * some custom code to finish the job.
     */
    private static DateFormat rfc3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * A number formatting object to format the the timezone offset info in RFC3339 output.
     */
    private static NumberFormat doubleDigit = new DecimalFormat("00");

    /**
     * A FeatureTemplate used for formatting feature info.
     * @TODO: Are these things threadsafe?
     */
    private static FeatureTemplate featureTemplate = new FeatureTemplate();

    /**
     * This is a utility class so don't allow instantiation.
     */
    private AtomUtils(){ /* Nothing to do */ }

    /**
     * Format dates as specified in rfc3339 (required for Atom dates)
     * @param d the Date to be formatted
     * @return the formatted date
     */
    public static String dateToRFC3339(Date d){
        StringBuilder result = new StringBuilder(rfc3339.format(d));
        Calendar cal = new GregorianCalendar();
        cal.setTime(d);
        cal.setTimeZone(TimeZone.getDefault());
        int offset_millis = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);
        int offset_hours = Math.abs(offset_millis / (1000 * 60 * 60));
        int offset_minutes = Math.abs((offset_millis / (1000 * 60)) % 60);

        if (offset_millis == 0) {
            result.append("Z");
        } else {
            result
                .append((offset_millis > 0) ? "+" : "-")
                .append(doubleDigit.format(offset_hours))
                .append(":")
                .append(doubleDigit.format(offset_minutes));
        }

        return result.toString();
    }

    public static String getEntryURL(SimpleFeature feature){
        try{
            return featureTemplate.link(feature);
        } catch (IOException ioe){
            return feature.getID();
        }
    }

    public static String getEntryURI(SimpleFeature feature){
        return getEntryURL(feature);
    }

    public static String getFeatureTitle(SimpleFeature feature){
        try{
            return featureTemplate.title(feature);
        } catch (IOException ioe){
            return feature.getID();
        }
    }

    public static String getFeatureDescription(SimpleFeature feature){
        try{
            return featureTemplate.description(feature);
        } catch (IOException ioe) {
            return feature.getID();
        }
    }

    public static String getFeedURL(Operation op){
    	try{
	    	GetFeatureRequest request = GetFeatureRequest.adapt(op.getParameters()[0]);
	    	QName typeName = request.getQueries().get(0).getTypeNames().get(0);
	
	        HashMap<String,String> params = new HashMap<String,String>();
	        params.put("outputFormat", request.getOutputFormat());
	        params.put("typeName",  typeName.getPrefix() + ":" + typeName.getLocalPart());
	        params.put("request", "GetFeature");
	        
	    	return ResponseUtils.buildURL(request.getBaseUrl(), "wfs", 
	    			params, URLType.SERVICE);
    	}
    	catch (Exception ex) {
    		LOGGER.severe("Unable to create feed url\n" + ex.getMessage()	);
    		return "Error while creating feed url";
    	}
    }

    public static String getFeedURI(Operation op){
        return getFeedURL(op);
    }

    public static String getFeedTitle(Operation op, GeoServer gs){
    	String title = "";
    	
    	try {
	    	Catalog catalog = gs.getCatalog();
	    	
	    	GetFeatureRequest request = GetFeatureRequest.adapt(op.getParameters()[0]);
	    	List<QName> typeNames = request.getQueries().get(0).getTypeNames();
	    	
	    	for (QName typeName : typeNames) {
	    		FeatureTypeInfo meta = catalog.getFeatureTypeByName(typeName.getNamespaceURI(), typeName.getLocalPart());
	    		title += meta.getTitle() + "\n";
	    	}
    	}
    	catch (Exception ex) {
    		LOGGER.warning("Unable to determine a valid title for feed:" + ex.getMessage());
    		title = "Auto-generated by geoserver";
    	}
    	
    	// Remove trailing carriage return
    	title = title.substring(0, title.length() - 1);
    	
    	return title;
    }

    public static String getFeedDescription(Operation op, GeoServer gs) {
    	String description = "";
    	
    	try {
	    	Catalog catalog = gs.getCatalog();
	    	
	    	GetFeatureRequest request = GetFeatureRequest.adapt(op.getParameters()[0]);
	    	List<QName> typeNames = request.getQueries().get(0).getTypeNames();
	    	
	    	for (QName typeName : typeNames) {
	    		FeatureTypeInfo meta = catalog.getFeatureTypeByName(typeName.getNamespaceURI(), typeName.getLocalPart());
	    		description += meta.getAbstract() + "\n";
	    	}
    	}
    	catch (Exception ex) {
    		LOGGER.warning("Unable to determine a valid description for feed:" + ex.getMessage());
    		description = "Auto-generated by geoserver";
    	}
    	
    	// Remove trailing carriage return
    	description = description.substring(0, description.length() - 1);
    	
    	return description;
    }
}
