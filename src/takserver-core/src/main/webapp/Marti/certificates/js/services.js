var services = angular.module('certificateManagerServices', []).factory('FederateCertificatesService', function($resource) {
  return $resource('/Marti/api/federatecertificates/:id', { id: '@_id' }, {
	  		'query': {method: "GET", isArray: false}
  		}
  );
});

services.factory('FederateCAGroupsService', function($resource){
      return $resource('/Marti/api/federatecagroups/:caId', {}, {
                 'query' : {method: 'GET', isArray: false}
            }
      );
    });
