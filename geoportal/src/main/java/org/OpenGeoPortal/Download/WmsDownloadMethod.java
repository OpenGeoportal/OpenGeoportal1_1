package org.OpenGeoPortal.Download;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

public class WmsDownloadMethod implements UnpackagedDownloadMethod {
	private final Boolean metadata = false;
	private HttpRequester httpRequester;
	private RequestedLayer currentLayer;
	private Double maxArea = 1800.0 * 1800.0;  //should be within recommended geoserver memory settings.
	
	public void setCurrentLayer(RequestedLayer currentLayer){
		this.currentLayer = currentLayer;
	}
	
	public void setHttpRequester(HttpRequester httpRequester){
		this.httpRequester = httpRequester;
	}
	
	@Override
	public Boolean includesMetadata() {
		return this.metadata;
	}
	
	@Override
	public void validate(RequestedLayer currentLayer) {
		// TODO Auto-generated method stub
	}

	@Override
	public String createDownloadRequest() throws Exception {
		//--generate POST message
		//info needed: geometry column, bbox coords, epsg code, workspace & layername
	 	//all client bboxes should be passed as lat-lon coords.  we will need to get the appropriate epsg code for the layer
	 	//in order to return the file in original projection to the user (will also need to transform the bbox)
		BoundingBox bounds = this.getClipBounds();
		String layerName = this.currentLayer.getLayerNameNS();
		//for now we'll force wgs84.  we'll revisit if we need something different
		int epsgCode = 4326;
		/*
geoserver/wms?VERSION=1.3.0&REQUEST=GetMap&CRS=epsg:4326&BBOX=-90,-180,90,180&...

The format_options is a container for parameters that are format specific. The options in it are expressed as:

param1:value1;param2:value2;...

The currently recognized format options are:

    antialiasing (on, off, text): allows to control the use of antialiased rendering in raster outputs.
    dpi: sets the rendering dpi in raster outputs. The OGC standard dpi is 90, but if you need to perform 
    high resolution printouts it is advised to grab a larger image and set a higher dpi. For example, to 
    print at 300dpi a 100x100 image it is advised to ask for a 333x333 image setting the dpi value at 300. 
    In general the image size should be increased by a factor equal to targetDpi/90 and the target dpi set 
    in the format options.
			*/
		//height and width should be calculated based on the bounds
		Map<String,String> requestDimensions = this.calculateDimensions(bounds.getAspectRatio());
		
		String format = this.currentLayer.requestedFormat;
		if (format.toLowerCase().equals("geotiff")){
			format = "image/geotiff";
		}
		String getFeatureRequest = "VERSION=1.1.1&REQUEST=GetMap&SRS=epsg:" +
				epsgCode + "&BBOX=" + bounds.toString() + "&LAYERS=" + layerName +
				"&HEIGHT=" + requestDimensions.get("height") + "&WIDTH=" + requestDimensions.get("width") +
				"&FORMAT=" + format;
		if (!format.equals("kmz")){
			getFeatureRequest += "&TILED=no";
		} else {
			if (this.currentLayer.isVector()){
				getFeatureRequest += "&kmscore=100";
			}
		}
    	return getFeatureRequest;
	}

	private Map<String, String> calculateDimensions(Double aspectRatio){
		String requestWidth;
		String requestHeight;
		/*String shortSide = Integer.toString((int) Math.round(this.multiplier));
		String longSide; 
		if (aspectRatio >= 1){
			longSide = Integer.toString((int) Math.round(this.multiplier * aspectRatio));
			requestWidth = longSide;
			requestHeight = shortSide;
		} else {
			longSide = Integer.toString((int) Math.round(this.multiplier * (1/aspectRatio)));
			requestHeight = longSide;
			requestWidth = shortSide;
		}*/
		Double heightNumber = Math.sqrt(this.maxArea / aspectRatio);
		requestHeight = Integer.toString((int) Math.round(heightNumber));
		requestWidth = Integer.toString((int) Math.round(maxArea/heightNumber));
		Map<String,String>requestDimensions = new HashMap<String,String>();
		requestDimensions.put("height", requestHeight);
		requestDimensions.put("width", requestWidth);
		return requestDimensions;
	};
	
	private BoundingBox getClipBounds() {
			return this.currentLayer.nativeBounds.getIntersection(this.currentLayer.requestedBounds);
	}

	@Override
	public InputStream getResponseStream(RequestedLayer currentLayer) throws IOException {
			this.currentLayer = currentLayer;
			this.currentLayer.metadata = this.includesMetadata();
			URLConnection responseConnection = null;
			try {
				responseConnection = this.httpRequester.sendRequest(this.currentLayer.getWmsUrl(), this.createDownloadRequest(), "GET");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.currentLayer.responseMIMEType = responseConnection.getContentType();
			return responseConnection.getInputStream();
		}
}
