package org.OpenGeoPortal.Download;

import java.util.Map;

public interface DownloadHandler {
	public boolean requestLayers(Map<String,String> layerMap, String[] bounds);
	public String getJsonResponse();
	public void setLocallyAuthenticated(Boolean authenticated);
	public void setReplyEmail(String emailAddress);
}
