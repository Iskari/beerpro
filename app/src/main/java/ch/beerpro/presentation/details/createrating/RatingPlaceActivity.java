package ch.beerpro.presentation.details.createrating;

import android.app.AlertDialog;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.tasks.Task;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import ch.beerpro.R;
import ch.beerpro.domain.models.BeerPlace;
import ch.beerpro.presentation.details.createrating.dialogs.NewPlaceDialog;

public class RatingPlaceActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnPoiClickListener, NewPlaceDialog.PlaceDialogListener{

    private GoogleMap mMap;
    private BeerPlace beerPlace;
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // Keys for storing activity state.
    private static final String KEY_LOCATION = "location";

    private Location mLastKnownLocation;
    private final LatLng mDefaultLocation = new LatLng(47.3774337,8.4666756);

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private PlacesClient pClient;
    List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS, Place.Field.ID);

    private LatLng ownPlace;



    @Override
    protected void onCreate(Bundle savedInstanceState) {



        super.onCreate(savedInstanceState);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
        }

        setContentView(R.layout.activity_rating_place);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if(mapFragment != null) {
            mapFragment.getMapAsync(this);
        }else{
            makeToast("Map could not be loaded.");
        }



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
            public void onPlaceSelected(@NonNull Place place) {
                if(place.getLatLng() != null) {
                    beerPlace = new BeerPlace(place.getId(), place.getName(), place.getAddress(), place.getLatLng().latitude, place.getLatLng().longitude);
                    setBeerMarker(place.getLatLng(), beerPlace.getName() + ", " + beerPlace.getAddress());
                }else{
                    Log.e(getString(R.string.app_name), "No coordinates in place");
                }
            }

            @Override
            public void onError(@NonNull Status status) {
                makeToast("An error occurred: " + status);
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
                intent.putExtra("LONGITUDE", beerPlace.getLongitude());
                intent.putExtra("LATITUDE", beerPlace.getLatitude());
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
            Place place = response.getPlace();
            if(place.getLatLng() != null) {
                beerPlace = new BeerPlace(place.getId(), place.getName(), place.getAddress(), place.getLatLng().latitude, place.getLatLng().longitude);
            }else{
                Log.e(getString(R.string.app_name), "No coordinates in place");
            }

            setBeerMarker(place.getLatLng(), place.getName() + "," + place.getAddress());
        }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                int statusCode = apiException.getStatusCode();
                // Handle error with given status code.
                Log.e(getString(R.string.app_name), "Place not found: " + exception.getMessage() + statusCode);
                makeToast("Place not found: " + exception.getMessage() + statusCode);
            }
        });

    }

    private void getLocation(LatLng place){
        LinkedList<BeerPlace> beerPlaces = new LinkedList<>();

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + place.latitude + "," + place.longitude + "&radius=50&key=" + getString(R.string.google_api_key);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, response -> {
                    try {
                        JSONArray array = response.getJSONArray("results");
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject c = array.getJSONObject(i);
                            String name = c.getString("name");
                            String id = c.getString("place_id");
                            String address = c.getString("vicinity");

                            JSONObject geometry = c.getJSONObject("geometry");
                            JSONObject location = geometry.getJSONObject("location");
                            double latitude = location.getDouble("lat");
                            double longitude = location.getDouble("lng");

                            BeerPlace temp = new BeerPlace(id, name, address, latitude, longitude);
                            beerPlaces.add(temp);
                        }
                        showPlacesDialog(beerPlaces, place);
                    } catch (JSONException e) {
                            Log.e(getString(R.string.app_name), e.getMessage());
                    }
                }, error -> {
                    Log.e(getString(R.string.app_name), "Error occured on nearby request: " + error.getMessage());
                    makeToast("Error occured on nearby request:" + error.getMessage());
                });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsonObjectRequest);
    }

    private void showPlacesDialog(LinkedList<BeerPlace> beerPlaces, LatLng place){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        CharSequence[] names = new CharSequence[beerPlaces.size()+1];
        names[0] = getString(R.string.setOwnPlace);
        for(int i = 0; i < beerPlaces.size(); i++){
            names[i+1] = beerPlaces.get(i).getName();
        }
        builder.setTitle(getString(R.string.choosePlace));
        builder.setItems(names, (dialog, which) -> {
            if(which == 0){
                showNewPlaceDialog(place);
            }else {
                beerPlace = beerPlaces.get(which-1);
                setBeerMarker(new LatLng(beerPlace.getLatitude(), beerPlace.getLongitude()), beerPlace.getName() + "," + beerPlace.getAddress());
            }
        });
        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showNewPlaceDialog(LatLng place){
        ownPlace = place;
        NewPlaceDialog placeDialog = new NewPlaceDialog();
        placeDialog.show(getSupportFragmentManager(), "PlaceDialogFragment");
    }

    @Override
    public void onDialogPositiveClick(String name, String address) {
        beerPlace = new BeerPlace(null, name, address, ownPlace.latitude, ownPlace.longitude);
        setBeerMarker(ownPlace, name + ", " + address);
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
                locationResult.addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Set the map's camera position to the current location of the device.
                        mLastKnownLocation = (Location) task.getResult();
                        assert mLastKnownLocation != null;
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                    } else {
                        Log.d(getString(R.string.app_name), "Current location is null. Using defaults.");
                        Log.e(getString(R.string.app_name), "Exception: %s", task.getException());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                        mMap.getUiSettings().setMyLocationButtonEnabled(false);
                    }
                });
            }
        } catch(SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    public void makeToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
