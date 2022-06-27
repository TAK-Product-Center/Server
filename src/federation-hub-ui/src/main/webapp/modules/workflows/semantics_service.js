
'use strict';

angular.module('roger_federation.Workflows')

.factory('SemanticsService', function($http, ConfigService) {
    var service = {};
    
    service.getLowLevelSubjects = function(datasetName) {
    	var url = ConfigService.getServerBaseUrlStr() + 'semantics/level/low/subjects';
    	return $http.get(url, {params: {datasetName: datasetName}})
			.then(
				function(res) {
				    return res.data;
				}, function(reason) {
				    throw reason;
				});
    }
    
    service.getLowLevelPredicates = function(datasetName, subject, triples) {
    	var url = ConfigService.getServerBaseUrlStr() + 'semantics/level/low/predicates';
    	return $http.post(url, JSON.stringify({entity: subject, triples: triples}), {params: {datasetName: datasetName}})
			.then(
				function(res) {
				    return res.data;
				}, function(reason) {
				    throw reason;
				});
    }
    
    service.getLowLevelObjects = function(datasetName, predicateURI) {
    	var url = ConfigService.getServerBaseUrlStr() + 'semantics/level/low/objects';
    	return $http.get(url, {params: {datasetName: datasetName, predicateURI: predicateURI}})
			.then(
				function(res) {
				    return res.data;
				}, function(reason) {
				    throw reason;
				});
    }
    
    service.getLowLevelSubclasses = function(datasetName, element, triples) {
    	var url = ConfigService.getServerBaseUrlStr() + 'semantics/level/low/subclasses';
    	return $http.post(url, JSON.stringify({entity: element, triples: triples}), {params: {datasetName: datasetName}})
			.then(
				function(res) {
				    return res.data;
				}, function(reason) {
				    throw reason;
				});
    }
    
    service.getLowLevelClasses = function(datasetName) {
    	var url = ConfigService.getServerBaseUrlStr() + 'semantics/level/low/classes';
    	return $http.get(url, {params: {datasetName: datasetName}})
			.then(
				function(res) {
				    return res.data;
				}, function(reason) {
				    throw reason;
				});
    }
    
    service.createUpdateRequest = function(request) {
    	var url = ConfigService.getServerBaseUrlStr() + 'semantics/requests';
    	return $http.post(url, JSON.stringify(request))
			.then(
				function(res) {
				    return res.data;
				}, function(reason) {
				    throw reason;
				});
    }
    
    service.getRequestsForWorkflow = function(workflowID) {
    	var url = ConfigService.getServerBaseUrlStr() + 'semantics/requests';
    	return $http.get(url, {params: {workflowID: workflowID}})
			.then(
				function(res) {
				    return res.data;
				}, function(reason) {
				    throw reason;
				});
    }
    
    service.generateSPARQL = function(request, format) {
    	var noOPTransform = function(data, headersGetter, status) {
    		return data;
    	}
    	var url = ConfigService.getServerBaseUrlStr() + 'semantics/sparql';
    	return $http.post(url, JSON.stringify(request), {params: {format: format}, transformResponse: noOPTransform})
			.then(
				function(res) {
				    return res.data;
				}, function(reason) {
				    throw reason;
				});
    }
    
    return service;
});