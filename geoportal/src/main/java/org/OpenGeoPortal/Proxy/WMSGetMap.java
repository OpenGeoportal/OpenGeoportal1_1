package org.OpenGeoPortal.Proxy;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.HttpRequestHandler; 

public class WMSGetMap implements HttpRequestHandler {
	private GenericProxy genericProxy;

	public GenericProxy getGenericProxy() {
		return genericProxy;
	}

	public void setGenericProxy(GenericProxy genericProxy) {
		this.genericProxy = genericProxy;
	}

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
	    	String serverLocation = request.getParameter("server");
	    	//we should get this server side
	    	String layerNames = request.getParameter("layers");
	    	String srs = request.getParameter("srs");
	    	String bbox = request.getParameter("bbox");
	    	String height = request.getParameter("height");
	    	String width = request.getParameter("width");
	    	String format = request.getParameter("format");
	    	//String type = request.getParameter("type");
	    	String sld = request.getParameter("sld");
		
	        String urlFormat = URLEncoder.encode(format, "UTF-8");
	        //switch based on format to add dpi settings, change/add header info
	        String queryString;
	    	queryString = "service=wms&version=1.1.1&request=GetMap&format=" + urlFormat + "&SRS=" + srs;
	        queryString += "&styles=&bbox=" + bbox + "&layers=" + layerNames;
	    	if (format.equals("image/png")){
	    		/*int requestedDpi = 90;//dpi & size options?
	    		url$ += "&format_options=dpi:" + Integer.toString(requestedDpi) + ";";
	    		width = Integer.toString(Math.round(Integer.parseInt(width) * requestedDpi / 90));
	    		height = Integer.toString(Math.round(Integer.parseInt(height) * requestedDpi / 90));
	    		*/
	    		queryString += "&tiled=false&transparent=true";
	    	} 
	    	queryString += "&height=" + height + "&width=" + width;
	    	
	    	if ((sld != null)&&(!sld.equals("null"))){
	    		queryString += "&sld_body=" + URLEncoder.encode(sld, "UTF-8");
	    	}

	    	int j = serverLocation.indexOf("://");
	    	String remoteProtocol = serverLocation.substring(0, j);
	    	serverLocation = serverLocation.substring(j + 3);
	    	int k = serverLocation.indexOf("/");
	    	String remoteHostInfo = serverLocation.substring(0, k);
	    	String remotePath = serverLocation.substring(k);
	    	String remoteHost;
	    	int remotePort;
	    	if (remoteHostInfo.contains(":")){
	    		String[] remoteHostInfoArray = remoteHostInfo.split(":");
	    		remoteHost = remoteHostInfoArray[0];
	    		remotePort = Integer.parseInt(remoteHostInfoArray[1]);
	    	} else {
	    		remoteHost = remoteHostInfo;
	    		remotePort = 80;
	    	}
	    	
	    	String remoteUrl = remoteProtocol + "://" + remoteHost + ":" + remotePort + remotePath + "?" + queryString;
			System.out.println("In WMSGetMap, executing WMS request: " + remoteUrl);
			
			//force this to be an attachment
            layerNames = layerNames.replace(":", "-");
        	response.setHeader("Content-Disposition", "attachment; filename=" + layerNames + ".png");
        	
			this.genericProxy.proxyRequest(request, response, remoteUrl);

	}

}
