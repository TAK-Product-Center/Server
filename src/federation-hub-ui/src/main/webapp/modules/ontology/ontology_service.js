
'use strict';
angular.module('roger_federation.OntologyService', []);
angular.module('roger_federation.OntologyService')

.factory('OntologyService', function($rootScope, $http, ConfigService) {
  var OntologyService = {};
  var fusekiURL = 'http://' + ConfigService.getUiServerInfo().fuseki.uri + '/';


  var namespaces = [
    'owl: <http://www.w3.org/2002/07/owl#>',
    'rdfs: <http://www.w3.org/2000/01/rdf-schema#>',
    'skos: <http://www.w3.org/2004/02/skos/core#>'
  ];

  var prefixes = function() {
    var pre = '';
    for (var i = 0; i < namespaces.length; i++) {
      pre += 'PREFIX ';
      pre += namespaces[i];
      pre += ' ';
    }
    return pre;
  };


  OntologyService.queryClasses = function(dataset, searchString, maxResults) {
    var query = prefixes() +
    'select ?uri ?label {' +
      '?uri a owl:Class .' + //      #  <- Only Select Classes
      '?uri rdfs:label ?label .' +
       'FILTER regex(str(?label), "' + searchString + '", "i")' +
    '}' +
    'limit ' + maxResults;

    return OntologyService.queryFusekiEndpoint(dataset, query).then(function(result) {
      return result;
    }, function(reason) {
      throw reason;
    });
  };

  OntologyService.queryClassInstances = function(dataset, uri) {
    var query = prefixes() +
    'SELECT DISTINCT ?uri' +
    'WHERE {' +
    '  ?uri a <' + uri + '>  .' +
    '}' +
    'limit ' + 250;

    return OntologyService.queryFusekiEndpoint(dataset, query).then(function(result) {
      return result;
    }, function(reason) {
      throw reason;
    });
  };

  OntologyService.describeURI = function(dataset, uri) {
    var query = ' DESCRIBE <' + uri + '>  ';
    return OntologyService.queryFusekiEndpoint(dataset, query).then(function(result) {
      return result;
    }, function(reason) {
      throw reason;
    });
  };

  OntologyService.queryClassComment = function(dataset, uri) {
    var query = prefixes() +
    'SELECT ?comment ?uri' +
    'WHERE {' +
    '  <' + uri + '> rdfs:comment ?comment .' +
    '}' +
    'limit 1';

    return OntologyService.queryFusekiEndpoint(dataset, query).then(function(result) {
      if (result.data.results.bindings.length > 0) {
        return result.data.results.bindings[0].comment.value;
      } else {
        return "None";
      }
    }, function(reason) {
      throw reason;
    });
  };


  OntologyService.getDatasetNames = function() {
    return $http({
      method: 'GET',
      url: fusekiURL +  '$/datasets',
      transformRequest: [],
      headers: {
        'Content-type': 'application/x-www-form-urlencoded',
        'Accept': 'application/sparql-results+json'
      },
      params: {
        format: "json"
      }
    }).then(function(result) {
      var datasetNames = [];
      result.data.datasets.forEach ( function (dataset) {
        datasetNames.push(dataset["ds.name"].substring(1));
      });
      return datasetNames;
    }, function(reason) {
      throw reason;
    });
  };


  OntologyService.queryFusekiEndpoint = function(dataset, sparqlQuery) {
    return $http({
      method: 'POST',
      url: fusekiURL + dataset + '/query',
      // data: encodeURIComponent(query),
      transformRequest: [],
      headers: {
        'Content-type': 'application/x-www-form-urlencoded',
        'Accept': 'application/sparql-results+json'
      },
      params: {
        query: sparqlQuery,
        format: "json"
      }
    });

  };

  return OntologyService;


// getInstanceReferringTypesQuery
//
// SELECT (COUNT(?val) AS ?valCount) ?valType
//       WHERE {
//         ?instance a <file://securboration/main/resources/owl/core/military/1.0#USAirForceBase>.
//         ?instance ?prop ?val .
//         BIND (datatype(?val) AS ?valType) .
//       }
//       GROUP BY ?valType
//       ORDER BY DESC(?valCount)
//       LIMIT 100

// getOrderedClassTypeRelationQuery
//
//   SELECT (count(?instance) AS ?count) ?prop
//         WHERE {
//           ?instance a <file://securboration/main/resources/owl/core/military/1.0#USAirForceBase> .
//           ?instance ?prop ?val .
//
//         }
//         GROUP BY ?prop
//         ORDER BY DESC(?count)
//         LIMIT 100


});
