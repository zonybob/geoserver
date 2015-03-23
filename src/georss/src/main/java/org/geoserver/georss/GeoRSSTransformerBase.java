/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.georss;

import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.geotools.gml3.GMLConfiguration;
import org.geotools.xml.Configuration;
import org.geotools.xml.Encoder;
import org.geotools.xml.transform.TransformerBase;
import org.opengis.feature.simple.SimpleFeature;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Base class for RSS/Atom xml transformers
 * 
 * 
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * @author Andrea Aime - GeoSolutions
 */
public abstract class GeoRSSTransformerBase extends TransformerBase {
    /** logger */
    protected static Logger LOGGER = org.geotools.util.logging.Logging.getLogger(GeoRSSTransformerBase.class);
    
    static final Configuration GML_CONFIGURATION = new GMLConfiguration();

    /**
     * Enumeration for geometry encoding.
     *
     * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
     *
     */
    public static class GeometryEncoding {
        private GeometryEncoding() {
        }

        public String getPrefix() {
            return null;
        }

        public String getNamespaceURI() {
            return null;
        }

        public void encode(Geometry g, GeoRSSTranslatorSupport translator) {
        }

        /**
         * "simple" encoding:
         *
         *  ex:
         *  <georss:point>45.256 -71.92</georss:point>,<georss:line>...</georss:line>,...
         */
        public static GeometryEncoding SIMPLE = new GeometryEncoding() {
                public String getPrefix() {
                    return "georss";
                }

                public String getNamespaceURI() {
                    return "http://www.georss.org/georss";
                }

                public void encode(Geometry g, GeoRSSTranslatorSupport t) {
                    if (g instanceof Point) {
                        Point p = (Point) g;
                        t.element("georss:point", p.getY() + " " + p.getX());
                    }

                    if (g instanceof LineString) {
                        LineString l = (LineString) g;

                        StringBuffer sb = new StringBuffer();

                        for (int i = 0; i < l.getNumPoints(); i++) {
                            Coordinate c = l.getCoordinateN(i);
                            sb.append(c.y).append(" ").append(c.x).append(" ");
                        }

                        sb.setLength(sb.length() - 1);

                        t.element("georss:line", sb.toString());
                    }

                    if (g instanceof Polygon) {
                        Polygon p = (Polygon) g;
                        LineString line = p.getExteriorRing();

                        StringBuffer sb = new StringBuffer();

                        for (int i = 0; i < line.getNumPoints(); i++) {
                            Coordinate c = line.getCoordinateN(i);
                            sb.append(c.y).append(" ").append(c.x).append(" ");
                        }

                        sb.setLength(sb.length() - 1);

                        t.element("georss:polygon", sb.toString());
                    }
                }
            };

        /**
         * gml encoding:
         *
         * ex:
         * <gml:Point>
         *   <gml:pos>45.256 -71.92</gml:pos>
         * </gml:Point>
         */
        public static GeometryEncoding GML = new GeometryEncoding() {
                public String getPrefix() {
                    return "gml";
                }

                public String getNamespaceURI() {
                    return "http://www.opengis.net/gml";
                }
                
                public void encode(Geometry g, final GeoRSSTranslatorSupport translator) {
                    try {
                        // get the proper element name
                        QName elementName = null;
                        if (g instanceof Point) {
                            elementName = org.geotools.gml2.GML.Point;
                        } else if (g instanceof LineString) {
                            elementName = org.geotools.gml2.GML.LineString;
                        } else if (g instanceof Polygon) {
                            elementName = org.geotools.gml2.GML.Polygon;
                        } else if (g instanceof MultiPoint) {
                            elementName = org.geotools.gml2.GML.MultiPoint;
                        } else if (g instanceof MultiLineString) {
                            elementName = org.geotools.gml2.GML.MultiLineString;
                        } else if (g instanceof MultiPolygon) {
                            elementName = org.geotools.gml2.GML.MultiPolygon;
                        } else {
                            elementName = org.geotools.gml2.GML._Geometry;
                        }
                        
                        // encode in GML3
                        Encoder encoder = new Encoder(GML_CONFIGURATION);
                        encoder.encode(g, elementName, translator);
                    } catch(Exception e) {
                        throw new RuntimeException("Cannot transform the specified geometry in GML", e);
                    }
                };
            };

        /**
         * lat/long encoding:
         *
         * ex:
         * <geo:lat>45.256</geo:lat>
         * <geo:long>-71.92</geo:long>
         *
         */
        public static GeometryEncoding LATLONG = new GeometryEncoding() {
                public String getPrefix() {
                    return "geo";
                }

                public String getNamespaceURI() {
                    return "http://www.w3.org/2003/01/geo/wgs84_pos#";
                }

                public void encode(Geometry g, GeoRSSTranslatorSupport t) {
                    //encode the centroid
                    Point p = g.getCentroid();
                    t.element("geo:lat", "" + p.getY());
                    t.element("geo:long", "" + p.getX());
                }
            };
    }
    ;

    /**
     * Geometry encoding to use.
     */
    protected GeometryEncoding geometryEncoding = GeometryEncoding.LATLONG;

    public void setGeometryEncoding(GeometryEncoding geometryEncoding) {
        this.geometryEncoding = geometryEncoding;
    }

    abstract class GeoRSSTranslatorSupport extends TranslatorSupport implements ContentHandler {
        public GeoRSSTranslatorSupport(ContentHandler contentHandler, String prefix, String nsURI) {
            super(contentHandler, prefix, nsURI);

            nsSupport.declarePrefix(geometryEncoding.getPrefix(), geometryEncoding.getNamespaceURI());
        }

        /**
         * Encodes the geometry of a feature.
         *
         */
        protected void encodeGeometry(SimpleFeature feature) {
            if (feature.getDefaultGeometry() != null) {
                Geometry g = (Geometry) feature.getDefaultGeometry();

                //handle case of multi geometry with a single geometry in it
                if (g instanceof GeometryCollection) {
                    GeometryCollection mg = (GeometryCollection) g;

                    if (mg.getNumGeometries() == 1) {
                        g = mg.getGeometryN(0);
                    }
                }

                geometryEncoding.encode(g, this);
            }
        }

        //overrides to increase visiblity
        public void start(String element) {
            super.start(element);
        }

        public void element(String element, String content) {
            super.element(element, content);
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            String string = new String(ch, start, length);
            chars(string);
        }
        
        public void endElement(String uri, String localName, String qName) throws SAXException {
            // working around a bug in the GML encoder, it won't properly setup the qName
            end("gml:" + localName);            
        }
        
        public void startElement(String uri, String localName, String qName, Attributes atts)
                throws SAXException {
            start(qName, atts);
        }


        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            if(getIndentation() > 0) {
                characters(ch, start, length);
            }
        }

        public void endDocument() throws SAXException {
            // nothing to do
        }
        
        public void endPrefixMapping(String prefix) throws SAXException {
            // nothing to do
        }

        public void processingInstruction(String target, String data) throws SAXException {
            // nothing do do
        }

        public void setDocumentLocator(Locator locator) {
            // nothing do do
        }

        public void skippedEntity(String name) throws SAXException {
            // nothing to do
            
        }

        public void startDocument() throws SAXException {
            // nothing to do
            
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            // nothing to do
        }
        
    }

}
