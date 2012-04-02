package org.OpenGeoPortal.Export.GeoCommons;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.client.HttpClientErrorException;

public class ExportServlet implements HttpRequestHandler {
	private GeoCommonsClient geoCommonsClient;
	
	public GeoCommonsClient getGeoCommonsClient() {
		return geoCommonsClient;
	}


	public void setGeoCommonsClient(GeoCommonsClient geoCommonsClient) {
		this.geoCommonsClient = geoCommonsClient;
	}


	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		//given a list of ogpids, export them to geocommons
		//read the POST'ed JSON object
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(request.getInputStream());
		String basemap = rootNode.path("basemap").getTextValue();
		String bbox = rootNode.path("extent").getTextValue();
		String username = rootNode.path("username").getTextValue();
		String password = rootNode.path("password").getTextValue();
		
		if ((username.isEmpty())||(password.isEmpty())){
			response.sendError(401, "This request requires a valid GeoCommons username and password.");
			return;
		}
		String title = rootNode.path("title").getTextValue();
		String description = rootNode.path("description").getTextValue();
		JsonNode idArray = rootNode.path("OGPIDS");
		ArrayList<String> layers = new ArrayList<String>();
		for (JsonNode idNode : idArray){
			layers.add(idNode.getTextValue());
		}
		if (layers.isEmpty()) {
			response.sendError(400, "No layers specified in request.");
			return;
		}
	
		//get username and password from session object
    	//if it doesn't exist, return an object that pops up a geocommons login dialog

    	this.geoCommonsClient.initializeClient(username, password);
    	
    	try {
    		this.geoCommonsClient.checkUser(username);
    	} catch (HttpClientErrorException e){
			response.sendError(401, "This request requires a valid GeoCommons username and password.");
    		return;
    	}
    	ArrayList<String> dataSetLocations = new ArrayList<String>();
    	
    	for (String layer : layers){
    		System.out.println(layer);
    		try {
        		dataSetLocations.add(this.geoCommonsClient.uploadKmlDataSet(layer));
    		} catch (Exception e){
    			System.out.println("There was an error adding this data set to GeoCommons.");
    			System.out.println(e.getMessage());
    			System.out.println(e.getStackTrace());
    		}
    	}
    	
    	//before we create a map, we need to check the status of the added data sets
    	ArrayList<DataSetStatus> dataSetStatusArray = new ArrayList<DataSetStatus>();
    		for (String location : dataSetLocations){
    			DataSetStatus status = this.geoCommonsClient.checkDataSetStatus(location);
    			while (status.getStatus().equalsIgnoreCase("processing")||status.getStatus().equalsIgnoreCase("parsed")){
    				try {
    					Thread.sleep(5000);
    				} catch (InterruptedException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
    				status = this.geoCommonsClient.checkDataSetStatus(location);
    				System.out.println(status.getStatus());
    			}
    			System.out.println(status.getStatus());
				if(status.getStatus().equalsIgnoreCase("complete")||status.getStatus().equalsIgnoreCase("verified")){
					dataSetStatusArray.add(status);
				} else {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
    		}

    	if (dataSetStatusArray.isEmpty()){
    		return;
    	} else {
    		
    		String mapId = null;
    		try {
    			System.out.println("attempting to create Map...");
    			mapId = this.geoCommonsClient.createMap(basemap, bbox, title, description);
    		} catch (Exception e){
    			System.out.println(e.getMessage());
    			return;
    		}
    		try {
    			Thread.sleep(5000);
    		} catch (InterruptedException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
		
    		for (DataSetStatus successfulLayer : dataSetStatusArray){
    			System.out.println("attempting to add Layer: " + successfulLayer.getId());
    			try {
    				System.out.println(successfulLayer.getStatus());
    				System.out.println(successfulLayer.getTitle());

    				this.geoCommonsClient.addLayerToMap(successfulLayer.getId(), successfulLayer.getTitle(), mapId);
    			} catch (Exception e){
    				System.out.println("Failed to add layer to map.");
    			}
    		}
    	}	
	}

}
