package gov.ic.wpafb.giat;

import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class TimeRequestFilter implements Filter {


	
	/**
	 * Provided default constructor.
	 */
	public TimeRequestFilter() {

	}

	public void destroy() {
		System.err.println("Destroyed TimeRequestFilter filter...");
	}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {
		System.out
				.println("Initializing 'filter' override *Servlet Filter* (TimeRequestFilter)...");
	}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		chain.doFilter(new WrappedRequest(request), response);
	}

	static class WrappedRequest extends HttpServletRequestWrapper {
		/* 
		 Pattern to find any instance of the @BACK replacement parameter
		 The following are parameters that will be matched:

		 @BACK1H
		 @BACK25H
		 @BACK1999H
		 @BACK2D
		 @BACK2W
		*/ 
		Pattern pattern = Pattern.compile("@BACK\\d+[HDW]");
		
		/**
		 * Wrap the request, to bypass its immutability.
		 * 
		 * @param request
		 *            The original ServletRequest.
		 * @param config
		 *            The configuration for this filter.
		 */
		public WrappedRequest(ServletRequest request) {
			super((HttpServletRequest) request);
		}

		/**
		 * Build a new value based on the existing value and the entry from our
		 * configuration, whether an original or one from the configuration is
		 * available.
		 * 
		 * @param originalFilter
		 *            The original CQL filter String.
		 */
		private String getModifiedFilter(final String originalFilter) {
			String finalFilter = originalFilter;
			
			SimpleDateFormat sdf = new SimpleDateFormat(
					"yyyy-MM-dd'T'HH:mm:ss'Z'");
			
			// For every match of our @NOW replace with current time
			if (finalFilter.contains("@NOW")) {

				Date now = new Date(System.currentTimeMillis());
				finalFilter = finalFilter.replaceAll("@NOW", sdf.format(now));
			}
			Matcher m = pattern.matcher(finalFilter);
			
			// For every match of our @BACK regex replace with specified time back interval
			while (m.find()) {
				Calendar today = Calendar.getInstance();
				
				String filter = m.group();
				String timeBack = filter.substring(5);
				String interval = timeBack.substring(timeBack.length() - 1);
				int duration = Integer.parseInt(timeBack.substring(0, timeBack.length() - 1));

				// Apply the appropriate duration based on interval specified
				if (interval.equalsIgnoreCase("H")) {
					today.add(Calendar.HOUR, -1 * duration);
				}
				else if (interval.equalsIgnoreCase("D")){
					today.add(Calendar.DAY_OF_YEAR, -1 * duration);
				}
				else if (interval.equalsIgnoreCase("W")) {
					today.add(Calendar.DAY_OF_YEAR, -7 * duration);
				}
				
				finalFilter = finalFilter.replaceAll(filter, sdf.format(today.getTime()));
			}
			
			return finalFilter;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * javax.servlet.ServletRequestWrapper#getParameter(java.lang.String)
		 */
		@Override
		public String getParameter(String paramName) {
			if (paramName.toLowerCase().equals("cql_filter") || paramName.toLowerCase().equals("time")) {
				// Modified the filter param to add our param
				return getModifiedFilter(super.getParameter(paramName));
			}
			return super.getParameter(paramName);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * javax.servlet.ServletRequestWrapper#getParameterValues(java.lang.
		 * String)
		 */
		@Override
		public String[] getParameterValues(String paramName) {
			String[] values = super.getParameterValues(paramName);
			if (paramName.toLowerCase().equals("cql_filter") || paramName.toLowerCase().equals("time")) {
				for (int i = 0; i < values.length; i++) {
					values[i] = getModifiedFilter(values[i]);
				}
				return values;
			}
			return super.getParameterValues(paramName);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.servlet.ServletRequestWrapper#getParameterMap()
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Map getParameterMap() {
			Map<String, String> newMap = new HashMap<String, String>();
			Enumeration<String> en = super.getParameterNames();
			while (en.hasMoreElements()) {
				String mapKey = en.nextElement();
				String mapValue = super.getParameter(mapKey);
				if (mapKey.equalsIgnoreCase("cql_filter")) {
					mapValue = getModifiedFilter(mapValue);
				}
				else if (mapKey.equalsIgnoreCase("time")) {
					mapValue = getModifiedFilter(mapValue);
				}
				newMap.put(mapKey, mapValue);
			}
			return newMap;
		}
	}
}