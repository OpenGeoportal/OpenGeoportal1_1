/**
 * This javascript module includes functions for exporting public layers to
 * GeoCommons via KML
 * 
 * @author Chris Barnett
 */

if (typeof org == 'undefined'){ 
	org = {};
} else if (typeof org != "object"){
	throw new Error("org already exists and is not an object");
}

// Repeat the creation and type-checking code for the next level
if (typeof org.OpenGeoPortal == 'undefined'){
	org.OpenGeoPortal = {};
} else if (typeof org.OpenGeoPortal != "object"){
    throw new Error("org.OpenGeoPortal already exists and is not an object");
}

//Repeat the creation and type-checking code for the next level
if (typeof org.OpenGeoPortal.Export == 'undefined'){
	org.OpenGeoPortal.Export = {};
} else if (typeof org.OpenGeoPortal.Export != "object"){
    throw new Error("org.OpenGeoPortal.Export already exists and is not an object");
}

org.OpenGeoPortal.Export.GeoCommons = function GeoCommons(layerObj){
	this.layerObj = layerObj;
	
	this.init = function init(){
		
	};
	
	this.exportDialog = function(uiObject){
		var dialogContent = "";		
		var that = this;
		var dialogTitle = "Export Layers to GeoCommons";
		var dialogDivId = "geoCommonsDialog";
		//this.layerObj = uiObject.getLayerList("mapIt");
		var buttonsObj = {
			"Export to GeoCommons": function(){that.exportLayers.call(that);},
			Cancel: function() {
				jQuery(this).dialog('close');
			}
		};
		uiObject.dialogTemplate(dialogDivId, dialogContent, dialogTitle, buttonsObj);
		
		jQuery("#" + dialogDivId).dialog('open');
		
		jQuery("#" + dialogDivId).unbind("keypress");
		jQuery("#" + dialogDivId).bind("keypress", function(event){
			if (event.keyCode == '13') {
			} 
		});
		

	};
	
	
	this.ogpids = [];
	this.basemap = "acetate";
	this.extent;
	this.username = "chrissbarnett";
	this.password = "T3deum";
	this.title = "ogp export";
	this.description = "ogp export test";
	    
	//need code to create dialog
	//code to send ajax request to export servlet
		this.createExportRequest = function createExportRequest(){
			//need a list of ogpids, basemap, username, password, extent
			//title, description....most of these will be provided by a form
			var requestObj = {};
			requestObj.basemap = this.getBasemapId(this.basemap);
			requestObj.username = this.username;
			requestObj.password = this.password;
			requestObj.extent = "-180,-80,180,80";
			requestObj.title = this.title;
			requestObj.description = this.description;
			this.ogpids = [];
			for (var layerId in this.layerObj){
				this.ogpids.push(layerId);
			}
			requestObj.OGPIDS = this.ogpids;
			return requestObj;
		};
		
		this.getBasemapId = function getBasemapId(basemapDescriptor){
			basemapDescriptor = basemapDescriptor.toLowerCase();
			var basemapMap = {"acetate": "Acetate", "googlephysical":"Google Physical", "googlehybrid":"Google Hybrid",
					"googlesatellite": "Google Satellite", "googlestreet":"Google Street", "openstreetmap":"OpenStreetMap"};
			if(basemapMap[basemapDescriptor] != "undefined"){
				return basemapMap[basemapDescriptor];
			} else {
				throw new Exception('Basemap "' + basemapDescriptor + '" is undefined.');
			}
		};
		
		
		this.exportLayers = function exportLayers(){
			var requestObj = this.createExportRequest();
			console.log(JSON.stringify(requestObj));
			var params = {
				url: "geoCommonsExport",
				data: JSON.stringify(requestObj),
				processData: false,
				dataType: "json",
				type: "POST",
	    		context: this,
	    		success: function(data){
	    			//do stuff
	    		}
			};
			jQuery.ajax(params);
		};
};