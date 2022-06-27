var services = angular.module('injectorManagerServices', []).factory('InjectorManagerService', function($resource) {
  return $resource('/Marti/api/injectors/cot/uid', { }, {
	  		'query': {method: "GET", isArray: false}
  		}
  );
});

services.factory('UIDSearchService', function($resource) {
	  return $resource('/Marti/api/uidsearch', {}, {
		  'query': {method: "GET", isArray: false}
	  });
	});
