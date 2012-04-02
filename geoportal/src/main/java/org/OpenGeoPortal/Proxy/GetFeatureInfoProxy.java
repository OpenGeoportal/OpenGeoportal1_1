package org.OpenGeoPortal.Proxy;


import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.OpenGeoPortal.Download.LayerInfoRetriever;
import org.OpenGeoPortal.Utilities.ParseJSONSolrLocationField;

import org.springframework.web.HttpRequestHandler;

public class GetFeatureInfoProxy implements HttpRequestHandler {
	private GenericProxy genericProxy;
	private LayerInfoRetriever layerInfoRetriever;
	int numberOfFeatures = 1;

	/**
	 * 
	 */

	public int getNumberOfFeatures() {
		return numberOfFeatures;
	}

	public void setNumberOfFeatures(int numberOfFeatures) {
		this.numberOfFeatures = numberOfFeatures;
	}
	
	public void setGenericProxy(GenericProxy genericProxy){
		this.genericProxy = genericProxy;
	}
	
	public GenericProxy getGenericProxy(){
		return this.genericProxy;
	}
	
	public void setLayerInfoRetriever(LayerInfoRetriever layerInfoRetriever){
		this.layerInfoRetriever = layerInfoRetriever;
	}
	
	public LayerInfoRetriever getLayerInfoRetriever(){
		return this.layerInfoRetriever;
	}
	
	@Override
	public void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
	    String layerId = request.getParameter("OGPID");
	    String xCoord = request.getParameter("x");
	    String yCoord = request.getParameter("y");
	    String bbox = request.getParameter("bbox");
	    String height = request.getParameter("height");
	    String width = request.getParameter("width");
	    String format = "application/vnd.ogc.gml";
		//format = URLEncoder.encode(format, "UTF-8");

	    Set<String> layerIds = new HashSet<String>();
	    layerIds.add(layerId);
	    Map<String,String> layerInfo = null;
	    try {
			layerInfo = this.layerInfoRetriever.getAllLayerInfo(layerIds).get(layerId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			response.sendError(500);
		}
	    String previewUrl = null;
	    try {
	    	//is there a proxy?
			previewUrl = this.layerInfoRetriever.getWMSUrl(layerInfo);
			//use below, once we used cached credentials
	    	//previewUrl = ParseJSONSolrLocationField.getWmsUrl(layerInfo.get("Location"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			response.sendError(500);
		}
	    String layerName = layerInfo.get("WorkspaceName") + ":" + layerInfo.get("Name");
	    String remoteUrl = previewUrl + "?service=wms&version=1.1.1&request=GetFeatureInfo&info_format=" + format 
				+ "&SRS=EPSG:900913&feature_count=" + this.getNumberOfFeatures() + "&styles=&height=" + height + "&width=" + width +"&bbox=" + bbox 
				+ "&x=" + xCoord + "&y=" + yCoord +"&query_layers=" + layerName + "&layers=" + layerName;
		System.out.println("executing WMS getFeatureRequest: " + remoteUrl);
		if (!previewUrl.contains("http")){
			//this has to change
			request.getRequestDispatcher(remoteUrl).forward(request, response);
		} else {
			//response.getOutputStream().println(remoteUrl);
			this.genericProxy.proxyRequest(request, response, remoteUrl);

		}
	}
}