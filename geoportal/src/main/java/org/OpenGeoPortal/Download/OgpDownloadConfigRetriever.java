package org.OpenGeoPortal.Download;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;

public class OgpDownloadConfigRetriever extends ConfigRetriever implements DownloadConfigRetriever{
	public OgpDownloadConfigRetriever(String configFilePath){
		this.setConfigFilePath(configFilePath);
	}
	
	public String getClassKey(RequestedLayer layer) 
		throws Exception{
		//System.out.println(institution + " " + accessLevel.toLowerCase() + " " +  dataType.toLowerCase() + " " +  requestedFormat.toLowerCase());
		this.readConfigFile();
		JsonNode institutions = this.configContents.path("institutions");
		ArrayNode jsonArray = (ArrayNode) institutions.path(layer.institution);
		Iterator<JsonNode> institutionIterator = jsonArray.getElements();
		String classKey = null;
		while (institutionIterator.hasNext()){
			JsonNode currentNode = institutionIterator.next();
			List<String> accessArray = currentNode.findValuesAsText("accessLevel");
			if (!accessArray.contains(layer.accessLevel.toLowerCase())){
				continue;
			}
			List<String> dataTypeArray = currentNode.findValuesAsText("dataType");
			String generalizedDataType = layer.dataType.toLowerCase();

			if (generalizedDataType.equals("point")||generalizedDataType.equals("line")||generalizedDataType.equals("polygon")){
				generalizedDataType = "vector";
			}
			if (!dataTypeArray.contains(generalizedDataType)){
				continue;
			}
			List<String> outputFormatArray = currentNode.findValuesAsText("outputFormats");
			if (!outputFormatArray.contains(layer.requestedFormat.toLowerCase())){
				continue;
			}
			classKey = currentNode.path("classKey").getTextValue();

		}
		/*int numKeys = jsonArray.size();
		for(int i = 0; i < numKeys; i++){
			JSONObject currentResult = (JSONObject) jsonArray.get(i);
			JSONArray accessLevelArray = (JSONArray) currentResult.get("accessLevel");
			if (!accessLevelArray.contains(layer.accessLevel.toLowerCase())){
				continue;
			}
			JSONArray dataTypeArray = (JSONArray) currentResult.get("dataType");
			String generalizedDataType = layer.dataType.toLowerCase();

			if (generalizedDataType.equals("point")||generalizedDataType.equals("line")||generalizedDataType.equals("polygon")){
				generalizedDataType = "vector";
			}
			//System.out.println(generalizedDataType);
			if (!dataTypeArray.contains(generalizedDataType)){
				continue;
			}
			JSONArray formatArray = (JSONArray) currentResult.get("outputFormats");

			if (!formatArray.contains(layer.requestedFormat.toLowerCase())){
				continue;
			}
			classKey = currentResult.get("classKey").toString();
			//System.out.println("classkey=" + classKey);
			
		}*/
		if (classKey == null){
			throw new Exception("Class Key not defined for this layer.");
		}
		return classKey;
	}
	
	/*public String getServiceURL(RequestedLayer layer) throws Exception {
		this.readConfigFile();
		JSONObject institutions = (JSONObject) this.configContents.get("institutions");
		JSONArray jsonArray = (JSONArray) institutions.get(layer.institution);
		int numKeys = jsonArray.size();
		String url = null;
		for(int i = 0; i < numKeys; i++){
			JSONObject currentResult = (JSONObject) jsonArray.get(i);
			JSONArray accessLevelArray = (JSONArray) currentResult.get("accessLevel");
			if (!accessLevelArray.contains(layer.accessLevel.toLowerCase())){
				continue;
			}
			JSONArray dataTypeArray = (JSONArray) currentResult.get("dataType");
			String generalizedDataType = layer.dataType.toLowerCase();

			if (generalizedDataType.equals("point")||generalizedDataType.equals("line")||generalizedDataType.equals("polygon")){
				generalizedDataType = "vector";
			}
			if (!dataTypeArray.contains(generalizedDataType)){
				continue;
			}
			JSONArray formatArray = (JSONArray) currentResult.get("outputFormats");

			if (!formatArray.contains(layer.requestedFormat.toLowerCase())){
				continue;
			}
			JSONObject paramsObject = (JSONObject) currentResult.get("params");
			url = paramsObject.get("serviceAddress").toString();

		}
		if (url == null){
			throw new Exception("No serviceAddress specified for this layer.");
		}
		return url;
		
	}*/
	
	public String getRelativePath() throws IOException{
		this.readConfigFile();
		String directory = this.configContents.path("relativePath").getTextValue();
		return directory;
		//return "../webapps/ROOT/download";
	}

}
