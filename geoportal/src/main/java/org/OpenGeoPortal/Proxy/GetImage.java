package org.OpenGeoPortal.Proxy;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.OpenGeoPortal.Authentication.OgpAuthenticator;
import org.OpenGeoPortal.Download.LayerInfoRetriever;
import org.OpenGeoPortal.Download.SearchConfigRetriever;
import org.OpenGeoPortal.Utilities.DirectoryRetriever;
import org.OpenGeoPortal.Utilities.OgpLogger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.*;

import org.codehaus.jackson.map.ObjectMapper;

import org.slf4j.Logger;
import org.springframework.web.HttpRequestHandler; 

public class GetImage implements HttpRequestHandler {
	@OgpLogger
	Logger logger;
	
	private GenericProxy genericProxy;
	private LayerInfoRetriever layerInfoRetriever;
	private DirectoryRetriever directoryRetriever;
	private String downloadDirectoryName = "download";
	private OgpAuthenticator ogpAuthenticator;
	private String proxyTo;
	private SearchConfigRetriever searchConfigRetriever;

	/**
	
		 * 
		 * ultimately, this servlet, given the above parameters + z order, should be able to grab images
		 * from various servers and composite them.  Should be passed a custom json object to maintain structure.
		 * 
		 * offer formats available via geoserver.
		 * 
		 * 
		 * determine if a layer is within the provided bounds and exclude it if not
		 * 
		 * 
		 * 
		 
	 */
	
	public String getProxyTo() {
		return proxyTo;
	}

	public void setProxyTo(String proxyTo) {
		this.proxyTo = proxyTo;
	}
	
	public SearchConfigRetriever getSearchConfigRetriever() {
		return searchConfigRetriever;
	}

	public void setSearchConfigRetriever(SearchConfigRetriever searchConfigRetriever) {
		this.searchConfigRetriever = searchConfigRetriever;
	}

	public DirectoryRetriever getDirectoryRetriever() {
		return directoryRetriever;
	}

	public void setDirectoryRetriever(DirectoryRetriever directoryRetriever) {
		this.directoryRetriever = directoryRetriever;
	}
	
	public void setGenericProxy(GenericProxy genericProxy){
		this.genericProxy = genericProxy;
	}
	
	public GenericProxy getGenericProxy(){
		return this.genericProxy;
	}
	
	public void setLayerInfoRetriever(LayerInfoRetriever layerInfoRetriever){
		this.layerInfoRetriever = layerInfoRetriever;
	}
	
	public LayerInfoRetriever getLayerInfoRetriever(){
		return this.layerInfoRetriever;
	}
	
	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		logger.info("test logger");
		//read the POST'ed JSON object
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(request.getInputStream());
		String srs = rootNode.path("srs").getTextValue();
		String bbox = rootNode.path("bbox").getTextValue();
		String format = rootNode.path("format").getTextValue();
		Integer height = rootNode.path("height").asInt();
		Integer width = rootNode.path("width").asInt();
		ArrayNode layers = (ArrayNode) rootNode.path("layers");
        String formatSuffix = "png";

	    Set<String> layerIds = new HashSet<String>();
	    Map<Integer, ArrayList<String>> zIndexMap = new TreeMap<Integer, ArrayList<String>>();
	    
		Iterator<JsonNode> layerIterator = layers.iterator();
		
		while (layerIterator.hasNext()){
			//actually, lets use zIndex as a key, and make this a sorted map
			JsonNode currentLayer = layerIterator.next();
			ArrayList<String> layerParams = new ArrayList<String>();
			String layerId = currentLayer.path("layerId").getTextValue();
		    layerIds.add(layerId.trim());
			String sld = currentLayer.path("sld").getTextValue();
		    String opacity = currentLayer.path("opacity").asText();

			layerParams.add(layerId);
			layerParams.add(sld);
			layerParams.add(opacity);
			
		    int currentZIndex = currentLayer.path("zIndex").asInt();
		    zIndexMap.put(currentZIndex, layerParams);
		}
    	
	    Map<String, Map<String,String>> layerInfo = null;
	    try {
			layerInfo = this.layerInfoRetriever.getAllLayerInfo(layerIds);
		} catch (Exception e) {
			//e.printStackTrace();
			response.sendError(500);
		}
	    
	    
        String urlFormat = URLEncoder.encode(format, "UTF-8");
        //switch based on format to add dpi settings, change/add header info
        String genericQueryString;
    	genericQueryString = "?service=wms&version=1.1.1&request=GetMap&format=" + urlFormat + "&SRS=" + srs;
        genericQueryString += "&styles=&bbox=" + bbox;

    	if (format.equals("image/png")){
	    	
    		/*int requestedDpi = 90;//dpi & size options?
    		url$ += "&format_options=dpi:" + Integer.toString(requestedDpi) + ";";
    		width = Integer.toString(Math.round(Integer.parseInt(width) * requestedDpi / 90));
    		height = Integer.toString(Math.round(Integer.parseInt(height) * requestedDpi / 90));
    		*/
    		genericQueryString += "&tiled=false&transparent=true";
    	} 
    	genericQueryString += "&height=" + height + "&width=" + width;
	    

