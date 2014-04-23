package org.geoserver.wfs.response;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;

import javax.xml.transform.TransformerException;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.json.JSONType;
import org.geoserver.wfs.request.FeatureCollectionResponse;

/**
 * 
 * @author Jonathan Meyer, Applied Information Sciences, jon@gisjedi.com
 *
 */
public class GeoRSSOutputFormat extends WFSGetFeatureOutputFormat {
	
    public static String formatName = "GeoRSS";
    public static final String MIME_TYPE = "application/rss+xml";

	public GeoRSSOutputFormat(GeoServer gs) {
		super(gs, formatName);
	}
	
	/**
	 * Constructor to allow formats to be specified in the applicationContext.xml
	 * @param gs
	 * @param selectedFormat
	 */
	public GeoRSSOutputFormat(GeoServer gs, String selectedFormat) {
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
		
		RSSGeoRSSTransformer tx = new RSSGeoRSSTransformer(gs, op);
		
		if (featureCollection.getFormatOptions() != null && featureCollection.getFormatOptions().containsKey("encoding")) {			
			String geometryEncoding = (String) featureCollection.getFormatOptions().get("encoding");
			if ("gml".equals(geometryEncoding)) {
				tx.setGeometryEncoding(GeoRSSTransformerBase.GeometryEncoding.GML);
			} else if ("latlong".equals(geometryEncoding)) {
				tx.setGeometryEncoding(GeoRSSTransformerBase.GeometryEncoding.LATLONG);
			} else {
				tx.setGeometryEncoding(GeoRSSTransformerBase.GeometryEncoding.SIMPLE);
			}
		}
		// We don't care if format options were not set, default to SIMPLE geom encoding 
		else 
		{
			tx.setGeometryEncoding(GeoRSSTransformerBase.GeometryEncoding.SIMPLE);
		}
		
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
