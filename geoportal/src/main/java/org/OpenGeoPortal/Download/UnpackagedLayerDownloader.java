package org.OpenGeoPortal.Download;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ListIterator;

/**
 * Class to download layers that must be downloaded one at a time.
 * 
 * The actual method for downloading is injected by Spring
 * @author chris
 *
 */
//the layer downloader should handle all the errors thrown by the download method,
//and take care of layer status as much as possible
public class UnpackagedLayerDownloader extends AbstractLayerDownloader implements LayerDownloader {
	private UnpackagedDownloadMethod unpackagedDownloadMethod;
	
	/**
	 * UnpackagedDownloadMethod is injected
	 * @param unpackagedDownloadMethod
	 */
	public void setDownloadMethod(UnpackagedDownloadMethod unpackagedDownloadMethod){
		this.unpackagedDownloadMethod = unpackagedDownloadMethod;
	}
	
	public void downloadLayers(List<RequestedLayer> layerList) throws Exception {
		ListIterator<RequestedLayer> layerIterator = layerList.listIterator();
		while (layerIterator.hasNext()){
			RequestedLayer currentLayer = layerIterator.next();
			//does the RequestedLayer contain everything needed?
			//this.downloadMethod.validate(currentLayer);
				//check to see if the filename exists
			File currentFile = null;
			try {
				InputStream responseStream = this.unpackagedDownloadMethod.getResponseStream(currentLayer);

				currentFile = this.writeResponseToLocalFile(responseStream, 
						currentLayer);
				currentLayer.downloadedFiles.add(currentFile);
				currentLayer.setStatus(LayerStatus.DOWNLOAD_SUCCESS);
			} catch (Exception e){
				//e.printStackTrace();
				System.out.println("an error downloading this layer: " + currentLayer.name);
				currentLayer.setStatus(LayerStatus.DOWNLOAD_FAILED);
				continue;
			}
		} 
	}
	
	File writeResponseToLocalFile(InputStream source, RequestedLayer currentLayer)
	 throws IOException
	 {
		File newFile = createNewFileObject(currentLayer);
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
		currentLayer.setDisposition(LayerDisposition.DOWNLOADED_LOCALLY);
		return newFile;
	 }
}
