package org.OpenGeoPortal.Download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.OpenGeoPortal.Utilities.FileName;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;


/**
 * This class extends AbstractDownloadHandler to add the behavior of packaging downloaded
 * layers together in a zip file. 
 * 
 * @author Chris Barnett
 *
 */
public class DownloadPackager extends AbstractDownloadHandler implements
		DownloadHandler {
	private MetadataRetriever metadataRetriever;

	/**
	 * a DownloadConfigRetriever is injected
	 * @param downloadConfigRetriever
	 */
	public void setDownloadConfigRetriever(DownloadConfigRetriever downloadConfigRetriever){
		this.downloadConfigRetriever = downloadConfigRetriever;
	}
	
	/**
	 * a LayerInfoRetriever is injected
	 * @param layerInfoRetriever
	 */
	public void setLayerInfoRetriever(LayerInfoRetriever layerInfoRetriever){
		this.layerInfoRetriever = layerInfoRetriever;
	}
	
	/**
	 * a MetadataRetriever is injected
	 * @param metadataRetriever
	 */
	public void setMetadataRetriever(MetadataRetriever metadataRetriever){
		this.metadataRetriever = metadataRetriever;
	}
	
	/**
	 * a SearchConfigRetriever is injected
	 * @param searchConfigRetriever
	 */
	public void setSearchConfigRetriever(SearchConfigRetriever searchConfigRetriever){
		this.searchConfigRetriever = searchConfigRetriever;
	}
	
	/**
	 * defines what post processing happens after layers are downloaded.  In this case, retrieves metadata if needed,
	 * adds all the files to a zip archive
	 */
	@Override
	void doWork() {
		final File downloadDirectory = this.getDownloadDirectory();
		try {
			this.addFilesToZipArchive();
			this.setLayerLink(this.downloadDirectoryName + "/" + this.zipArchive.getName());
		} catch (Exception e) {
			System.out.println("File Download Error(doWork): " + e.getMessage());
		} finally {
			 new Thread(new Runnable() {
		            public void run() { cleanupDownloadDirectory(downloadDirectory, 300); }
		        }).start();
		}
	}
	
	void setLayerLink(String layerLink){
		this.layerLink = layerLink;
	}
	
	String getLayerLink(){
		return this.layerLink;
	}
	
	/**
	 * creates a zip file for the downloaded layers
	 * 
	 * constructs a randomized name based on a pattern.  
	 * 
	 * @return zipFile file handle object
	 * @throws Exception
	 */
	private File createZipArchive(){
		File zipFile;
		do {
			String zipFileName = "OGP" + Math.round(Math.random() * 10000) + ".zip";
			zipFile = new File(this.getDownloadDirectory(), zipFileName);
		} while (zipFile.exists());
		this.zipArchive = zipFile;
		return zipFile;
	}
	
	private static void cleanupDownloadDirectory(File downloadDirectory, long fileAgeMinutes){
		try {
			//convert to milliseconds
			long timeInterval = fileAgeMinutes * 60 * 1000;
			File[] downloadedFiles = downloadDirectory.listFiles();
			for (File downloadedFile : downloadedFiles) {
				long currentTime = System.currentTimeMillis();
				if (currentTime - downloadedFile.lastModified() > timeInterval){
					System.out.println("deleting " + downloadedFile.getName());
					downloadedFile.delete();
				}
			}
		} catch (Exception e) {
			System.out.println("Attempt to delete old files was unsuccessful.");
		}
		
	}

	private Set<File> getFilesToPackage(){
	    Set<File> filesToPackage = new HashSet<File>();
	    for (RequestedLayer layer : this.layers) {
	    	if ((layer.getStatus() == LayerStatus.DOWNLOAD_SUCCESS)&&
    			(layer.getDisposition() == LayerDisposition.DOWNLOADED_LOCALLY)){

	    		if (!layer.metadata){
	    			//get metadata for this layer, add the resulting xml file to the file list
	    			File xmlFile;
	    			if (layer.isVector()&&(!layer.requestedFormat.equals("kmz"))){
	    				xmlFile = new File(this.getDownloadDirectory(), FileName.filter(layer.name) + ".shp.xml");
	    			} else {
	    				xmlFile = new File(this.getDownloadDirectory(), FileName.filter(layer.name) + ".xml");
	    			}

	    			try {
						layer.downloadedFiles.add(this.metadataRetriever.getXMLFile(layer.name, xmlFile));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						//couldn't get the metadata, but don't kill the download
						e.printStackTrace();
					} 
	    		}
	    		
	    		filesToPackage.addAll(layer.downloadedFiles);

	    	}
	    }

		return filesToPackage;
	};
	    
	    
	/**
	 * Creates a zip archive of downloaded files.
	 * 
	 * Cycles through the RequestedLayer objects in 'layers'.  If the status of the layer indicates that
	 * the layer was successfully downloaded, the method adds the associated files to a zip archive.
	 * @throws FileNotFoundException 
	 * @throws Exception 
	 * 
	 * @throws Exception
	 */
	private void addFilesToZipArchive() throws FileNotFoundException {

		System.out.println("packaging files...");
	    Set<File> filesToPackage = this.getFilesToPackage();
	    if (filesToPackage.isEmpty()){
	    	//if there are no files, don't do anything.
	    	return;
	    }
	    
		byte[] buffer = new byte[1024 * 1024];
		long startTime = System.currentTimeMillis();
	    
		ZipOutputStream newZipStream = null;
		newZipStream = new ZipOutputStream(new FileOutputStream (this.createZipArchive()));
	    int zipFileCounter = 0;
	    for (File currentFile : filesToPackage){
	    	try{
	    		FileInputStream currentFileStream = new FileInputStream(currentFile);
	    		zipFileCounter++;
	    		if (!currentFile.getName().contains(".zip")){
	    			//add this uncompressed file to the archive
	    			int bytesRead;
	    			String entryName = currentFile.getName();
	    			ZipEntry zipEntry = new ZipEntry(entryName);
	    			newZipStream.putNextEntry(zipEntry);
	    			while ((bytesRead = currentFileStream.read(buffer))!= -1) {
	    				newZipStream.write(buffer, 0, bytesRead);
	    			}
	    		} else {
	    			//read the entries from the zip file and copy them to the new zip archive
	    			//so that we don't have to recompress them.
	    			ZipInputStream currentZipStream = new ZipInputStream(currentFileStream);
	    			ZipEntry currentEntry;
	    			while ((currentEntry = currentZipStream.getNextEntry()) != null) {
	    				String entryName = currentEntry.getName();
	    				ZipEntry zipEntry = new ZipEntry(entryName);
	    				try {
	    					newZipStream.putNextEntry(zipEntry);
	    				} catch (ZipException e){
	    					//duplicate names should never happen.
	    					entryName = Math.round(Math.random() * 10000) + "_" + entryName;
	    					ZipEntry zipEntry2 = new ZipEntry(entryName);
	    					newZipStream.putNextEntry(zipEntry2);
	    				}
	    				int bytesRead;
	    				while ((bytesRead = currentZipStream.read(buffer))!= -1) {
	    					newZipStream.write(buffer, 0, bytesRead);
	           	 		}
	    			}
	    			currentZipStream.close();
	    		}	
	    	} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				//always delete the file
	    		currentFile.delete();

	    	}
    	}
	    
	    if (zipFileCounter > 0){
	     	try {
				newZipStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    
		long endTime = System.currentTimeMillis();
		System.out.println(zipFileCounter + " file(s) zipped in " + (endTime - startTime) + " milliseconds.");
	}

	/**
	 * Creates a JSON string to return to the client, so that it knows what to do.
	 * 
	 * iterates through RequestedLayer objects in 'layers', constructing a JSON object that indicates
	 * success or failure for each layer, also includes the link to any created zip archive of downloaded
	 * files
	 * 
	 * @return a JSON string
	 */
	@Override
	public String getJsonResponse() {
		//return a status message for each requested layer
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode successArray = mapper.createArrayNode();
		ArrayNode failureArray = mapper.createArrayNode();

	    for (RequestedLayer layer : this.layers) {
	    	//System.out.println(layer.name);
    		ObjectNode layerInfo = mapper.createObjectNode();
    		layerInfo.put("layerId", layer.id);
    		layerInfo.put("institution", layer.institution);
    		layerInfo.put("title", layer.title);
    		layerInfo.put("disposition", layer.getDisposition().name());
    		layerInfo.put("message", layer.getStatus().message());
    		
	    	if (layer.getStatus() == LayerStatus.DOWNLOAD_SUCCESS){
	    		successArray.add(layerInfo);
	    	} else {
	    		failureArray.add(layerInfo);
	    	}
	    }

		ObjectNode responseObject = mapper.createObjectNode();
		try {
			if (this.zipArchive.exists()){
				responseObject.put("packageLink", this.getLayerLink());
			}
		} catch (Exception e){}
	    responseObject.put("succeeded", successArray);
	    responseObject.put("failed", failureArray);

	    return responseObject.getValueAsText();
	}

}
