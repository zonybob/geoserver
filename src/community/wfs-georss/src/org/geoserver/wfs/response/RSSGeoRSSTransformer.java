/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.xml.transform.Translator;
import org.opengis.feature.simple.SimpleFeature;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;

/**
 * Encodes an RSS feed tagged with geo information.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * @author Jonathan Meyer, Applied Information Sciences, jon@gisjedi.com
 *
 */
public class RSSGeoRSSTransformer extends GeoRSSTransformerBase {
    
    private GeoServer gs;
    private Operation op;

    public RSSGeoRSSTransformer(GeoServer gs, Operation op){
        this.gs = gs;
        this.op = op;
    }
    
    public Translator createTranslator(ContentHandler handler) {
        return new RSSGeoRSSTranslator(this.gs, handler, this.op);
    }

    class RSSGeoRSSTranslator extends GeoRSSTranslatorSupport {
        private GeoServer gs;
        private Operation op;

        public RSSGeoRSSTranslator(GeoServer gs, ContentHandler contentHandler, Operation op) {
            super(contentHandler, null, null);
            this.op = op;
            this.gs = gs;
            nsSupport.declarePrefix("georss", "http://www.georss.org/georss");
            nsSupport.declarePrefix("atom", "http://www.w3.org/2005/Atom");
        }

        public void encode(Object o) throws IllegalArgumentException {
            FeatureCollectionResponse features = (FeatureCollectionResponse) o;

            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, "version", "version", null, "2.0");

            start("rss", atts);
            start("channel");
            
            element( "title", AtomUtils.getFeedTitle(this.op) );
            element("description", AtomUtils.getFeedDescription(this.op) );
            
            start( "link" );
            cdata(AtomUtils.getFeedURL(this.op));
            end( "link" );
            
            atts = new AttributesImpl();
            atts.addAttribute(null, "href", "href", null, AtomUtils.getFeedURL(this.op));
            atts.addAttribute(null, "rel", "rel", null, "self");
            element("atom:link", null, atts);

            //items
            try {
                encodeItems(features);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            end("channel");
            end("rss");
        }

        void encodeItems(FeatureCollectionResponse featureCollection) throws IOException {
        	List<FeatureCollection> resultsList = featureCollection.getFeatures();
            for (Iterator f = resultsList.iterator(); f.hasNext(); ) {
                SimpleFeatureCollection features = (SimpleFeatureCollection) f.next();
                FeatureIterator <SimpleFeature> iterator = null;

                try {
                    iterator = features.features();

                    while (iterator.hasNext()) {
                        SimpleFeature feature = iterator.next();
                        try {
                            encodeItem(feature);    
                        }
                        catch( Exception e ) {
                            LOGGER.warning("Encoding failed for feature: " + feature.getID());
                            LOGGER.log(Level.FINE, "", e );
                        }
                        
                    }
                } finally {
                    if (iterator != null) {
                        iterator.close();
                    }
                }
                
            }
        }

        void encodeItem(SimpleFeature feature)
            throws IOException {
            start("item");

            String title = feature.getID();
            String link = null; 
            String description = "[Error while loading description]";

            try {
                title = AtomUtils.getFeatureTitle(feature);
                link = AtomUtils.getEntryURL(feature);
                description = AtomUtils.getFeatureDescription(feature);
            } catch( Exception e ) {
                String msg = "Error occured executing title template for: " + feature.getID();
                LOGGER.log( Level.WARNING, msg, e );
            }

            element("title", title);
            
            //create the link as getFeature request with fid filter
            start("link");
            cdata(link);
            end("link");

            start("guid");
            cdata(link);
            end("guid");

            start("description");
            cdata(AtomUtils.getFeatureDescription(feature));
            end("description");
            
            GeometryCollection col = feature.getDefaultGeometry() instanceof GeometryCollection 
                ? (GeometryCollection) feature.getDefaultGeometry()
                : null;

            if (geometryEncoding == GeometryEncoding.LATLONG 
                || (col == null && feature.getDefaultGeometry() != null)) {
                geometryEncoding.encode((Geometry)feature.getDefaultGeometry(), this);
                end("item");
            } else {
                geometryEncoding.encode(col.getGeometryN(0), this);
                end("item");

                for (int i = 1; i < col.getNumGeometries(); i++){
                    encodeRelatedGeometryItem(col.getGeometryN(i), title, link, i);
                }
            }
        }

        void encodeRelatedGeometryItem(Geometry g, String title, String link, int count){
            start("item");
            element("title", "Continuation of " + title);
            element("link", link);
            element("guid", link + "#" + count);
            element("description", "Continuation of " + title);
            geometryEncoding.encode(g, this);
            end("item");
        }
    }
}
