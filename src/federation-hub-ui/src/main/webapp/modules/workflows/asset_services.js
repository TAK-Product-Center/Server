
'use strict';

angular.module('roger_federation.Workflows')

.factory('AssetService', function($http, ConfigService) {
    var service = {};


    /**
     * Get Asset Filenames
     *
     * @return {Array}
     */
    service.getAssetFilenames = function() {
	return $http.get(
		ConfigService.getServerBaseUrlStr() + 'asset/filenames').then(
			function(res) {
			    return res.data;
			}, function(reason) {
			    throw reason;
			});
    };

    /**
     * Get Workflow Descriptors
     *
     * @return {Array}
     */
    service.getAssetFileDescriptors = function() {
	return $http.get(
		ConfigService.getServerBaseUrlStr() + 'asset/descriptors').then(
			function(res) {
			    return res.data;
			}, function(reason) {
			    throw reason;
			});
    };

    /**
     * Upload Asset
     *
     * @param {Object}
     *                content
     * @return {Boolean} true
     */
    service.uploadAssetFile = function(data) {
	var fd = new FormData();
	fd.append('name', data.name);
	fd.append('description', data.description);
	fd.append('creatorName', data.creatorName);
	fd.append('fileName', data.fileName);
	fd.append('contents', data.contents);

	return $http({
	    method: 'POST',
	    url: ConfigService.getServerBaseUrlStr() + 'asset',
	    data: fd,
	    transformRequest: angular.identity,
	    headers: {
		'Content-Type': undefined
	    }}).then(function(res) {
		return res.headers('Location');
	    }, function(reason) {
		throw reason;
	    });

    };


    /**
     * Delete Asset File
     *
     * @param {Object}
     *                filename
     * @return {Array}
     */
    service.deleteAssetFile = function(filename) {
	return $http.delete(
		ConfigService.getServerBaseUrlStr() + 'asset/' + filename).then(
			function(res) {
			    return res;
			}, function(reason) {
			    throw reason;
			});
    };


    /**
     * Update Asset File
     *
     * @param {Object}
     *                content
     * @return {Array}
     */
    service.updateAssetFile = function(content) {
	return $http({
	    method: 'PUT',
	    url: ConfigService.getServerBaseUrlStr() + 'asset' ,
	    data: JSON.stringify(content),
	    transformRequest: [],
	    headers: {
		'Content-Type': 'application/json'
	    }}).then(function(res) {
		return res.data;
	    }, function(reason) {
		throw reason;
	    });
    };

    /**
     * Get Asset File
     *
     * @param {Object}
     *                assetFileId
     * @return {Array}
     */
    service.getAssetFile = function(assetFileId) {
	return $http.get(
		ConfigService.getServerBaseUrlStr() + 'asset/' + assetFileId).then(
			function(res) {
			    return res.data;
			}, function(reason) {
			    throw reason;
			});
    };


    return service;
})
