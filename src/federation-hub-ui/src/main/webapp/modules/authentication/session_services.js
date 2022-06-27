
'use strict';

angular.module('roger_federation.Authentication')
.service('Session', function($cookieStore) {
    this.create = function(username, clientId, activities) {
	this.username = username;
	this.clientId = clientId;
	this.activities = activities;
	$cookieStore.put("session", this);
    };

    this.destroy = function() {
	this.clientId = null;
	this.activities = null;
	this.username = null;
	$cookieStore.remove("session");
    };

    return this;
});
