package org.OpenGeoPortal.Download;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.OpenGeoPortal.Download.LayerDownloader;
import org.OpenGeoPortal.Download.LayerInfoRetriever;
import org.OpenGeoPortal.Utilities.DirectoryRetriever;
import org.springframework.context.ApplicationContext; 

/**
 * This is an abstract class that provides the logic to determine which concrete class 
 * should be selected to download a layer.
 * 
 * 
 * @author Chris Barnett
 *
 */
abstract class AbstractDownloadHandler implements DownloadHandler {
	private BoundingBox bounds;
	String downloadDirectoryName = "download";
	protected File zipArchive = null;
	protected String layerLink;
	public List<RequestedLayer> layers = null; 
	private Map <String, Map <String, String>> layerInfo;
	private Boolean locallyAuthenticated = false;
	protected LayerInfoRetriever layerInfoRetriever;
	protected DownloadConfigRetriever downloadConfigRetriever;
	protected SearchConfigRetriever searchConfigRetriever;
	private String emailAddress = "";
	private DirectoryRetriever directoryRetriever;
	
	public DirectoryRetriever getDirectoryRetriever() {
		return directoryRetriever;
	}

	public void setDirectoryRetriever(DirectoryRetriever directoryRetriever) {
		this.directoryRetriever = directoryRetriever;
	}

	/**
	 * the main method of the class.  Initializes layers and bounds, calls download actions in appropriate
	 * order
	 * 
	 * @param layerMap a hashmap that maps Solr layer IDs to requested download format
	 * @param bounds an array of geodetic bounding coordinates (eastmost, southmost, westmost, northmost)
	 * @return boolean that indicates the success of the function
	 * @throws Exception
	 */
	public boolean requestLayers(Map<String,String> layerMap, String[] bounds){
		this.layers = new ArrayList<RequestedLayer>();
		this.bounds = new BoundingBox(bounds[0], bounds[1], bounds[2], bounds[3]);
		try {
			this.layerInfo = this.layerInfoRetriever.getAllLayerInfo(layerMap.keySet());
		} catch (Exception e) {
			return false;
		}
		this.routeDownloads(this.createDownloadMap(layerMap));
		this.doWork();
		return true;
	}

	/**
	 * a method to set the locallyAuthenticated property.  
	 * 
	 * This is a way to pass information about the user's session into the java class.  If the user has 
	 * authenticated locally, a session variable is set.  The calling code should set this value.
	 * 
	 * @param authenticated  true if the user has authenticated locally, otherwise false
	 */
	public void setLocallyAuthenticated(Boolean authenticated){
		this.locallyAuthenticated = authenticated;
	}
	
	/**
	 * a method to get the locallyAuthenticated property.  
	 * 
	 * @return true if the user has authenticated locally, otherwise false
	 */
	public Boolean getLocallyAuthenticated(){
		return this.locallyAuthenticated;
	}
	

	/**
	 * a method to get a concrete class of type LayerDownloader given a string key defined in WEB-INF/download.xml
	 * 
	 * 
	 * @param downloaderKey a string key that identifies a concrete class of LayerDownloader
	 * @return the concrete LayerDownloader object
	 */
	public LayerDownloader getLayerDownloader(String downloaderKey){
		ApplicationContext injector = AppContext.getApplicationContext();
		LayerDownloader layerDownloader = (LayerDownloader) injector.getBean(downloaderKey);
		return layerDownloader;
	}
	
	public File getDownloadDirectory() {
		try {
			File theDirectory = directoryRetriever.getDirectory(this.downloadDirectoryName);
			return theDirectory;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("The directory \"" + this.downloadDirectoryName + "\" could not be retrieved.");
			return null;
		}
	};
	/**
	 * a method that finds the appropriate concrete LayerDownloader and makes the actual request to download layers.
	 * 
	 * @param downloadMap a map that relates a string key (that identifies the concrete LayerDownloader Class) to a List of
	 * RequestedLayer objects that can be downloaded using that concrete class.
	 */
	public void routeDownloads(Map <String, List<RequestedLayer>> downloadMap){
		for (String currentDownloader: downloadMap.keySet()){
			//get concrete class key from config
			try{
				LayerDownloader layerDownloader = this.getLayerDownloader(currentDownloader);
				layerDownloader.downloadLayers(downloadMap.get(currentDownloader));
			} catch (Exception e) {
				System.out.println("routeDownloads: " + e.getMessage());
				continue;
			}
		}
	}
	
