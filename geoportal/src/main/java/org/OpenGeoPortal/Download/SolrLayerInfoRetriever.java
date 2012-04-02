package org.OpenGeoPortal.Download;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.OpenGeoPortal.Utilities.ParseJSONSolrLocationField;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;

public class SolrLayerInfoRetriever implements LayerInfoRetriever{
	private String solrUrl;
	private Set<String> layerIds;
	private String solrQuery;
	private String resultString;
	public SearchConfigRetriever searchConfigRetriever;
	
	public void setSearchConfigRetriever(SearchConfigRetriever searchConfigRetriever) throws Exception{
		this.searchConfigRetriever = searchConfigRetriever;
		this.solrUrl = this.searchConfigRetriever.getSearchUrl();
	}
	
	public Map<String, Map<String,String>> getAllLayerInfo(Set<String> layerIds) throws Exception{
		this.layerIds = layerIds;
		this.solrQueryBuilder("*");
		this.solrHandler();
		return this.solrJsonParser();
	}
	
	public Map<String,Map<String,String>> getInfo(Map<String,String> conditions, List<String> requestedFields, Boolean isAnd) 
			throws IOException{
		Iterator<String> conditionsIterator = conditions.keySet().iterator();
		String conditionsClause = "q=";
		int offset = 0;
		while(conditionsIterator.hasNext()){
			String currentCondition = conditionsIterator.next();
			conditionsClause += currentCondition;
			conditionsClause += ":";
			conditionsClause += conditions.get(currentCondition);
			if (isAnd){
				conditionsClause += "+AND+";
				offset = 5;
			} else {
				conditionsClause += "+OR+";
				offset = 4;
			}
		}
		
		conditionsClause = conditionsClause.substring(0, conditionsClause.length() - offset);

		Iterator<String> fieldsIterator = requestedFields.iterator();
		String fieldsClause = "fl=LayerId,";
		while(fieldsIterator.hasNext()){
			String currentField = fieldsIterator.next();
			fieldsClause += currentField;
			fieldsClause += ",";
		}
		fieldsClause = fieldsClause.substring(0, fieldsClause.length() - 1);
		if (!this.solrUrl.startsWith("http://")){
			this.solrUrl = "http://" + this.solrUrl;
		}
	 	this.solrQuery = this.solrUrl + "?" + conditionsClause + "&" + fieldsClause 
	 				  + "&wt=json";
	 	this.solrHandler();
		return this.solrJsonParser();
	}
	
	public void solrQueryBuilder(String returnedColumns){

	 	String layerIdsString = "";
	 	int j = this.layerIds.size();
	 	String[] arrLayerIds = this.layerIds.toArray(new String[0]);
		for(int i = 0; i < j - 1; i++){
			layerIdsString += "LayerId:" + arrLayerIds[i] + "+OR+";
		}
		layerIdsString += "LayerId:" + arrLayerIds[j - 1];
		//String returnedColumns = "LayerId,FgdcText,Name,Institution,WorkspaceName,Access,DataType,MaxX,MaxY,MinX,MaxX,SrsProjectionCode";
		String queryString = "q=" + layerIdsString + "&fl=" + returnedColumns;
		if (!this.solrUrl.startsWith("http://")){
			this.solrUrl = "http://" + this.solrUrl;
		}
	 	this.solrQuery = this.solrUrl + "?" + queryString 
	 				  + "&wt=json";
	}
	
	public void solrHandler() throws IOException {
		URL url = new URL(this.solrQuery);
		InputStream inputStream = url.openStream();
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		String currentLine = bufferedReader.readLine();
		String resultJson = "";
		while (currentLine != null)
			{
			//currentLine = currentLine.replaceAll("\\n|\\r", " ");
				resultJson += currentLine;	
				currentLine = bufferedReader.readLine();
			}
		//System.out.println(resultJson);
		this.resultString = resultJson;
	}
	
	private Map<String,String> parseSolrDoc(JsonNode solrDoc){
		Map<String,String> solrDocMap = new HashMap<String,String>();
		Iterator<String> fieldNameNodes = solrDoc.getFieldNames();
		while (fieldNameNodes.hasNext()){
			String currentField = fieldNameNodes.next();
			String currentFieldValue = solrDoc.path(currentField).getTextValue();
			solrDocMap.put(currentField, currentFieldValue);
		}
		return solrDocMap;
	}
	
