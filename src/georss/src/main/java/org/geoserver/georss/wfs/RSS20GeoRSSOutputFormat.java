package org.geoserver.georss.wfs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.TransformerException;

import org.geoserver.config.GeoServer;
import org.geoserver.georss.GeoRSSTransformerBase;
import org.geoserver.georss.GeoRSSTransformerBase.GeometryEncoding;
import org.geoserver.georss.RSS20GeoRSSTransformer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;

/**
 * 
 * @author Jonathan Meyer, Applied Information Sciences, jon@gisjedi.com
 *
 */
public class RSS20GeoRSSOutputFormat extends WFSGetFeatureOutputFormat {
	
	protected static Logger LOGGER = org.geotools.util.logging.Logging.getLogger(RSS20GeoRSSOutputFormat.class);
	
    public static String formatName = "GeoRSS";
    public static final String MIME_TYPE = "application/rss+xml";
    
	/**
	 * Constructor to allow formats to be specified in the applicationContext.xml
	 * @param gs
	 * @param selectedFormat
	 */
	public RSS20GeoRSSOutputFormat(GeoServer gs, String selectedFormat) {
		super(gs, selectedFormat);
	}

    /**
     * Returns the mime type
     */
	@Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return MIME_TYPE;
    }
	
    /**
     * capabilities output format string.
     */
    public String getCapabilitiesElementName() {
        return formatName;
    }
	
	@Override
	protected void write(FeatureCollectionResponse featureCollection, OutputStream output,
			Operation op) throws IOException, ServiceException {
		
		RSS20GeoRSSTransformer tx = new RSS20GeoRSSTransformer(gs, op);
        
        GetFeatureRequest request = GetFeatureRequest.adapt(op.getParameters()[0]);
        GeometryEncoding geometryEncoding = GeoRSSTransformerBase.GeometryEncoding.SIMPLE;
        if (request != null) {
        	String geometryEncodingString = null;

            Map<String, ?> formatOptions = request.getFormatOptions();
            geometryEncodingString = (String) formatOptions.get("ENCODING");
            
            if (geometryEncodingString != null) {
            	LOGGER.log(Level.INFO, "Overriding geometry encoding with value specified in format options: " + geometryEncodingString);
    			if ("gml".equals(geometryEncodingString.toLowerCase())) {
    				geometryEncoding = GeoRSSTransformerBase.GeometryEncoding.GML;
    			} else if ("latlong".equals(geometryEncodingString.toLowerCase())) {
    				geometryEncoding = GeoRSSTransformerBase.GeometryEncoding.LATLONG;
    			}
            }
            
            String continuationEnabledString = (String) formatOptions.get("CONTINUATION");
            if (continuationEnabledString != null) {
            	LOGGER.log(Level.INFO, "Overriding continuation boolean with value specified in format options: " + continuationEnabledString);
            	
            	Boolean continuationEnabled = Boolean.parseBoolean(continuationEnabledString);
            	tx.setContinuation(continuationEnabled);
            }
        }
		// We don't care if format options were not set, default to SIMPLE geom encoding 
        tx.setGeometryEncoding(geometryEncoding);
        
		Charset encoding = Charset.forName(gs.getSettings().getCharset());
		tx.setEncoding(encoding);
		
		try {
			tx.transform(featureCollection, output);
		} catch (TransformerException e) {
			ServiceException serviceException = new ServiceException("Error: " + e.getMessage());
			serviceException.initCause(e);
			throw serviceException;
		}
    }
}
