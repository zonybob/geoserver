package org.geoserver.wps.density;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.raster.MultiplyCoveragesProcess;
import org.geotools.process.vector.VectorProcess;
import org.geotools.process.vector.VectorToRasterProcess;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.Envelope;
import org.opengis.util.ProgressListener;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@DescribeProcess(title = "intensityByTime", description = "Create a raster output binned by specified time ")
public class IntensityByTime implements VectorProcess {

	@DescribeResult(name = "result", description = "The rasterized, binned geometry")
	public GridCoverage2D execute(
			@DescribeParameter(name = "data", description = "Feature collection to process") SimpleFeatureCollection data,
			@DescribeParameter(name = "rasterWidth", description = "Width of the output grid in pixels") int rasterWidth,
			@DescribeParameter(name = "rasterHeight", description = "Height of the output grid in pixels") int rasterHeight,
			@DescribeParameter(name = "bounds", description = "Bounding box of the area to rasterize") Envelope bounds,
			@DescribeParameter(name = "timeField", description = "Name of the time field to be used to bin data") String timeField,
			@DescribeParameter(name = "binType", description = "Bin type to apply to data (h for hourly, d for daily, w for weekly, and m for monthly)") String binType,
			@DescribeParameter(name = "intensityField", description = "Name of the field that will be used") String intensityField) throws Exception 
	{
	    
	    // Compute peak intensity by binning data by selected times
	    binType = binType.toLowerCase();
	    
	    int count = 0;
	    
	    // store binned data in dictionary
	    Map<Date, ListFeatureCollection> binnedData = new HashMap<Date, ListFeatureCollection>();
	    
	    SimpleFeatureIterator iterator = data.features();
	    
	    while (iterator.hasNext())
	    {
	    	SimpleFeature feature = iterator.next();
	        if (count == 0){
	            // validate intensityField exists in data set
	            // if not this statement will raise KeyError
	        	try {
	        		feature.getAttribute(intensityField);
	        	}
	        	catch (Exception ex)
	        	{
	        		System.out.println(ex.getMessage());
	        	}
	        }
	        
	        Date featureTime = (Date) feature.getAttribute(timeField);
	        Date reducedTime = reduceDate(featureTime, binType);
	        
	        // ensure there is a layer key for the data bin
	        if (!binnedData.containsKey(reducedTime)) {
	        	
	        	binnedData.put(reducedTime, new ListFeatureCollection(data.getSchema()));
	        }
	            
	            
	        // shove feature into the appropriate bin
	        binnedData.get(reducedTime).add(feature);
	        
	        count++;
	        
	        if (count % 1000 == 0) {
	        	System.out.println("Processed " + count + " records");
	        }
	    }
	    
	    System.out.println("Created " + binnedData.size() + " bins of data");
	        
	    List<GridCoverage2D> rasters = new ArrayList<GridCoverage2D>();
	    // create raster's of binned data    
	    for (ListFeatureCollection collection : binnedData.values()){
	    	ProgressListener monitor = null;
	    	String coverageName = "IDontCare";
	        Dimension gridDim = new Dimension(rasterWidth, rasterHeight);
	        GridCoverage2D coverage = VectorToRasterProcess.process(
	        		collection, intensityField, gridDim, bounds, coverageName, monitor);
	        rasters.add(coverage);
	    	
	    }
	    
	    Iterator<GridCoverage2D> rasterIter = rasters.iterator();
	    GridCoverage2D currentRaster = rasterIter.next();
	    while (rasterIter.hasNext()) {
	    	GridCoverage2D processingRaster = rasterIter.next();
	    	currentRaster = new MultiplyCoveragesProcess().execute(currentRaster, processingRaster, null);
	    }
	        
	    System.out.print("Created "+ rasters.size() + " rasters from data");
		return currentRaster;
	}
	
	/**
	 * 
	 * @param dateTime
	 * @param binType
	 * @return
	 * @throws Exception
	 */
	private Date reduceDate(Date dateTime, String binType) throws Exception{
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(dateTime.getTime());
	    if (binType.compareToIgnoreCase("h") == 0) {
	    	cal.set(Calendar.MINUTE, 0);
	    	cal.set(Calendar.SECOND, 0);
	    	cal.set(Calendar.MILLISECOND, 0);
	    }
	    else if (binType.compareToIgnoreCase("d") == 0) {
	    	cal.set(Calendar.HOUR, 0);
	    	cal.set(Calendar.MINUTE, 0);
	    	cal.set(Calendar.SECOND, 0);
	    	cal.set(Calendar.MILLISECOND, 0);
	    }
	    else if (binType.compareToIgnoreCase("w") == 0) {
	    	cal.set(Calendar.WEEK_OF_YEAR, cal.getWeekYear());
	    	cal.set(Calendar.HOUR, 0);
	    	cal.set(Calendar.MINUTE, 0);
	    	cal.set(Calendar.SECOND, 0);
	    	cal.set(Calendar.MILLISECOND, 0);
	    }
	    else if (binType.compareToIgnoreCase("m") == 0) {
	    	cal.set(Calendar.DAY_OF_MONTH, 1);
	    	cal.set(Calendar.MINUTE, 0);
	    	cal.set(Calendar.SECOND, 0);
	    	cal.set(Calendar.MILLISECOND, 0);
	    }
	    else {
	        throw new Exception("Unexpected value '" + binType + "' for binType. Provide a value for parameter as specified in DescribeProcess.");
	    }
	    return cal.getTime();
	}
}