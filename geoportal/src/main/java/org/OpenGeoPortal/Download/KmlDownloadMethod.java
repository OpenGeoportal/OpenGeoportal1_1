package org.OpenGeoPortal.Download;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

public class KmlDownloadMethod implements UnpackagedDownloadMethod {
	private final Boolean metadata = false;
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
	public String createDownloadRequest() throws Exception {
		//--generate POST message
		//info needed: geometry column, bbox coords, epsg code, workspace & layername
	 	//all client bboxes should be passed as lat-lon coords.  we will need to get the appropriate epsg code for the layer
	 	//in order to return the file in original projection to the user (will also need to transform the bbox)
		BoundingBox bounds = this.getClipBounds();
		String layerName = this.currentLayer.getLayerNameNS();
		/* we're going to use the kml reflector
		 * http://localhost:8080/geoserver/wms/kml?layers=topp:states

kmscore=<value> 0 = force raster, 100 = force vector
kmattr=[true|false] : whether or not kml has clickable attributes
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
		String getFeatureRequest = "BBOX=" + bounds.toString() + "&LAYERS=" + layerName;
		if (!this.currentLayer.isVector()){
			getFeatureRequest += "&mode=superoverlay";
		}
    	return getFeatureRequest;
	}
	
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
