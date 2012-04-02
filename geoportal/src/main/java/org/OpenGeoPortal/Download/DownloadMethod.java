package org.OpenGeoPortal.Download;

public interface DownloadMethod {

	void validate(RequestedLayer currentLayer) throws Exception;

	String createDownloadRequest() throws Exception;

	Boolean includesMetadata();
}
