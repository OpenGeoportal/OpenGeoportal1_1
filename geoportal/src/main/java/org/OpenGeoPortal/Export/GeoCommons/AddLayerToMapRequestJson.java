package org.OpenGeoPortal.Export.GeoCommons;

/*
 * Required Parameters:
Parameter 	Description 	Example
source 	the source of the data layer for GeoIQ/GeoCommons layers it is “finder:xxxx” for external sources it is “url:http://example.com” 	source=finder:98765

Optional Parameters:
Parameter 	Description 	Example
title 	the title of the layer 	title=“Population and Age”
subtitle 	the subtitle of the layer 	subtitle="200 Census Demographics
opacity 	the opacity of the layer default is 1.0 	opacity=.5
styles 	the styling of the layer (see detailed styling information below) 	see style examples below
visible 	sets if the layer is visible, or turned off 	visible=true
 * 
 * 
 */
public class AddLayerToMapRequestJson {
	String source;
	Boolean visible;
	String title;

	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	public Boolean getVisible() {
		return visible;
	}
	public void setVisible(Boolean visible) {
		this.visible = visible;
	}
}
