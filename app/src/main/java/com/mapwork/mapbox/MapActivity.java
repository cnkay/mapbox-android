package com.mapwork.mapbox;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.button.MaterialButton;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.optimization.v1.MapboxOptimization;
import com.mapbox.api.optimization.v1.models.OptimizationResponse;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnMapClickListener, MapboxMap.OnMapLongClickListener, PermissionsListener {

    private static final String ICON_GEOJSON_SOURCE_ID = "icon-source-id";
    private static final String FIRST = "first";
    private static final String ANY = "any";
    private static final String LAST = "last";
    private static final String TEAL_COLOR = "#23D2BE";
    private static final float POLYLINE_WIDTH = 5;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private MaterialButton navigationButton;
    private LocationComponent locationComponent;
    private PermissionsManager permissionsManager;
    private DirectionsRoute optimizedRoute;
    private MapboxOptimization optimizedClient;
    private ArrayList<Point> points = new ArrayList<>();
    private Point origin;
    private boolean buttonVisibility = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getToken();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.maps);
        navigationButton = findViewById(R.id.startButton);
        navigationButton.setVisibility(View.INVISIBLE);
        mapView.onCreate(savedInstanceState);


        navigationButton.setOnClickListener((View v) -> { //new ViewOnClickListener() to Lambda Expression
            Intent intent = new Intent();
            intent.setClass(MapActivity.this, NavigationActivity.class);
            intent.putExtra("points", points);
            startActivityForResult(intent,RESULT_OK);
        });

        mapView.getMapAsync(this);
    }
    public void truncateList(ArrayList<Point> points){
        for(Point p : points)
            points.remove(p);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==RESULT_OK)
            truncateList(points);
    }

    public void getToken() {
        Mapbox.getInstance(this, getString(R.string.access_token));
    }

    @Override
    @SuppressWarnings({"MissingPermission"})
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "Konum bilginiz gerekiyor.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent(mapboxMap.getStyle());
        } else {
            Toast.makeText(getApplicationContext(), "GPS izni alınamadı!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        if (!buttonVisibility) {
            navigationButton.setVisibility(View.VISIBLE);
            buttonVisibility = true;
        }
        if (alreadyTwelveMarkersOnMap()) {
            Toast.makeText(this, "Maksimum 12 nokta işaretlenebilir!", Toast.LENGTH_SHORT).show();
        } else {
            Style style = mapboxMap.getStyle();
            if (style != null) {
                addDestinationMarker(style, point);
                addPointToStopsList(point);
                getOptimizedRoute(style, points);
            }
            Point destinationPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
            points.add(destinationPoint);
        }
        return true;
    }

    @Override
    public boolean onMapLongClick(@NonNull LatLng point) {
        points.clear();
        if (mapboxMap != null) {
            Style style = mapboxMap.getStyle();
            if (style != null) {
                resetDestinationMarkers(style);
                removeOptimizedRoute(style);
                addFirstStopToStopsList();
                return true;
            }
        }
        return false;
    }

    private void resetDestinationMarkers(@NonNull Style style) {
        GeoJsonSource optimizedLineSource = style.getSourceAs(ICON_GEOJSON_SOURCE_ID);
        if (optimizedLineSource != null) {
            optimizedLineSource.setGeoJson(Point.fromLngLat(origin.longitude(), origin.latitude()));
        }
    }
    private void removeOptimizedRoute(@NonNull Style style) {
        GeoJsonSource optimizedLineSource = style.getSourceAs("optimized-route-source-id");
        if (optimizedLineSource != null) {
            optimizedLineSource.setGeoJson(FeatureCollection.fromFeatures(new Feature[] {}));
        }
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        //When map is ready, create other components
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, (@NonNull Style style) -> {
                    enableLocationComponent(style);
                    addDestinationIconLayer(style);
                    initOptimizedRouteLineLayer(style);
                    addFirstStopToStopsList();
                    mapboxMap.addOnMapClickListener(MapActivity.this);
                    mapboxMap.addOnMapLongClickListener(MapActivity.this);
                }
        );
        // Zoom preference for user experience
        // this.mapboxMap.setMinZoomPreference(15);
    }

    private void addDestinationIconLayer(@NonNull Style loadedMapStyle) {

        loadedMapStyle.addImage("icon-image", BitmapFactory.decodeResource(
                this.getResources(), R.drawable.mapbox_marker_icon_default));

        // Add the source to the map

        loadedMapStyle.addSource(new GeoJsonSource(ICON_GEOJSON_SOURCE_ID,
                FeatureCollection.fromFeatures(new Feature[]{})));

        loadedMapStyle.addLayer(new SymbolLayer("icon-layer-id", ICON_GEOJSON_SOURCE_ID).withProperties(
                iconImage("icon-image"),
                iconSize(1f),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
                iconOffset(new Float[]{0f, -7f})
        ));

    }

    // Optimization
    private void initOptimizedRouteLineLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addSource(new GeoJsonSource("optimized-route-source-id"));
        loadedMapStyle.addLayerBelow(new LineLayer("optimized-route-layer-id", "optimized-route-source-id")
                .withProperties(
                        lineColor(Color.parseColor(TEAL_COLOR)),
                        lineWidth(POLYLINE_WIDTH)
                ), "icon-layer-id");
    }
    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) { //Check GPS permission is granted

            this.locationComponent = mapboxMap.getLocationComponent();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                permissionsManager = new PermissionsManager(this);
                permissionsManager.requestLocationPermissions(this);
            }
            else {

                //Activate with options
                LocationComponentOptions locationComponentOptions = LocationComponentOptions.builder(this)
                        //.layerBelow(String.valueOf(0))
                        //.foregroundDrawable(R.drawable.map_marker_light)
                        .elevation(5)
                        .accuracyColor(Color.RED)
                        //.bearingTintColor(Color.YELLOW)
                        .accuracyAlpha(.6f)
                        .build();

                LocationComponentActivationOptions locationComponentActivationOptions = LocationComponentActivationOptions
                        .builder(this, loadedMapStyle)
                        .locationComponentOptions(locationComponentOptions)
                        .build();


                locationComponent.activateLocationComponent(locationComponentActivationOptions);
                locationComponent.setLocationComponentEnabled(true);
                locationComponent.setCameraMode(CameraMode.TRACKING);
                locationComponent.setRenderMode(RenderMode.COMPASS);

                Point originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                        locationComponent.getLastKnownLocation().getLatitude());
                this.points.add(originPoint); //Çok önemli, başlangıç konumu listeye kaydediliyor
            }
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private Point getOriginPoint() {
        Point originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLatitude());
        return originPoint;
    }
    //********************************************

    // Optimization API
    private void addDestinationMarker(@NonNull Style style, LatLng point) {
        List<Feature> destinationMarkerList = new ArrayList<>();
        for (Point singlePoint : points) {
            destinationMarkerList.add(Feature.fromGeometry(
                    Point.fromLngLat(singlePoint.longitude(), singlePoint.latitude())));
        }
        destinationMarkerList.add(Feature.fromGeometry(Point.fromLngLat(point.getLongitude(), point.getLatitude())));
        GeoJsonSource iconSource = style.getSourceAs(ICON_GEOJSON_SOURCE_ID);
        if (iconSource != null) {
            iconSource.setGeoJson(FeatureCollection.fromFeatures(destinationMarkerList));
        }
    }

    private void addPointToStopsList(LatLng point) {
        points.add(Point.fromLngLat(point.getLongitude(), point.getLatitude()));
    }

    @SuppressWarnings({"MissingPermission"})
    private void addFirstStopToStopsList() {
        // Set first stop
        origin = getOriginPoint();
        points.add(origin);
    }

    private void getOptimizedRoute(@NonNull final Style style, List<Point> coordinates) {
        optimizedClient = MapboxOptimization.builder()
                .source(ANY)
                .destination(ANY)//Any
                .coordinates(coordinates)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .roundTrip(true)
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .accessToken(Mapbox.getAccessToken() != null ? Mapbox.getAccessToken() : getString(R.string.access_token))
                .build();
        /*
        roundtrip	source	destination	supported
        true	    first	    last	yes
        true	    first	    any	    yes
        true	    any	        last	yes
        true	    any	        any	    yes
        false	    first	    last	yes
        false	    first	    any	    no
        false	    any	        last	no
        false	    any	        any	    no
        */

        optimizedClient.enqueueCall(new Callback<OptimizationResponse>() {
            @Override
            public void onResponse(Call<OptimizationResponse> call, Response<OptimizationResponse> response) {
                if (!response.isSuccessful()) {
                    Timber.d("Bağlantı başarısız");
                    Toast.makeText(MapActivity.this, "Başarısız", Toast.LENGTH_SHORT).show();
                } else {
                    if (response.body() != null) {
                        List<DirectionsRoute> routes = response.body().trips();
                        if (routes != null) {
                            if (routes.isEmpty()) {
                                Timber.d("%s size = %s", "Bağlantı başarılı ama rota yok", routes.size());
                                Toast.makeText(MapActivity.this, "Bağlantı başarılı ama rota yok",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                // Get most optimized route from API response
                                optimizedRoute = routes.get(0);
                                drawOptimizedRoute(style, optimizedRoute);
                            }
                        } else {
                            Timber.d("list of routes in the response is null");
                            Toast.makeText(MapActivity.this, "Return null", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Timber.d("response.body() is null");
                        Toast.makeText(MapActivity.this, "Return null", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<OptimizationResponse> call, Throwable throwable) {
                Timber.d("Error: %s", throwable.getMessage());
            }
        });
    }

    private void drawOptimizedRoute(@NonNull Style style, DirectionsRoute route) {
        GeoJsonSource optimizedLineSource = style.getSourceAs("optimized-route-source-id");
        if (optimizedLineSource != null) {
            optimizedLineSource.setGeoJson(FeatureCollection.fromFeature(Feature.fromGeometry(
                    LineString.fromPolyline(route.geometry(), 6))));
        }
    }

    private boolean alreadyTwelveMarkersOnMap() {
        return points.size() == 12;
    }


}
