package org.OpenGeoPortal.Download;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.List;

/**
 * a class that implements PackagedDownloadMethod to request raster layers from HGL.  They are not downloaded locally.
 * Rather, an email is sent to the user containing a link to the requested layers.
 * 
 * @author chris
 *
 */
public class HGLRasterDownloadMethod implements PackagedDownloadMethod {
	private final Boolean metadata = true;
	private HttpRequester httpRequester;
	private List<RequestedLayer> layerList;

	
	public void setLayerList(List<RequestedLayer> layerList){
		this.layerList = layerList;
	}
	
	/**
	 * an HttpRequester is injected
	 * @param httpRequester
	 */
	public void setHttpRequester(HttpRequester httpRequester){
		this.httpRequester = httpRequester;
	}
	
	@Override
	public Boolean includesMetadata() {
		return this.metadata;
	}
	
	@Override
	public void validate(RequestedLayer currentLayer) throws Exception {
		if (currentLayer.emailAddress.isEmpty()){
			throw new Exception("A valid email address must be supplied.");
		}
	}
	
	public void setAllLayerDisposition(LayerDisposition disposition){
		for (RequestedLayer currentLayer: this.layerList){
			currentLayer.setDisposition(disposition);
		}
	}
	
	@Override
	public String createDownloadRequest() {
		RequestedLayer representativeLayer = this.layerList.get(0);
		try {
			this.validate(representativeLayer);
		} catch (Exception e){
			//die gracefully
			this.setAllLayerDisposition(LayerDisposition.REQUEST_ABORTED);
			return null;
		}
		BoundingBox bounds = representativeLayer.requestedBounds;
		String userEmail = representativeLayer.emailAddress;

		String layerQuery = "";
		for (RequestedLayer currentLayer: this.layerList){
			layerQuery += "LayerName=" + currentLayer.name + "&";
		}

		String getFeatureRequest = layerQuery 
		 + "UserEmail=" + userEmail + "&Clip=true&EmailAdmin=true&AppID=55&EncryptionKey=OPENGEOPORTALROCKS"
		 + "&XMin=" + bounds.getMinX() + "&YMin=" + bounds.getMinY() + "&XMax=" + bounds.getMaxX() + "&YMax=" + bounds.getMaxY();
		return getFeatureRequest;
	}

	@Override
	public InputStream getResponseStream(List<RequestedLayer> layerList)
			throws IOException {
		this.layerList = layerList;
		String request = this.createDownloadRequest();
		if (request.isEmpty() || request.equals(null)){
			throw new IOException("There is a problem with the query string.");
		}
		URLConnection responseConnection = this.httpRequester.sendRequest(layerList.get(0).getDownloadUrl(), request, "GET");
		for (RequestedLayer currentLayer: this.layerList){
			currentLayer.metadata = this.includesMetadata();
			currentLayer.setDisposition(LayerDisposition.LINK_EMAILED);
			System.out.println(currentLayer.getDisposition());
			currentLayer.responseMIMEType = responseConnection.getContentType();
		}
		return responseConnection.getInputStream();
	}

}
