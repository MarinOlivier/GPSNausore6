package com.gpsnausore.oliviermarin.gpsnausore6;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.MathUtils;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.StepManeuver;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteLegProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static android.content.Context.LOCATION_SERVICE;
import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NavigationMap.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link NavigationMap#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NavigationMap extends Fragment implements OnMapReadyCallback, PermissionsListener {

    private OnFragmentInteractionListener mListener;

    private View myView;

    private MapView mapView;
    private MapboxMap mapboxMap;
    // variables for adding location layer
    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;
    // variables for calculating and drawing a route
    private DirectionsRoute currentRoute;
    private static final String TAG = "DirectionsActivity";
    private NavigationMapRoute navigationMapRoute;
    private DirectionsRoute directionsRoute;
    // variables needed to initialize navigation
    private Button button;

    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;
    private String geojsonSourceLayerId = "geojsonSourceLayerId";

    private Point destinationPoint;
    private Point originPoint;

    private FloatingActionButton fab_nav_start;
    private FloatingActionButton fab_location_search;

    private MapboxNavigation navigation;
    private NavigationCamera camera;

    private LocationManager mLocationManager;

    private boolean navIsStarted = false;

    public NavigationMap() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment NavigationMap.
     */
    // TODO: Rename and change types and number of parameters
    public static NavigationMap newInstance(String param1, String param2) {
        NavigationMap fragment = new NavigationMap();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ((savedInstanceState != null) && (savedInstanceState.getSerializable("navigationMapRoute") != null)) {
            navigationMapRoute=(NavigationMapRoute)savedInstanceState.getSerializable("navigationMapRoute");
        }

    }

    @SuppressLint("RestrictedApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        Mapbox.getInstance(Objects.requireNonNull(getContext()), getString(R.string.access_token));


        super.onCreateView(inflater, container, savedInstanceState);
        final View view = inflater.inflate(R.layout.fragment_navigation_map, container, false);



        //setContentView(R.layout.activity_main);
        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        fab_nav_start = view.findViewById(R.id.fab_nav_start);
        fab_nav_start.setVisibility(View.INVISIBLE);


        myView = view;
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @SuppressLint("MissingPermission")
    private Location getLastKnownLocation() {
        mLocationManager = (LocationManager)getContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
             Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(getString(R.string.navigation_guidance_day), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                Location lastKnowLocation = getLastKnownLocation();
                initSearchFab();
                setUpSource(style);
                enableLocationComponent(style);
                addDestinationIconSymbolLayer(style);
                Toast.makeText(getActivity(), "Steady ?", Toast.LENGTH_SHORT).show();
                if (lastKnowLocation != null) {
                    LatLng pos = null;
                    pos = new LatLng(lastKnowLocation.getLatitude(),lastKnowLocation.getLongitude());

                    mapboxMap.setCameraPosition(new CameraPosition.Builder()
                            .target(pos)
                            .zoom(15)
                            .build());
                    Toast.makeText(getActivity(), "GO!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }



    private void addDestinationIconSymbolLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage("destination-icon-id",
                BitmapFactory.decodeResource(this.getResources(), R.drawable.mapbox_marker_icon_default));
        GeoJsonSource geoJsonSource = new GeoJsonSource("destination-source-id");
        loadedMapStyle.addSource(geoJsonSource);
        SymbolLayer destinationSymbolLayer = new SymbolLayer("destination-symbol-layer-id", "destination-source-id");
        destinationSymbolLayer.withProperties(
                iconImage("destination-icon-id"),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
        );
        loadedMapStyle.addLayer(destinationSymbolLayer);
    }

    @SuppressLint("RestrictedApi")
    private void initSearchFab() {

        fab_location_search = myView.findViewById(R.id.fab_location_search);
        fab_location_search.setVisibility(View.VISIBLE);

        fab_location_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                assert Mapbox.getAccessToken() != null;
                Intent intent = new PlaceAutocomplete.IntentBuilder()
                        .accessToken(Mapbox.getAccessToken())
                        .placeOptions(PlaceOptions.builder()
                                .backgroundColor(Color.parseColor("#EEEEEE"))
                                .limit(10)
                                .build(PlaceOptions.MODE_CARDS))
                        .build(getActivity());
                startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
            }
        });
    }

    @SuppressLint("RestrictedApi")
    private void initNavFab(){
        fab_location_search.setVisibility(View.INVISIBLE);
        fab_nav_start.setVisibility(View.VISIBLE);
        fab_nav_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNavigation(directionsRoute);
            }
        });
    }

    private JSONObject serializeInstruction(double distance, StepManeuver maneuver) throws JSONException {
        int roundedDistance = (int) ((((int)distance + 4) / 5) * 5);
        return new JSONObject("{'distance':"+roundedDistance+", " +
                "'instruction':'"+maneuver.instruction()+"', " +
                "'type':'"+maneuver.type()+"', " +
                "'modifier':'"+maneuver.modifier()+"', " +
                "'exit':'"+maneuver.exit()+"'}");
    }

    private void startNavigation(DirectionsRoute route) {
        if (locationComponent != null) {
            navigation = new MapboxNavigation(getContext(), getString(R.string.access_token));
            navigation.addProgressChangeListener(new ProgressChangeListener() {
                @Override
                public void onProgressChange(Location location, RouteProgress routeProgress) {
                    try {
                            Log.d("MY_DEBUG", String.valueOf(routeProgress.currentLegProgress()));

                        if (((MainActivity) Objects.requireNonNull(getActivity())).isConnected()) {
                            MainActivity.ConnectedThread mainThread = ((MainActivity)getActivity()).getMainThread();
                            mainThread.write(serializeInstruction(routeProgress.currentLegProgress().distanceRemaining(),
                                    routeProgress.currentLegProgress().currentStep().maneuver()).toString().getBytes());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
            navigation.addOffRouteListener(new OffRouteListener() {
                @Override
                public void userOffRoute(Location location) {
                    Log.d("MY_DEBUG", "Wrong route !");
                    navigation.stopNavigation();
                    restartNav();
                }
            });
            LocationEngine locationEngine = LocationEngineProvider.getBestLocationEngine(getContext());
            navigation.setLocationEngine(locationEngine);
            navigationMapRoute.addProgressChangeListener(navigation);
            navigationMapRoute.addRoute(route);
            navigation.startNavigation(route);
            camera = new NavigationCamera(mapboxMap, navigation, locationComponent);
            camera.start(route);
            navIsStarted = true;
        }
    }

    private void restartNav() {
        originPoint =  Point.fromLngLat(getLastKnownLocation().getLongitude(), getLastKnownLocation().getLatitude());
        navigationMapRoute = getRoute(originPoint, destinationPoint);
        startNavigation(directionsRoute);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {

            // Retrieve selected location's CarmenFeature
            CarmenFeature selectedCarmenFeature = PlaceAutocomplete.getPlace(data);

            // Create a new FeatureCollection and add a new Feature to it using selectedCarmenFeature above.
            // Then retrieve and update the source designated for showing a selected location's symbol layer icon

            if (mapboxMap != null) {
                Style style = mapboxMap.getStyle();
                if (style != null) {
                    GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
                    if (source != null) {
                        source.setGeoJson(FeatureCollection.fromFeatures(
                                new Feature[] {Feature.fromJson(selectedCarmenFeature.toJson())}));
                    }

                    // Move map camera to the selected location
                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(((Point) selectedCarmenFeature.geometry()).latitude(),
                                            ((Point) selectedCarmenFeature.geometry()).longitude()))
                                    .zoom(14)
                                    .build()), 4000);

                    destinationPoint = Point.fromLngLat(((Point) selectedCarmenFeature.geometry()).longitude(), ((Point) selectedCarmenFeature.geometry()).latitude());
                    originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                            locationComponent.getLastKnownLocation().getLatitude());

                    if (source != null) {
                        source.setGeoJson(Feature.fromGeometry(destinationPoint));
                    }
                    navigationMapRoute = getRoute(originPoint, destinationPoint);
                    initNavFab();
                }
            }
        }
    }


    private void setUpSource(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addSource(new GeoJsonSource(geojsonSourceLayerId));
    }

    private NavigationMapRoute getRoute(Point origin, Point destination) {
        origin = Point.fromLngLat(getLastKnownLocation().getLongitude(), getLastKnownLocation().getLatitude());
        NavigationRoute.builder(getContext())
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        // You can get the generic HTTP info about the response
                        Log.d(TAG, "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e(TAG, "No routes found");
                            return;
                        }

                        currentRoute = response.body().routes().get(0);

                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);

                        directionsRoute = currentRoute;
                    }
                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                    }
                });
        return navigationMapRoute;
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
// Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(getContext())) {
// Activate the MapboxMap LocationComponent to show user location
// Adding in LocationComponentOptions is also an optional parameter
            LocationComponentActivationOptions locationComponentActivationOptions = LocationComponentActivationOptions
                    .builder(getContext(), loadedMapStyle)
                    .useDefaultLocationEngine(true)
                    .build();
            locationComponent = mapboxMap.getLocationComponent();
            //locationComponent.activateLocationComponent(getContext(), loadedMapStyle, true);
            locationComponent.activateLocationComponent(locationComponentActivationOptions);
            locationComponent.setLocationComponentEnabled(true);
// Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(getActivity());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(getContext(), R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent(mapboxMap.getStyle());
        } else {
            Toast.makeText(getContext(), R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            getActivity().finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        Timber.tag(TAG).w("On resume");
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        Timber.tag(TAG).w("On pause");

        if (navIsStarted) {
            navigation.removeProgressChangeListener(null);
            navigation.removeOffRouteListener(null);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
        if (navIsStarted) {
            navigation.removeProgressChangeListener(null);
            navigation.removeOffRouteListener(null);
        }
        navIsStarted = false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        //navigation.stopNavigation();
        if (navIsStarted) {
            navigation.removeProgressChangeListener(null);
            navigation.removeOffRouteListener(null);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
