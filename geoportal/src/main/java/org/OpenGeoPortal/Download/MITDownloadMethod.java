package org.OpenGeoPortal.Download;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

public class MITDownloadMethod implements UnpackagedDownloadMethod {
	private final Boolean metadata = true;
	private HttpRequester httpRequester;
	private RequestedLayer currentLayer;
	
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
	public String createDownloadRequest() {
		BoundingBox bounds = this.getClipBounds();
		String layerName = this.getLayerName();
		String geometry = this.getDataType();
		
		String getFeatureRequest = "layer=" + layerName + "&bbox=" + bounds.toString() + "&geom=" + geometry;
		return getFeatureRequest;
	}
	
	private BoundingBox getClipBounds() {
		return this.currentLayer.nativeBounds.getIntersection(this.currentLayer.requestedBounds);
	}

	private String getDataType() {
		String geometry = this.currentLayer.dataType;
		if (geometry.equals("line")){
			geometry = "arc";
		}
		return geometry;
	}
	
	private String getLayerName() {
		String layerName = this.currentLayer.name;
		layerName = layerName.substring(layerName.indexOf(":") + 1);
		//temporary?  name is wrong in solr index
		layerName = layerName.toUpperCase();
		if (layerName.indexOf("SDE_DATA") < 0){
			layerName = "SDE_DATA." + layerName;
		}
		return layerName;
	}

	@Override
	public InputStream getResponseStream(RequestedLayer currentLayer) throws IOException {
		this.currentLayer = currentLayer;
		this.currentLayer.metadata = this.includesMetadata();
		URLConnection responseConnection = this.httpRequester.sendRequest(this.currentLayer.getDownloadUrl(), this.createDownloadRequest(), "GET");
		this.currentLayer.responseMIMEType = responseConnection.getContentType();

		return responseConnection.getInputStream();
	}
}
