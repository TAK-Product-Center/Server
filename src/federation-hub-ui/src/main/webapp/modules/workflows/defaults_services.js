
'use strict';

angular.module('roger_federation.Workflows')

.factory('DefaultsService', function($http, ConfigService) {
    var service = {};
    
    service.initiateOntologiesRetrieval = function() {
    	return $http.get(
			ConfigService.getServerBaseUrlStr() + 'ontology').then(
				function(res) {
				    return res.data;
				}, function(reason) {
				    throw reason; 
				});
    }
    
    service.initiateDefaultsRetrieval = function(datasetName) {
    	var url = ConfigService.getServerBaseUrlStr() + 'defaults/';
    	return $http.get(url, {params: {datasetName: datasetName}}
			).then(
				function(res) {
					var itemCache = {};
					var data = res.data;
					var i;
					for(i = 0; i < data.length; i++) {
						if(data[i].ownerItem.id === undefined) {
							data[i].ownerItem = itemCache[data[i].ownerItem];							
						} else {
							itemCache[data[i].ownerItem.id] = data[i].ownerItem;
						}
						
						if(data[i].valueItem.id === undefined) {
							data[i].valueItem = itemCache[data[i].valueItem];							
						} else {
							itemCache[data[i].valueItem.id] = data[i].valueItem;
						}
					}
				    return data;
				}, function(reason) {
				    throw reason;
				});
    }
    
    service.initiateDefaultsImport = function(datasetName) {
    	return $http(
			{
			    method: 'POST',
			    url: ConfigService.getServerBaseUrlStr() + 'defaults/import',
			    data: datasetName,
			    transformRequest: [],
			    headers: {
			    	'Content-Type': 'application/json'
			    }
			}).then(
				function(res) {
					return res.data;
				}, function(reason) {
				    throw reason;
				}
			);
    }
    
    service.initiateDefaultRemoval = function(defaultID) {
    	return $http.delete(
			ConfigService.getServerBaseUrlStr() + 'defaults/remove/' + defaultID).then(
				function(res) {
					return res.data;
				}, function(reason) {
				    throw reason;
				});
    }
   
    return service;
});