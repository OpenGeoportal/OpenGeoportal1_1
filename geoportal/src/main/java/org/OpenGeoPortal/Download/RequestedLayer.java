package org.OpenGeoPortal.Download;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.OpenGeoPortal.Utilities.ParseJSONSolrLocationField;

public class RequestedLayer {
	String name;
	String requestedFormat;
	final String id;

	String institution;
	String accessLevel;
	String dataType;
	String title;
	String emailAddress = "";
	private LayerStatus status;
	private LayerDisposition disposition;
	File targetDirectory;
	String workSpace;
	BoundingBox requestedBounds;
	BoundingBox nativeBounds;
	String epsgCode;
	public Set<File> downloadedFiles = new HashSet<File>();
	public String responseMIMEType;
	public Map<String, List<String>> responseHeaders;
	public Boolean metadata;
	public String serviceURLs;
	
	public RequestedLayer(String id, String requestedFormat){
		this.id = id;
		//probably best to make all values with a limited set of possibilities enums
		this.setRequestedFormat(requestedFormat);
		this.status = LayerStatus.AWAITING_REQUEST;
		this.disposition = LayerDisposition.AWAITING_REQUEST;
	}
	
	private void setRequestedFormat(String requestedFormat){
		requestedFormat = requestedFormat.toLowerCase().trim();
		if (requestedFormat.equals("kml")){
			requestedFormat = "kmz";
		}
		this.requestedFormat = requestedFormat;
	}
	public Boolean isVector(){
		Set <String> vectorTypes = new HashSet<String>();
		vectorTypes.add("point");
		vectorTypes.add("line");
		vectorTypes.add("polygon");
		if (vectorTypes.contains(this.dataType.toLowerCase())){
			return true;
		} else {
			return false;
		}
	}
	
	public void setStatus(LayerStatus status){
		this.status = status;
	}
	
	public LayerStatus getStatus(){
		return this.status;
	}
	
	public String getLayerNameNS(){
		return this.workSpace + ":" + this.name;
	}

	public void setDisposition(LayerDisposition disposition) {
		this.disposition = disposition;
	}
	
	public LayerDisposition getDisposition(){
		return this.disposition;
	}

	public String getWmsUrl(){
		return ParseJSONSolrLocationField.getWmsUrl(this.serviceURLs);
	}
	
	public String getWfsUrl(){
		return ParseJSONSolrLocationField.getWfsUrl(this.serviceURLs);
	}
	
	public String getWcsUrl(){
		return ParseJSONSolrLocationField.getWcsUrl(this.serviceURLs);
	}
	
	public String getDownloadUrl(){
		return ParseJSONSolrLocationField.getDownloadUrl(this.serviceURLs);
	}
	

}