	/**
	 * a method that maps inputs (layerIDs mapped to requested formats) to [download method key:<RequestedLayer>]
	 * 
	 * The method references the method "getClassKey" in DownloadConfigRetriever to retrieve the class
	 * key from the layer's ID
	 * 
	 * @param layerMap a Map that relates a string layer ID to the requested download format
	 * @return downloadMap
	 */
	public Map <String, List<RequestedLayer>> createDownloadMap(Map<String,String> layerMap){
		Set<String> layerIdSet = layerMap.keySet();
		Map <String, List<RequestedLayer>> downloadMap = new HashMap<String, List<RequestedLayer>>(); 
		for (String currentLayerId: layerIdSet){
			String currentRequestedFormat = layerMap.get(currentLayerId);
			RequestedLayer currentLayer = null;
			try {
				currentLayer = this.createLayer(currentLayerId, currentRequestedFormat);
			} catch (Exception e1) {
				e1.printStackTrace();
				// TODO this fails silently...should return info to the user
				continue;
			}

			if (!currentLayer.accessLevel.equalsIgnoreCase("public")){
				try {
					if (currentLayer.institution.equalsIgnoreCase(this.searchConfigRetriever.getHome())){
						if (!this.getLocallyAuthenticated()){
							//if the user is not logged in, deny the request
							currentLayer.setStatus(LayerStatus.PERMISSION_DENIED);
							continue;
						}
					} else {
						//currently no way to log in to remote institutions, so just deny all requests
						currentLayer.setStatus(LayerStatus.PERMISSION_DENIED);
						continue;
					}
				} catch (Exception e){
					currentLayer.setStatus(LayerStatus.PERMISSION_DENIED);
					System.out.println("CONFIGURATION ERROR: Please set your home institution in ogpConfig.json.");
				}
			}
			String currentClassKey = null;
			try {
				currentClassKey = this.downloadConfigRetriever.getClassKey(currentLayer);
			} catch(Exception e) {
				currentLayer.setStatus(LayerStatus.NO_DOWNLOAD_METHOD);
				//System.out.println(e.getMessage());
				continue;
			}
			if (downloadMap.containsKey(currentClassKey)){
				List<RequestedLayer> currentLayerList = downloadMap.get(currentClassKey);
				currentLayerList.add(currentLayer);
			} else {
				List<RequestedLayer> newLayerList = new ArrayList<RequestedLayer>();
				newLayerList.add(currentLayer);
				downloadMap.put(currentClassKey, newLayerList);
			}
		}
		return downloadMap;
	}
	
	/**
	 * a method that creates a RequestedLayer object for each requested layer that can hold
	 * metadata for the layer, as well as state information
	 * 
	 * 
	 * @param layerId a string that is the layer's id in Solr
	 * @param requestedFormat a string that is the download format requested for the layer
	 * @return the RequestedLayer object for the layer
	 * @throws Exception 
	 */
	private RequestedLayer createLayer(String layerId, String requestedFormat) throws Exception{
		//some validation should happen here;  an exception should be thrown if there is a problem
		RequestedLayer layer = new RequestedLayer(layerId, requestedFormat);
		layer.institution = this.layerInfo.get(layerId).get("Institution");
		layer.accessLevel = this.layerInfo.get(layerId).get("Access");
		layer.dataType = this.layerInfo.get(layerId).get("DataType");
		layer.name = this.layerInfo.get(layerId).get("Name");
		layer.workSpace	= this.layerInfo.get(layerId).get("WorkspaceName");
		layer.title	= this.layerInfo.get(layerId).get("LayerDisplayName");
		layer.serviceURLs = this.layerInfo.get(layerId).get("Location");
		layer.requestedBounds = this.bounds;
		layer.emailAddress = this.emailAddress;
		layer.nativeBounds = new BoundingBox(this.layerInfo.get(layerId).get("MinX"),this.layerInfo.get(layerId).get("MinY"),
				this.layerInfo.get(layerId).get("MaxX"),this.layerInfo.get(layerId).get("MaxY"));
		layer.targetDirectory = this.getDownloadDirectory();
		this.layers.add(layer);
		return layer;
	}
	
	
	/**
	 * a method that must be implemented in any concrete DownloadHandler that defines what work
	 * gets done to each layer after download
	 * 
	 */
	abstract void doWork();
	
	/**
	 * a method that must be implemented in any concrete DownloadHandler that gets a JSON response
	 * that can be returned to the client, containing info about the download process (error, success, etc.)
	 * 
	 */
	public abstract String getJsonResponse();
	
	/**
	 * a method that sets an email address.  Only used for certain download types
	 * 
	 * @param a string containing the user's email address passed from the client
	*/
	public void setReplyEmail(String emailAddress){
		this.emailAddress = emailAddress;
	}

}
