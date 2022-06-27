
'use strict';

angular.module('roger_federation.Authentication')

.factory('AuthenticationService', ["$http", "$cookieStore", "Session",
                                   function ($http, $cookieStore, Session) {
    var authService = {};

    var userId = "unknown";

    authService.login = function(credentials) {
	/*
	return $http
		.put(ConfigService.getServerBaseUrlStr()+'access_control/login', {
				"username" : credentials.username,
				"password" : credentials.password
		})
		.then(function (response) {
	 */
	userId = credentials.username;
	Session.create(credentials.username, credentials.username, []);
	return {
	    username: credentials.username,
	    clientId: credentials.username
	};
	/*
		}, function (reason) {
			var message = "";
			if(reason.status === 0) {
				message = "Cannot contact server";
			} else if(reason.status === 400 || reason.status === 401) {
				message = "Invalid username or password.";
			} else {
				message = reason.status + ": " + reason.statusText;
			};
			throw message;
		});
	 */
    };


    authService.getUserId = function() {
	return userId;
    };

    authService.isAuthenticated = function() {
	if(!Session.clientId && $cookieStore.get("session")) {
	    var session = $cookieStore.get("session");
	    userId = session.username;
	    Session.create(session.username, session.clientId, session.activities);
	}

	return !!Session.clientId;
    };

    authService.logout = function() {
	Session.destroy();
	return true;
    };

    return authService;
}]);
