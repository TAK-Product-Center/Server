# angular-uuid4

Angular service that generates [RFC4122](http://www.ietf.org/rfc/rfc4122.txt) version 4 UUIDs / GUIDs.

Sample UUID: `f7e81995-1a52-48a4-88d1-f979e1917b29`

Based on pnegri's npm package: <https://github.com/pnegri/uuid-js>

## Usage

Require the module in your app and call `uuid4.generate()`.

Example:

``` javascript
// add the uuid4 module to your app
myapp = angular.module('myapp', ['uuid4']);

// inject it into your component
myapp.factory('FancyFactory', function(uuid4){
  return {
    codeThatNeedsUUID: function() {
      return "Look ma! I'm unique: " + uuid4.generate();
    }
  };
});
```

You can also validate uuids.

```javascript
uuid4.validate('ded6dd9e-49d9-485b-bac1-da0ca0ae9d70')
// true

uuid4.validate('f81d4fae-7dec-11d0-a765-00a0c91e6bf6')
// false, because it's a v1 uuid

uuid4.validate('monkeys!')
// false
```

This returns true if the uuid is a valid v4 uuid.

## Collisions

There are [reports](http://stackoverflow.com/questions/6906916/collisions-when-generating-uuids-in-javascript) of `Math.random()` not working properly on some systems. This may causes collisions (UUIDs that are the same). 