	    BufferedImage compositeImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	    Graphics2D compositeImageGraphicsObj = compositeImage.createGraphics();
	    
    	//at this point, we need to iterate over the zIndexes, construct appropriate urls, layer on top of image, then write to a file
    	Iterator<Integer> zIndexIterator = zIndexMap.keySet().iterator();
    	while (zIndexIterator.hasNext()){
    		Integer currentZIndex = zIndexIterator.next();
    		
    			String layerQueryString = genericQueryString;
    		   	String currentSLD = zIndexMap.get(currentZIndex).get(1);
    		   	String currentLayerId = zIndexMap.get(currentZIndex).get(0);
    		   	String currentOpacity = zIndexMap.get(currentZIndex).get(2);

    		   	Map<String,String> currentLayerMap = layerInfo.get(currentLayerId);
    		   	if (!currentLayerMap.get("Access").equalsIgnoreCase("public")){
    		   		try {
						if (currentLayerMap.get("Institution").equalsIgnoreCase(this.searchConfigRetriever.getHome())){
							if (!this.ogpAuthenticator.isAuthenticated(request)){
								continue;
							}
						} else {
							continue;
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						continue;
					}
    		   	}
    		   	
    		   	if ((currentSLD != null)&&(!currentSLD.equals("null")&&(!currentSLD.isEmpty()))){
    		   		layerQueryString += "&sld_body=" + currentSLD;//URLEncoder.encode(currentSLD, "UTF-8");
    		   	}
    		   	
    		   	layerQueryString += "&layers=" + currentLayerMap.get("WorkspaceName") + ":" + currentLayerMap.get("Name");


    		   	String remoteUrlString = null;

    		   	//a kludge;  really the simplegenericproxy should be able to handle this
    		   	//System.out.println(this.layerInfoRetriever.hasProxy(currentLayerMap));
    			if (this.layerInfoRetriever.hasProxy(currentLayerMap)){
    				remoteUrlString = this.proxyTo;
    			}  else {
        		   	try {
    					remoteUrlString = this.layerInfoRetriever.getWMSUrl(currentLayerMap);
    				} catch (Exception e1) {
    					// TODO Auto-generated catch block
    					e1.printStackTrace();
    				}
    			}
    			
    		   	//now we have everything we need to create a request
    		   	
    		   	BufferedImage currentImg = null;
    		   	
    		   	System.out.println(remoteUrlString + layerQueryString);

    				URL remoteUrl = new URL(remoteUrlString + layerQueryString);
    		        try {
    		        	currentImg = ImageIO.read(remoteUrl);
    		        } catch (IOException e) {
    		        }
		            
    		   	//this needs to be done for each image received

		        int opacityNum = Integer.parseInt(currentOpacity.trim());
		        
		        //this defines opacity...do I need this, or will sld take care of it?
		        float[] scales = { 1f, 1f, 1f, 1f};
		        scales[3] = opacityNum / 100f;
		        //System.out.println(scales[3]);
		        float[] offsets = new float[4];
		        RescaleOp rop = new RescaleOp(scales, offsets, null);
		        compositeImageGraphicsObj.drawImage(currentImg, rop, 0, 0);
    	}
        //after we're done
        File outputFile = null;
        try {
            // retrieve image
        	File imageDirectory = directoryRetriever.getDirectory(this.downloadDirectoryName);
    		do {
    			String outputFileName = "OGPImage" + Math.round(Math.random() * 10000) + "." + formatSuffix;
    			outputFile = new File(imageDirectory, outputFileName);
    		} while (outputFile.exists());
            //write image to file
            ImageIO.write(compositeImage, formatSuffix, outputFile);
        } catch (IOException e) {
            //...

        } 
        File archive = this.zipFile(outputFile);
        compositeImageGraphicsObj.dispose();
        String jsonOutput = "{\"imageLink\":\"" + this.downloadDirectoryName + "/" + archive.getName() + "\", \"errors\":[]}";
        response.setContentType("application/json");
        response.getOutputStream().print(jsonOutput);
       
	}
	
	File zipFile(File file) throws IOException{
		ZipOutputStream newZipStream = null;
		String zipArchiveName = file.getName().substring(0, file.getName().indexOf(".")) + ".zip";
		File zipArchive = new File(file.getParentFile(), zipArchiveName);
		try {
			newZipStream = new ZipOutputStream(new FileOutputStream (zipArchive));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	    FileInputStream currentFileStream = new FileInputStream(file);
		byte[] buffer = new byte[1024 * 1024];
	    int bytesRead;
	    String entryName = file.getName();
	    ZipEntry zipEntry = new ZipEntry(entryName);
	    newZipStream.putNextEntry(zipEntry);
	    while ((bytesRead = currentFileStream.read(buffer))!= -1) {
	    	newZipStream.write(buffer, 0, bytesRead);
	    }
	    file.delete();
		newZipStream.close();
		return zipArchive;
	}

	public OgpAuthenticator getOgpAuthenticator() {
		return ogpAuthenticator;
	}

	public void setOgpAuthenticator(OgpAuthenticator ogpAuthenticator) {
		this.ogpAuthenticator = ogpAuthenticator;
	}

}
