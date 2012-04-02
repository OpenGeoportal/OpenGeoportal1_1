//not currently being used, since we are using the google geocoder
org.OpenGeoPortal.UserInterface.prototype.geoSearch = function() {
	if (true == true) return;
	jQuery("#geosearch").autocomplete({
		source: function(request, response) {
			jQuery.ajax({
				url: "http://ws.geonames.org/searchJSON",
				dataType: "jsonp",
				data: {
					style: "long",
					maxRows: 12,
					name: request.term
				},
				success: function(data) {
					response(jQuery.map(data.geonames, function(item) {
						var mercatorCoords = map.WGS84ToMercator(item.lng, item.lat);
						return {
							label: item.name + (item.adminName1 ? ", " + item.adminName1 : "") + ", " + item.countryName,
							value: item.name,
							lat: mercatorCoords.lat,
							lon: mercatorCoords.lon
						};
					}));
				}
			});
		},
		minLength: 2,
		delay: 750,
		select: function(event, ui) {
			map.setCenter(new OpenLayers.LonLat(ui.item.lon, ui.item.lat));
			//fire off another ajax request calling google geocoder
		},
		open: function() {
			jQuery(this).removeClass("ui-corner-all").addClass("ui-corner-top");
		},
		close: function() {
			jQuery(this).removeClass("ui-corner-top").addClass("ui-corner-all");
		}
	});
};

//used?
org.OpenGeoPortal.UserInterface.prototype.createQueryString = function(){
	var searchType = this.whichSearch().type;
	if (searchType == 'basicSearch'){
		var searchString = 'searchTerm=' + jQuery('#basicSearchTextField').val();
	    searchString += '&topic=' + jQuery('#selectTopic').val();
	} else if (searchType =='advancedSearch'){
		var searchString = 'keyword=' + jQuery('#advancedKeywordText').val();
		searchString += '&topic=' + jQuery('#advancedSelectTopic').val();
		//searchString += '&collection=' + jQuery('#advancedCollectionText').val();
		searchString += '&publisher=' + jQuery('#advancedPublisherText').val();
		searchString += '&dateFrom=' + jQuery('#advancedDateFromText').val();	
		searchString += '&dateTo=' + jQuery('#advancedDateToText').val();
		searchString += '&typeRaster=' + this.getCheckboxValue('dataTypeCheckRaster');
		searchString += '&typeVector=' + this.getCheckboxValue('dataTypeCheckVector');
		searchString += '&typeMap=' + this.getCheckboxValue('dataTypeCheckMap');
		searchString += '&sourceHarvard=' + this.getCheckboxValue('sourceCheckHarvard');
		searchString += '&sourceMit=' + this.getCheckboxValue('sourceCheckMit');
		searchString += '&sourceMassGis=' + this.getCheckboxValue('sourceCheckMassGis');
		searchString += '&sourcePrinceton=' + this.getCheckboxValue('sourceCheckPrinceton');
		searchString += '&sourceTufts=' + this.getCheckboxValue('sourceCheckTufts');
	}	
	if (this.filterState()){
		// pass along the extents of the map
		var extent = map.returnExtent();
		searchString += "&minX=" + extent.minX + "&maxX=" + extent.maxX + "&minY=" + extent.minY + "&maxY=" + extent.maxY; 
	}
	
	return searchString;
};

//must exclude header cell for the following click handlers

// this is a test function
// it tests jQuery creating a script tag 
/*
ajaxTest = function(thisObj){
    		var ajaxParams = {
  	    		type: "GET",
  	    		url: "http://geoportal-dev.atech.tufts.edu:8480/temp.jsp",
  	            dataType: 'jsonp',
  	            success: function(data){
    					var solrResponse = data["response"];
    					var totalResults = solrResponse["numFound"];
    					alert("in ajaxTest with " + totalResults + ", and " + data);
    					foo = data;
  	            },
  	            error: function() {throw new Error("The attempt to retrieve FGDC layer information failed.");}
  	    };
  	    jQuery.ajax(ajaxParams);
};
*/
/*this has been replaced with a jsonp version
//click-handler for showing metadata pane
showMetadata = function(thisObj){
	  	var tableElement = jQuery(thisObj).parents('tr').last();
      var tableObj = tableElement.parent().parent().dataTable();	
	  //Get the position of the current data from the node 
    		var aPos = tableObj.fnGetPosition( tableElement[0] );
    		//Get the data array for this row
    		var aData = tableObj.fnGetData(aPos);
    		//make an ajax call to retrieve metadata
    		var layerId = aData[that.tableHeadingsObj.getColumnIndex("LayerId")];
    		var ajaxParams = {
  	    		type: "GET",
  	            url: "getFgdcTextHandler.jsp",
  	            data: "layerId=" + layerId,
  	            dataType: 'json',
  	            success: function(data){
    					var solrResponse = data["response"];
    					var totalResults = solrResponse["numFound"];
    					if (totalResults != 1)
    					{
    						alert("Request for FGDC returned " + totalResults +".  Exactly 1 was expected.");
    						return;
    					}
    					var doc = solrResponse["docs"][0];  // get the first layer object
    					var fgdcRawText = doc["FgdcText"];
    					var fgdcText = unescape(fgdcRawText);  // text was escaped on ingest into Solr
    					var parser = new DOMParser();
    				    var fgdcDocument = parser.parseFromString(fgdcText,"text/xml");
    					var xsl = loadXMLDoc("FGDC_Classic_for_Web_body.xsl");
    					var xsltProcessor = new XSLTProcessor();
    					xsltProcessor.importStylesheet(xsl);
    					resultDocument = xsltProcessor.transformToFragment(fgdcDocument, document);
    					document.getElementById("dialogDiv").innerHTML = "";  // delete previously displayed metadata
    					document.getElementById("dialogDiv").appendChild(resultDocument);
    					jQuery('#dialogDiv').width("550");
    					jQuery("#dialogDiv").dialog({ zIndex: 9999, width: 560, height: 400 });
  	            },
  	            error: function() {throw new Error("The attempt to retrieve FGDC layer information failed.");}
  	    };
  	    jQuery.ajax(ajaxParams);
};
*/