	public Map<String, Map<String,String>> solrJsonParser() throws JsonParseException, JsonMappingException, IOException{
		//use jakcson to parse the results and place into a hashmap 
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readValue(this.resultString, JsonNode.class);
		JsonNode jsonResponse = rootNode.path("response");
		//int numFound = jsonResponse.path("numFound").asInt();
		ArrayNode docs = (ArrayNode) jsonResponse.path("docs");

		Map<String,Map<String,String>> layerInfoMap = new HashMap<String, Map<String, String>>();
		Iterator<JsonNode> docsIterator = docs.getElements();
		while(docsIterator.hasNext()){
			JsonNode currentDoc = docsIterator.next();
			Map<String,String> innerLayerInfoMap = this.parseSolrDoc(currentDoc);
			String currentKey = innerLayerInfoMap.get("LayerId");
			layerInfoMap.put(currentKey, innerLayerInfoMap);
		}
		/*for(int i = 0; i < numFound; i++){
			JSONObject currentResult = (JSONObject) docs.get(i);
			JSONArray currentKeyArray = (JSONArray) currentResult.get("LayerId");
			String currentKey = currentKeyArray.get(0).toString();
			Map<String,String> innerLayerInfoMap = new HashMap<String,String>();
			Iterator<String> currentResultIterator = currentResult.keySet().iterator();
			while (currentResultIterator.hasNext()){
				String currentFieldKey = currentResultIterator.next();			
				String currentFieldValue;
				if (currentResult.get(currentFieldKey) instanceof Double){
					currentFieldValue = Double.toString((Double) currentResult.get(currentFieldKey));
				} else if (currentResult.get(currentFieldKey) instanceof JSONArray) {
					currentFieldValue = currentResult.get(currentFieldKey).toString();
				} else if (currentResult.get(currentFieldKey) instanceof Boolean) {
					currentFieldValue = Boolean.toString((Boolean)currentResult.get(currentFieldKey));
				} else if (currentResult.get(currentFieldKey) instanceof String) {
					currentFieldValue = (String) currentResult.get(currentFieldKey);
				} else {
					throw new ParseException(1,"JSON value is not a recognized type");
				}
				innerLayerInfoMap.put(currentFieldKey, currentFieldValue);
			}
		}*/
		return layerInfoMap;
	}

	@Override
	public String getWMSUrl(Map<String, String> layerInfo) {
		String institution = layerInfo.get("Institution");
		String accessLevel = layerInfo.get("Access");

		String wmsProxyUrl = null;
		try {
			wmsProxyUrl = this.searchConfigRetriever.getWmsProxy(institution, accessLevel);
			//System.out.println("getwmsproxy" + wmsProxyUrl + institution + accessLevel);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (wmsProxyUrl != null){
			return wmsProxyUrl;
		} else {
			return ParseJSONSolrLocationField.getWmsUrl(layerInfo.get("Location"));
		}
	}

	@Override
	public boolean hasProxy(Map<String, String> layerInfo) {
		String institution = layerInfo.get("Institution");
		String accessLevel = layerInfo.get("Access");

		String wmsProxyUrl = null;
		try {
			wmsProxyUrl = this.searchConfigRetriever.getWmsProxy(institution, accessLevel);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (wmsProxyUrl != null){
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String getLayerInfo(String layerId, String field) throws IOException {
		Set<String> layerIdContainer = new HashSet<String>();
		layerIdContainer.add(layerId);
		this.layerIds = layerIdContainer;
		this.solrQueryBuilder("LayerId," + field);
		this.solrHandler();
		String requestedInfo = this.solrJsonParser().get(layerId).get(field);
		//System.out.println("requestedinfo: " + requestedInfo);
		return requestedInfo;
	}
	
	@Override
	public Map<String, String> getAllLayerInfo(String layerId) throws IOException {
		Set<String> layerIdContainer = new HashSet<String>();
		layerIdContainer.add(layerId);
		this.layerIds = layerIdContainer;
		this.solrQueryBuilder("*");
		this.solrHandler();
		return this.solrJsonParser().get(layerId);
	}
}
