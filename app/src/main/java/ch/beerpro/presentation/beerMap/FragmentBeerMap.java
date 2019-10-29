package ch.beerpro.presentation.beerMap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import butterknife.ButterKnife;
import ch.beerpro.R;
import ch.beerpro.data.repositories.RatingsRepository;
import ch.beerpro.domain.models.BeerPlace;
import ch.beerpro.domain.models.Rating;

public class FragmentBeerMap extends Fragment implements
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener {
    private final LatLng mDefaultLocation = new LatLng(47.3774337,8.4666756);
    private static final int DEFAULT_ZOOM = 8;

    private GoogleMap mMap;

    private LiveData<List<Rating>> ratings;
    private HashMap<String, MarkerOptions> markers = new HashMap<>();

    public FragmentBeerMap() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View map = inflater.inflate(R.layout.fragment_beer_map, container, false);
        ButterKnife.bind(this, map);

        RatingsRepository ratingsRepository = new RatingsRepository();
        this.ratings = ratingsRepository.getAllRatings();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        ratings.observe(this, this::updateRatings);
        return map;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.setInfoWindowAdapter(new BeerMapInfo());
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final long duration = 1500;

        final Interpolator interpolator = new BounceInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = Math.max(
                        1 - interpolator.getInterpolation((float) elapsed / duration), 0);
                marker.setAnchor(0.5f, 1.0f + 2 * t);

                if (t > 0.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                }
            }
        });
        return false;
    }

    private void updateRatings(List<Rating> ratings) {
        if(mMap != null && ratings != null) {
            for (int i = 0; i < ratings.size(); i++) {
                Rating rating = ratings.get(i);
                if(rating.getBeerPlace() != null) {
                    if (markers.containsKey(rating.getId())) {
                        markers.get(rating.getId()).position(new LatLng(rating.getBeerPlace().getLatitude(), rating.getBeerPlace().getLongitude()))
                                .title(rating.getBeerPlace().getName())
                        .snippet(rating.getBeerId());
                    } else {
                        MarkerOptions marker = new MarkerOptions()
                                .position(new LatLng(rating.getBeerPlace().getLatitude(), rating.getBeerPlace().getLongitude()))
                                .title(rating.getBeerPlace().getName())
                                .snippet(rating.getId());
                        this.mMap.addMarker(marker);
                        this.markers.put(rating.getId(), marker);
                    }
                }
            }
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Rating rating : ratings) {
                BeerPlace beerPlace = rating.getBeerPlace();
                if(beerPlace != null) {
                    builder.include(new LatLng(beerPlace.getLatitude(), beerPlace.getLongitude()));
                }
            }
            LatLngBounds bounds = builder.build();
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 80);
            mMap.animateCamera(cu);
            mMap.setLatLngBoundsForCameraTarget(bounds);
        } else if(ratings == null && mMap != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(mDefaultLocation));
            mMap.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));
        }
    }

    public class BeerMapInfo implements GoogleMap.InfoWindowAdapter {
        private final View markerItemView;

        public BeerMapInfo() {
            markerItemView = getLayoutInflater().inflate(R.layout.fragment_beer_map_info, null);
        }

        @Override
        public View getInfoContents(Marker marker) {
            Rating context = null;
            for(Rating rating : ratings.getValue()) {
                if(rating.getId().equals(marker.getSnippet())) {
                    context = rating;
                }
            }
            if (context == null) return null;
            TextView title = markerItemView.findViewById(R.id.title);
            TextView description = markerItemView.findViewById(R.id.rating_description);
            TextView poster = markerItemView.findViewById(R.id.poster);
            TextView place = markerItemView.findViewById(R.id.place);
            ImageView image = markerItemView.findViewById(R.id.image);
            RatingBar rating = markerItemView.findViewById(R.id.rating);

            title.setText(context.getBeerName());
            if(context.getComment().isEmpty()){
                description.setText(R.string.no_comment);
                description.setTextColor(ContextCompat.getColor(getContext(), R.color.quantum_grey200));
            } else {
                description.setText(context.getComment());
                description.setTextColor(ContextCompat.getColor(getContext(), R.color.quantum_grey));
            }
            poster.setText(context.getUserName());
            place.setText(context.getBeerPlace().getAddress());

            rating.setRating((int)context.getRating());

            String photo = context.getPhoto();
            if (context.getPhoto() != null) {
                final HandlerThread handlerThread = new HandlerThread("GetPhoto");
                handlerThread.start();
                final Handler handler = new Handler(handlerThread.getLooper());

                class LoadImage implements java.lang.Runnable {
                    public boolean notFinished = true;

                    public void run() {
                        try {
                            Bitmap bmp = BitmapFactory.decodeStream((new URL(photo).openConnection().getInputStream()));
                            image.setImageBitmap(bmp);
                            image.setVisibility(View.VISIBLE);
                        } catch (MalformedURLException e) {
                            image.setVisibility(View.GONE);
                        } catch (IOException e) {
                            image.setVisibility(View.GONE);
                        }
                        synchronized (this) {
                            this.notFinished = false;
                            this.notify();
                        }
                    }
                }

                LoadImage runnable = new LoadImage();
                handler.post(runnable);
                synchronized (runnable) {
                    try {
                        while (runnable.notFinished) {
                            runnable.wait();
                            runnable.notify();
                        }
                    } catch (InterruptedException e) {
                        image.setVisibility(View.GONE);
                    }
                }
            } else {
                image.setVisibility(View.GONE);
            }
            return markerItemView;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }
    }
}


