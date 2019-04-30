# EVRoutePlanner
This API's primary purpose is to create a list of electric vehicle (EV) charging sites along a specified route, as global EV charging infrastructure remains in a nascent state of development. To accomplish this, it queries the OpenChargeMaps API for site information and the Google Directions API to create the route.

Additional functionality includes the ability to search for charging sites by country (ISO country code), by latitude and longitude, and near the user's location, the latter of which calls the Google Geolocation API for the location.

## Endpoints
```/routeplanner/go?startlat={start_latitude}&startlng={start_longitude}&endlat={end_latitude}&endlng={end_longitude}&maxresults={max_results}```

Searches for sites along a route that begins with a the specified starting and ending coordinates. This query requires a key as it potentially requires many calls to external APIs.

```/getakey?alias={alias}```

Allows the user to register an alias and get a key to access the API. (Note: at present, a key is not needed to use the API.)

```/openchargemap/bycountry?q={countryCode}&maxresults={maxResults}```

Searches by country.

```/openchargemap/bylatlong?latitude={latitude}&longitude={longitude}&distance={distance}&distanceunit={distance_unit}&levelid={level_id}&maxresults={max_results}```

Searches by latitude and longitude.

```/openchargemap/nearme```

Searches near the user's location.

