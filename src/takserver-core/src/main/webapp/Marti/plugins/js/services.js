var services = angular.module('pluginManagerServices', []);

services.factory('AllPluginsInfoService', function($resource) {
    return $resource('/Marti/api/plugins/info/all');
});

services.factory('PluginEnabledService', function($resource) {
  return $resource('/Marti/api/plugins/info/enabled', {name:'@name', status: '@status'}, {});
});

services.factory('PluginStartedService', function($resource) {
  return $resource('/Marti/api/plugins/info/started', {name:'@name', status: '@status'}, {});
});

services.factory('AllPluginStartedService', function($resource) {
  return $resource('/Marti/api/plugins/info/all/started', {status: '@status'}, {});
});

services.factory('PluginArchiveService', function($resource) {
  return $resource('/Marti/api/plugins/info/archive', {name:'@name', archiveEnabled: '@archiveEnabled'}, {});
});
