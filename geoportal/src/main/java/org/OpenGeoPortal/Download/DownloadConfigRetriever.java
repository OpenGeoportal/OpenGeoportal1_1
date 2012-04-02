package org.OpenGeoPortal.Download;

public interface DownloadConfigRetriever {
	String getClassKey(RequestedLayer layer) throws Exception;
}
