package org.OpenGeoPortal.Export.GeoCommons;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class SearchResponseJson {
/*{"totalResults": 1, "entries": [{"detail_link": "http://geocommons.com/overlays/218328", "pk": 218328, "type": "Overlay", 
	"title": "sde:GISPORTAL.GISOWNER01.USARAILSTATIONS04", "data_type": "Data Feed", "link": "http://geocommons.com/overlays/218328.json", 
	"contributor": {"name": "chrissbarnett", "uri": "http://geocommons.com/users/chrissbarnett"}, "geometry_types": "point", 
	"name": "sde:GISPORTAL.GISOWNER01.USARAILSTATIONS04", "created": "2012-03-21T20:15:57Z", "description": "test data", 
	"bbox": [25.68499, -122.98516, 47.94884, -70.66873], "feature_count": 2507, "unique_name": null, "icon_path": null, 
	"permissions": {"edit": true, "download": true, "view": true}, "layer_size": 5394854, 
	"tags": "(streetcars), and, automated, cable, car, cars, commuter, fixed-guideway, guideway, heavy, high-occupancy, inclined, lanes, light, local, monorail, plane, point, rail, railroad, railroads, railroads--united, states, states., stations., territories, transit, transportation, united, us, usa, vehicle", 
	"source": null, "author": {"name": "open geo portal", "uri": null}, "short_classification": null, "analyzable": true, "published": "2012-03-21T17:02:16-04:00", "id": "Overlay:218328"}], "itemsPerPage": 10}
	*/

	@JsonProperty("totalResults")
	int totalResults;


	@JsonProperty("entries")
	List<Entry> entries = new ArrayList<Entry>();
	
	public int getTotalResults() {
		return totalResults;
	}

	public void setTotalResults(int totalResults) {
		this.totalResults = totalResults;
	}

	public List<Entry> getEntries() {
		return entries;
	}

	public void setEntries(List<Entry> entries) {
		this.entries.addAll(entries);
	}
	
	public class Entry {
		@JsonProperty("detail_link")
		String detail_link;
		@JsonProperty("pk")
		String pk;
		@JsonProperty("type")
		String type;
		@JsonProperty("title")
		String title;
		@JsonProperty("data_type")
		String data_type;
		@JsonProperty("link")
		String link;
		@JsonProperty("name")
		String name;
		@JsonProperty("description")
		String description;
		/*@JsonProperty("bbox")
		ArrayList<Double> bbox;*/
		@JsonProperty("feature_count")
		int feature_count;
		@JsonProperty("unique_name")
		String unique_name;
		/*@JsonProperty("permissions")
		Permissions permissions;*/
		@JsonProperty("layer_size")
		int layer_size;
		@JsonProperty("published")
		String published;
		@JsonProperty("id")
		String id;
		/*
		Entry(){
			this.permissions = new Permissions();
		}*/
		
		public class Permissions {
			@JsonProperty("edit")
			Boolean edit;
			@JsonProperty("download")
			Boolean download;			
			@JsonProperty("view")
			Boolean view;
			
			public Boolean getEdit() {
				return edit;
			}
			public void setEdit(Boolean edit) {
				this.edit = edit;
			}
			public Boolean getDownload() {
				return download;
			}
			public void setDownload(Boolean download) {
				this.download = download;
			}
			public Boolean getView() {
				return view;
			}
			public void setView(Boolean view) {
				this.view = view;
			}
		}

		public String getDetail_link() {
			return detail_link;
		}

		public void setDetail_link(String detail_link) {
			this.detail_link = detail_link;
		}

		public String getPk() {
			return pk;
		}

		public void setPk(String pk) {
			this.pk = pk;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getData_type() {
			return data_type;
		}

		public void setData_type(String data_type) {
			this.data_type = data_type;
		}

		public String getLink() {
			return link;
		}

		public void setLink(String link) {
			this.link = link;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		/*public ArrayList<Double> getBbox() {
			return bbox;
		}

		public void setBbox(ArrayList<Double> bbox) {
			this.bbox = bbox;
		}*/

		public int getFeature_count() {
			return feature_count;
		}

		public void setFeature_count(int feature_count) {
			this.feature_count = feature_count;
		}

		public String getUnique_name() {
			return unique_name;
		}

		public void setUnique_name(String unique_name) {
			this.unique_name = unique_name;
		}

		/*public Permissions getPermissions() {
			return permissions;
		}

		public void setPermissions(Permissions permissions) {
			this.permissions = permissions;
		}*/

		public int getLayer_size() {
			return layer_size;
		}

		public void setLayer_size(int layer_size) {
			this.layer_size = layer_size;
		}

		public String getPublished() {
			return published;
		}

		public void setPublished(String published) {
			this.published = published;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}
}
