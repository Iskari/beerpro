package ch.beerpro.presentation.details.createrating;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.util.Arrays;
import java.util.List;

import ch.beerpro.R;

public class RatingPlaceActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnPoiClickListener {

    private GoogleMap mMap;
    private Place beerPlace;
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    private Location mLastKnownLocation;
    private final LatLng mDefaultLocation = new LatLng(47.3774337,8.4666756);

    private CameraPosition mCameraPosition;


    private FusedLocationProviderClient mFusedLocationProviderClient;
    private PlacesClient pClient;
    List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS, Place.Field.ID);




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        setContentView(R.layout.activity_rating_place);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        //Get current location
        //mGeoDataClient = Places.getGeoDataClient(this, null);
        // Construct a PlaceDetectionClient.
        //mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);




        // Initialize Places.
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        pClient = Places.createClient(getApplicationContext());

        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        assert autocompleteFragment != null;
        autocompleteFragment.setPlaceFields(placeFields);
        autocompleteFragment.setCountry("ch");

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                beerPlace = place;
                Log.i(getString(R.string.app_name), "Place: " + place.getName() + ", " + place.getLatLng());
                setBeerMarker(place.getLatLng(), place.getName() + "," + place.getAddress());
            }

            @Override
            public void onError(Status status) {
                Toast.makeText(getApplicationContext(), "An error occured:" + status, Toast.LENGTH_SHORT).show();
                Log.i(getString(R.string.app_name), "An error occurred: " + status);
            }
        });

        Button recenter = findViewById(R.id.recenterButton);
        recenter.setOnClickListener((v) ->{
            getDeviceLocation();
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude())));
        });

        Button ok = findViewById(R.id.okButton);
        ok.setOnClickListener((v) -> {
            Intent intent=new Intent();
            intent.putExtra("HASDATA", false);
            if(beerPlace != null) {
                intent.putExtra("HASDATA", true);
                intent.putExtra("ID", beerPlace.getId());
                intent.putExtra("ADDRESS", beerPlace.getAddress());
                intent.putExtra("NAME", beerPlace.getName());
                intent.putExtra("LONGITUDE", beerPlace.getLatLng().longitude);
                intent.putExtra("LATITUDE", beerPlace.getLatLng().latitude);
            }
            setResult(2,intent);
            finish();//finishing activity
        });

        Button cancel = findViewById(R.id.cancelButton);
        cancel.setOnClickListener((v) -> {
            setResult(2);
            finish();
        });
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));

        mMap.setOnMapClickListener(this::getLocation);
        mMap.setOnPoiClickListener(this);



        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    }

    @Override
    public void onPoiClick(PointOfInterest poi) {
        FetchPlaceRequest fetchPlaceRequest = FetchPlaceRequest.builder(poi.placeId, placeFields).build();

        pClient.fetchPlace(fetchPlaceRequest).addOnSuccessListener((response) -> {
            beerPlace = response.getPlace();
            setBeerMarker(beerPlace.getLatLng(), beerPlace.getName() + "," + beerPlace.getAddress());
        }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                int statusCode = apiException.getStatusCode();
                // Handle error with given status code.
                Log.e(getString(R.string.app_name), "Place not found: " + exception.getMessage());
            }
        });

    }

    private void getLocation(LatLng place){
        setBeerMarker(place, "FuckOff");
        /*getDeviceLocation();

        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.builder(placeFields).build();
        pClient.findCurrentPlace(request).addOnSuccessListener(((response) -> {
            for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
                Log.i(getString(R.string.app_name), String.format("Place '%s' has likelihood: %f",
                        placeLikelihood.getPlace().getName(),
                        placeLikelihood.getLikelihood()));
            }
        })).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                Log.e(getString(R.string.app_name), "Place not found: " + apiException.getStatusCode());
            }
        });*/
    }


    private void setBeerMarker(LatLng latlng, String title){
        mMap.clear();
        Marker myBeerMarker = mMap.addMarker(new MarkerOptions().position(latlng).title(title));
        myBeerMarker.showInfoWindow();
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
        Log.v("BeerMarker", myBeerMarker.getPosition().toString());
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = (Location) task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        } else {
                            Log.d(getString(R.string.app_name), "Current location is null. Using defaults.");
                            Log.e(getString(R.string.app_name), "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch(SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }
}
