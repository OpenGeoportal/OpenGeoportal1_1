package org.OpenGeoPortal.Export.GeoCommons;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.OpenGeoPortal.Download.LayerInfoRetriever;
import org.OpenGeoPortal.Download.MetadataRetriever;
import org.OpenGeoPortal.Utilities.ParseJSONSolrLocationField;

import org.apache.http.HttpHost;
import org.apache.http.NoHttpResponseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

public class GeoCommonsJsonClient implements GeoCommonsClient {
	    private RestTemplate restTemplate;
	    private Credentials credentials;
	    String serverName = "http://geocommons.com";
	    //String workspace;
	    private String layerName;
		//private URI dataSetUri;
		LayerInfoRetriever layerInfoRetriever;
		MetadataRetriever metadataRetriever;
		private Set<String> allTags;
		
	    public LayerInfoRetriever getLayerInfoRetriever() {
			return layerInfoRetriever;
		}

		public void setLayerInfoRetriever(LayerInfoRetriever layerInfoRetriever) {
			this.layerInfoRetriever = layerInfoRetriever;
		}

		public MetadataRetriever getMetadataRetriever() {
			return metadataRetriever;
		}

		public void setMetadataRetriever(MetadataRetriever metadataRetriever) {
			this.metadataRetriever = metadataRetriever;
		}

		public void initializeClient(String username, String password) {
	    	this.layerName = null;
	    	DefaultHttpClient httpclient = new DefaultHttpClient();
	    	HttpHost targetHost = new HttpHost(this.serverName, 80, "http");
	        this.credentials = new UsernamePasswordCredentials(username, password);

			httpclient.getCredentialsProvider().setCredentials(
	    	        new AuthScope(AuthScope.ANY), 
	    	        this.credentials);
	    	
			// Create AuthCache instance
			AuthCache authCache = new BasicAuthCache();
			// Generate BASIC scheme object and add it to the local auth cache
			BasicScheme basicAuth = new BasicScheme();
			authCache.put(targetHost, basicAuth);

			// Add AuthCache to the execution context
			BasicHttpContext localcontext = new BasicHttpContext();
			localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);

			HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpclient);
			
	        this.restTemplate = new RestTemplate(factory);
	        
	        this.allTags = new HashSet<String>();
	    }
	    
		public String getExistingLayer(String layerName){
			SearchResponseJson searchResponse = this.searchForLayer(layerName);
			if (searchResponse.getTotalResults() == 1){
				return searchResponse.getEntries().get(0).getId();
			} else {
				return new String();
			}
		}
		
	    public String uploadKmlDataSet(String layerId) throws Exception{
	    	String url = this.serverName + "/datasets.json";
	    	CreateDataSetRequestJson createDataSetRequestJson = this.createDataSetRequestObject(layerId);
	    	String existingLayer = this.getExistingLayer(this.layerName);
	    	if (!existingLayer.isEmpty()){
	    		System.out.println("getting existing layer");
	    		return existingLayer;
	    	}
	    	
	    	String resultString = null;
	    	try {
	    		URI result = restTemplate.postForLocation(url, createDataSetRequestJson);
	    		//this.dataSetUri = result;
	    		resultString = result.toString();
	    	} catch (HttpClientErrorException e){
	    	//String result = restTemplate.postForObject(url, addLayerToMapRequestJson, String.class);
	    		//["Your file size has exceeded the KML file size limit. Please keep your file size under twenty (20) megabytes.", "RecordInvalid"]
	    		e.getMessage();
	    		System.out.println(e.getResponseBodyAsString());
	    		throw new Exception(e.getResponseBodyAsString());
	    	} catch (ResourceAccessException e){
	    		
	    		System.out.println("GeoCommons server is not responding.");
	    		throw new Exception("GeoCommons server is not responding.");
	    	} catch (Exception e){
	    		e.getMessage();
	    		e.getStackTrace();
	    	}
	    	return resultString;
	    }
	    
	    @Override
	    public String createMap(String basemap, String extent, String title, String description) throws Exception{
	    	String url = this.serverName + "/maps.json";
	    	CreateMapRequestJson createMapRequestJson = this.createMapRequestObject(basemap, extent, title, description);
	    	try {
	    	CreateMapResponseJson result = restTemplate.postForObject(url, createMapRequestJson, CreateMapResponseJson.class);
	    	return result.getId();

	    	} catch (HttpClientErrorException e){
	    		//{"error": "an unknown error occurred"}
	    		throw new Exception (e.getResponseBodyAsString());
	    	}
	    }
	    
	    public SearchResponseJson searchForLayer(String layerName){
	    	String url = this.serverName + "/search.json?query=" + layerName;
	    	SearchResponseJson result = restTemplate.getForObject(url, SearchResponseJson.class);
	    	return result;
	    }
	    
	    public void addLayerToMap(String layerId, String layerTitle, String mapId) throws Exception{
	    	String url = this.serverName + "/maps/" 
	    			+ mapId +"/layers.json";
	    	AddLayerToMapRequestJson addLayerToMapRequestJson = new AddLayerToMapRequestJson();
	    	addLayerToMapRequestJson.setSource("finder:" + layerId);
	    	addLayerToMapRequestJson.setVisible(true);
	    	//System.out.println(layerId);
	    	addLayerToMapRequestJson.setTitle(layerTitle);
	    	//System.out.println(url);
	    	try {
	    	ResponseEntity<String> result = restTemplate.postForEntity(url, addLayerToMapRequestJson, String.class);
	    	System.out.println(result.getBody());
	    	System.out.println(result.getStatusCode());
	    	System.out.println(result.getHeaders().getLocation());

	    	} catch (HttpClientErrorException e){
	    	//String result = restTemplate.postForObject(url, addLayerToMapRequestJson, String.class);
	    		e.getMessage();
	    		System.out.println(e.getResponseBodyAsString());
	    		throw new Exception(e.getResponseBodyAsString());
	    	}
	    }
	    
		private CreateMapRequestJson createMapRequestObject(String basemap, String extent, String title, String description) {
			CreateMapRequestJson createMapRequestJson = new CreateMapRequestJson();

				createMapRequestJson.setBasemap(cleanString(basemap));
				String[] extentArray = extent.split(",");
				createMapRequestJson.setExtent(extentArray);
				//extent should be calculated based on layers in map
			createMapRequestJson.setTags(cleanString(this.getTagString(), 500));//tags should be collated from layers in map
			//System.out.println(this.getTagString());
			createMapRequestJson.setTitle(cleanString(title));
			createMapRequestJson.setDescription(cleanString(description));//aggregate layer titles

			return createMapRequestJson;
		}

		private String getTagString() {
			String tagString = "";
			for (String tag : this.allTags){
				tagString += tag + " ";
			}
			return tagString.trim();
		}

		private String cleanString(String inputString, int maxLength){
			System.out.println(inputString);
			inputString = inputString.trim();
			//inputString = inputString.replace("\"", "\\\"");
			if (inputString.length() > maxLength){
				inputString = inputString.substring(0, maxLength);
			}
			System.out.println(inputString);

			return inputString;
		}
		
		private String cleanString(String inputString){
			System.out.println(inputString);

			inputString = inputString.trim();
			//inputString = inputString.replace("\"", "\\\"");
			System.out.println(inputString);

			return inputString;
		}
		
		private CreateDataSetRequestJson createDataSetRequestObject(String layerId) {
	    	CreateDataSetRequestJson createDataSetRequestJson = new CreateDataSetRequestJson();

				createDataSetRequestJson.setType("kml");

				Map<String, String> requestedLayerInfo = null;
				try {
					requestedLayerInfo = layerInfoRetriever.getAllLayerInfo(layerId);
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Error getting layer info.");
				//return null;
				}
				if (!requestedLayerInfo.get("Access").equalsIgnoreCase("public")){
					//throw new SecurityException();
				}
				//needs to be changed to kml service point, format
				String minXString = null;
				String maxXString = null;
				String minYString = null;
				String maxYString = null;
				try {
				double minX = Double.parseDouble(requestedLayerInfo.get("MinX"));
				double maxX = Double.parseDouble(requestedLayerInfo.get("MaxX"));
				double minY = Double.parseDouble(requestedLayerInfo.get("MinY"));
				double maxY = Double.parseDouble(requestedLayerInfo.get("MaxY"));

				if (minX > maxX){
					//this is supposed to mean that the layer crosses the dateline.  this causes problems with kml & geoserver,
					//so we give the full extent
					minXString = "-180";
					maxXString = "180";		
				} else {
					minXString = Double.toString(minX);
					maxXString = Double.toString(maxX);
				}
				

				if (minY > maxY){
					minYString = Double.toString(maxY);
					maxYString = Double.toString(minY);		
				} else {
					minYString = Double.toString(minY);
					maxYString = Double.toString(maxY);
				}
				} catch (Exception e){
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
				String bounds = cleanString(minXString) + "," + cleanString(minYString) + ",";
				bounds += cleanString(maxXString) + "," + cleanString(maxYString);
				String workspaceName = cleanString(requestedLayerInfo.get("WorkspaceName"));
				String layerName = cleanString(requestedLayerInfo.get("Name"));
				String SRS = "EPSG:4326";
	    	//http://geoserver01.uit.tufts.edu/wms?LAYERS=sde:GISPORTAL.GISOWNER01.CHELSEACULVERTSDITCHES05&request=getmap&format=kml&bbox=-71.052205,42.385485,-71.0138,42.41027&srs=EPSG:4326&width=1&height=1
				String kmlUrl = ParseJSONSolrLocationField.getWmsUrl(requestedLayerInfo.get("Location"))
	    		+ "?layers=" + workspaceName + ":" + layerName + "&request=getMap&format=kml&bbox="
	    		+ bounds + "&srs=" + SRS + "&width=1&height=1";
				this.layerName = workspaceName + ":" + layerName;
				//height and width don't seem to matter; should test with a raster layer
				System.out.println(kmlUrl);
				createDataSetRequestJson.setUrl(kmlUrl);
				String layerTitle = cleanString(requestedLayerInfo.get("LayerDisplayName"));
				createDataSetRequestJson.setTitle(layerTitle);
				
				String layerOriginator = cleanString(requestedLayerInfo.get("Originator"));
				createDataSetRequestJson.setAuthor(layerOriginator);
				
				String layerAbstract = cleanString(requestedLayerInfo.get("Abstract"), 1950);
				//layerAbstract = "test data";
				createDataSetRequestJson.setDescription(layerAbstract);

				String keywords = cleanString(requestedLayerInfo.get("ThemeKeywords")) + " " + cleanString(requestedLayerInfo.get("PlaceKeywords"));
				String[] keywordArray = keywords.split(" ");

				for (String keywordElement : keywordArray){
					//System.out.println(keywordElement);
					try {
						this.allTags.add(keywordElement);
					} catch (Exception e){
					}
				}

				createDataSetRequestJson.setTags(keywords);
				//where can I get this url from?
				createDataSetRequestJson.setMetadata_url("http://geodata.tufts.edu/getMetadata?id=" + layerId);
				try {
					createDataSetRequestJson.setContact_name(cleanString(this.metadataRetriever.getContactName(layerId)));
				} catch (Exception e){
					createDataSetRequestJson.setContact_name("open geo portal");
				}
				try {
					createDataSetRequestJson.setContact_address(cleanString(this.metadataRetriever.getContactAddress(layerId)));
				} catch (Exception e) {
					createDataSetRequestJson.setContact_address("replace with generic address");
				}
				try {
					createDataSetRequestJson.setContact_phone(cleanString(this.metadataRetriever.getContactPhoneNumber(layerId)));
				} catch (Exception e){
					createDataSetRequestJson.setContact_phone("replace with generic phone num");
				}
	    	return createDataSetRequestJson;
		}
		
		/*public URI getDataSetUri(){
			return this.dataSetUri;
		}*/
		
		//see if a user exists
		public void checkUser(String username){
	    	String url = this.serverName + "/users/" + username + ".json";
	    	String result = restTemplate.getForObject(url, String.class);
		}
		
		public String createUser(String full_name, String login, String password, String password_confirmation, String email){
	    	String url = this.serverName + "/users.json";
	    	CreateUserRequestJson createUserRequestJson = this.createUserRequestObject(full_name, login, password, password_confirmation, email);
	    	try {
	    		String result = restTemplate.postForObject(url, createUserRequestJson, String.class);
	    		
	    		return "User created";
	    	} catch (Exception e){
	    		return e.getMessage();
	    	}
		}
		/*
		 * 400 Bad Request – If it was unable to create the user. Should also provide a message in the body of the response for example "{"password"=>[“doesn’t match confirmation”]}"
401 Unauthorized – Usually seen if user signups have been disabled.
		 */
		//create user
		/*
		 * curl -i -X POST -H "Content-Type: application/json"
--data-binary  ' "user": { "full_name": "TheWizard", "login": "wizard", "password": "secretpassword", "password_confirmation": "secretpassword","email": "wizard@geoiq.com" } '
  http://geocommons.com/users.json
		 */

		private CreateUserRequestJson createUserRequestObject(String full_name, String login, String password, String password_confirmation, String email) {
			CreateUserRequestJson createUserRequestJson = new CreateUserRequestJson();
			createUserRequestJson.user.setFull_name(full_name);
			createUserRequestJson.user.setLogin(login);
			createUserRequestJson.user.setPassword(password);
			createUserRequestJson.user.setPassword_confirmation(password_confirmation);
			createUserRequestJson.user.setEmail(email);
			return createUserRequestJson;
		}
		
/*		Location 	returns the URI of the file requested 	http://geocommons.com/overlays/7294.json

		Note that it possible that for a large dataset, the upload operation may actually be executed asynchronously. Before you can do anything with your data, you need to ensure that it has completed uploading successfully. You can do a GET to download a JSON copy of your dataset, then check the “state” attribute, which will have one of the following values:

		    processing – the system is still processing your dataset
		    errored – an error occurred
		    complete – the dataset is ready to use
		    parsed, geocoded, verified, etc – the dataset requires additional information to complete processing
*/
		public DataSetStatus checkDataSetStatus(String location){
	    	DataSetStatusResponse result = restTemplate.getForObject(location, DataSetStatusResponse.class);
	    	//what we want is the value of the "state" attribute
	    	DataSetStatus dataSetStatus = new DataSetStatus(result.getId(), result.getTitle(), result.getState());

			return dataSetStatus;
		}
}
