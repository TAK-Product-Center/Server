var services = angular.module('pluginManagerServices', []);

services.factory('AllPluginsInfoService', function($resource) {
    return $resource('/Marti/api/plugins/info/all');
});

services.factory('PluginStatusService', function($resource) {
  return $resource('/Marti/api/plugins/info/status', {name:'@name', status: '@status'}, {});
});

services.factory('AllPluginStatusService', function($resource) {
  return $resource('/Marti/api/plugins/info/all/status', {status: '@status'}, {});
});
