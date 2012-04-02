package org.OpenGeoPortal.Download;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LayerInfoRetriever {
	Map<String, Map<String,String>> getAllLayerInfo(Set<String> layerIds) throws Exception;
	
	Map<String, String> getAllLayerInfo(String layerId) throws Exception;
	
	public Map<String, Map<String,String>> getInfo(Map<String,String> conditions, List<String> requestedFields, Boolean isAnd) throws IOException;

	String getWMSUrl(Map<String,String> layerInfo);

	boolean hasProxy(Map<String, String> layerInfo);

	String getLayerInfo(String layerId, String string) throws Exception;
}
