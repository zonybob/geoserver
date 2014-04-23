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
import java.util.TimeZone;

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
    	GetFeatureRequest request = GetFeatureRequest.adapt(op.getParameters()[0]);
        return request.getQueries().get(0).toString().replace(' ', '+');
    }

    public static String getFeedURI(Operation op){
        return getFeedURL(op);
    }

    public static String getFeedTitle(Operation op){
    	GetFeatureRequest request = GetFeatureRequest.adapt(op.getParameters()[0]);
    	String title = request.getQueries().get(0).getTypeNames().get(0).getLocalPart();
        return title.toString();
    }

    public static String getFeedDescription(Operation op) {
    	return "Auto-generated by geoserver";
    }
}
