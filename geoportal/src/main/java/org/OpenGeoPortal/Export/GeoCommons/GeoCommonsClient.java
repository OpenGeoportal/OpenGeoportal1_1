package org.OpenGeoPortal.Export.GeoCommons;

public interface GeoCommonsClient {
	void initializeClient(String username, String password);
    String uploadKmlDataSet(String layerId) throws Exception;
	String createMap(String basemap, String extent, String title, String description) throws Exception;
	void checkUser(String username);
	String createUser(String full_name, String login, String password, String password_confirmation, String email);
	DataSetStatus checkDataSetStatus(String location);
    void addLayerToMap(String layerId, String layerTitle, String mapId) throws Exception;


}
