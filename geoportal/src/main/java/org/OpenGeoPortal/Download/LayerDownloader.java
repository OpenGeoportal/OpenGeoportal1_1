package org.OpenGeoPortal.Download;

import java.util.List;

public interface LayerDownloader {
	public void downloadLayers(List<RequestedLayer> list) throws Exception;

}
