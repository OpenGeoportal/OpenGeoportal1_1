package org.OpenGeoPortal.Download;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Class to download layers that are pre-packaged by the remote server.
 * 
 * The actual method for downloading is injected by Spring
 * @author chris
 *
 */
//the layer downloader should handle all the errors thrown by the download method,
//and take care of layer status
public class PackagedLayerDownloader extends AbstractLayerDownloader implements LayerDownloader {
	//private List <RequestedLayer> requestedLayers;
	private String responseFileType;
	private String responseFileName;
	private PackagedDownloadMethod packagedDownloadMethod;
	private List<RequestedLayer> layerList;
	
	public void setPackagedDownloadMethod(PackagedDownloadMethod packagedDownloadMethod){
		this.packagedDownloadMethod = packagedDownloadMethod;
	}
	
	@Override
	public String getResponseFileType() {
		return this.responseFileType;
	}
	
	@Override
	public String getResponseFileName() {
		return this.responseFileName;
	}
	
	@Override
	public void setResponseFileName(String responseFileName) {
		this.responseFileName = responseFileName;
	}
	
	public void setStatusForAllLayers(LayerStatus layerStatus){
		for (RequestedLayer currentLayer: this.layerList){
			currentLayer.setStatus(layerStatus);
		}
	}
	
	public void addFileHandleToAllLayers(File file){
		for (RequestedLayer currentLayer: layerList){
			currentLayer.downloadedFiles.add(file);
		}
	}
	
	public void downloadLayers(List<RequestedLayer> layerList) throws Exception {
		this.layerList = layerList;
		@SuppressWarnings("unused")
		InputStream responseStream;
		try{
			responseStream = this.packagedDownloadMethod.getResponseStream(layerList);
		} catch(Exception e) {
			System.out.println("an error downloading these layers ");
			System.out.println(e.getMessage());
			this.setStatusForAllLayers(LayerStatus.DOWNLOAD_FAILED);
			return;
		}
		//File currentFile = this.writeResponseToLocalFile(responseStream, layerList);
		//this.addFileHandleToAllLayers(currentFile);
		this.setStatusForAllLayers(LayerStatus.DOWNLOAD_SUCCESS);
	}	
	
	File writeResponseToLocalFile(InputStream source, List<RequestedLayer> layerList)
	 throws IOException
	 {
		String fileName = "ogpPackage" + Math.round(Math.random() * 10000) + ".zip"; 
		File newFile = new File(fileName);
		FileOutputStream fileOutputStream = new FileOutputStream(newFile);
		InputStream bufferedIn = new BufferedInputStream(source);
		try {
			int currentBytes;
			while ((currentBytes = bufferedIn.read()) != -1) {
				//System.out.println("Receiving " + currentBytes + " bytes");
				fileOutputStream.write(currentBytes);
			} 
		} finally {
			try {
				bufferedIn.close();
			} finally {
				fileOutputStream.close();
			}
		}
		for (RequestedLayer currentLayer : layerList){
			currentLayer.setDisposition(LayerDisposition.DOWNLOADED_LOCALLY);
		}
		return newFile;
			 

	 }
}
