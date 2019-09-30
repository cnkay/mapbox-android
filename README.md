# Android Mapping And Navigation /w Mapbox SDK

Mapbox Optimization API, Route API and Navigation SDK implementation on Android<br>
Supports 22 API level and above.

### build.gradle

```gradle
dependencies {
...
api 'com.google.android.material:material:1.1.0-alpha10'
implementation 'com.mapbox.mapboxsdk:mapbox-android-navigation-ui:0.41.0'
implementation 'com.mapbox.mapboxsdk:mapbox-android-sdk:8.3.0'
...
}
```

### AndroidManifest.xml
```xml
...
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" /> 
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" /> 
<uses-permission android:name="android.permission.INTERNET" /> 
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" /> 
...
```


### string.xml
```xml
<resources>
 ...
  <string name="access_token"><!-- MAPBOX ACCESS TOKEN HERE --></string>
 ...
</resources>
```
for more information [Mapbox Access Token](https://docs.mapbox.com/help/how-mapbox-works/access-tokens/)


### Attention
Navigation has own simulation for debugging, for disable simulation;
```java
private NavigationViewOptions setupOptions(DirectionsRoute directionsRoute) {
        dropoffDialogShown = false;
        NavigationViewOptions.Builder options = NavigationViewOptions.builder();
        options.directionsRoute(directionsRoute)
                .navigationListener(this)
                .progressChangeListener(this)
                .routeListener(this)
                .shouldSimulateRoute(true); //Simulation
        return options.build();
    }

```

### Screenshots from application

#### Permission
---------------
![alt text](https://github.com/cnkay/mapbox-android/blob/master/images/permission.png)

#### Route
---------------
![alt text](https://github.com/cnkay/mapbox-android/blob/master/images/route.png)

#### OnMapLongClick
-------------------
![alt text](https://github.com/cnkay/mapbox-android/blob/master/images/refresh_waypoints.png)

#### Complicated Routes
-----------------------
![alt text](https://github.com/cnkay/mapbox-android/blob/master/images/complicated_routes.png)

#### Navigation
----------------
![alt text](https://github.com/cnkay/mapbox-android/blob/master/images/on_navigation.png)

#### Arrives Waypoint
-------------------
![alt text](https://github.com/cnkay/mapbox-android/blob/master/images/arrives_waypoint.png)

Sorry for black icons and markers, its all about emulator rendering crap.

### For more information...
[Mapbox](https://www.mapbox.com/)<br>
[Mapbox Maps SDK](https://docs.mapbox.com/android/maps/overview/)<br>
[Mapbox Navigation SDK](https://docs.mapbox.com/android/navigation/overview/)<br>
[Mapbox Optimization API](https://docs.mapbox.com/android/java/overview/optimization/)<br>
