package com.cumn.ark.locationService;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cumn.ark.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivityMaps extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap map;
    private String start = "";
    private String end = "";
    private Polyline poly;

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setMinZoomPreference(5f);
        createMarker();
        createMarkerFromJson();
    }

    private void createMarkerFromJson() {
        try {
            // Leer el archivo JSON
            InputStream inputStream = getAssets().open("serviciosYlugares.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            String json = new String(buffer, "UTF-8");

            // Parsear el JSON y agregar marcadores al mapa
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                double latitude = jsonObject.getDouble("latitude");
                double longitude = jsonObject.getDouble("longitude");
                LatLng latLng = new LatLng(latitude, longitude);
                map.addMarker(new MarkerOptions().position(latLng));
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void createMarker() {
        LatLng favoritePlace = new LatLng(40.417031, -3.703401);
        map.addMarker(new MarkerOptions().position(favoritePlace).title("Mi playa favorita!"));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(favoritePlace, 18f), 4000, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_maps);

        Button btnCalculate = findViewById(R.id.btnCalculateRoute);
        btnCalculate.setOnClickListener(v -> {
            start = "";
            end = "";
            if (poly != null) {
                poly.remove();
                poly = null;
            }
            Toast.makeText(this, "Selecciona punto de origen y final", Toast.LENGTH_SHORT).show();
            if (map != null) {
                map.setOnMapClickListener(latLng -> {
                    if (start.isEmpty()) {
                        start = latLng.longitude + "," + latLng.latitude;
                    } else if (end.isEmpty()) {
                        end = latLng.longitude + "," + latLng.latitude;
                        createRoute();
                    }
                });
            }
        });

        createMapFragment();
    }

    private void createMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void createRoute() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openrouteservice.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        Call<RouteResponse> call = apiService.getRoute("5b3ce3597851110001cf6248783c26e6800a4ee48be8b85d13af6824", start, end);
        call.enqueue(new Callback<RouteResponse>() {
            @Override
            public void onResponse(@NonNull Call<RouteResponse> call, @NonNull Response<RouteResponse> response) {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    drawRoute(response.body());
                } else {
                    Log.i("aris", "KO");
                }
            }

            @Override
            public void onFailure(@NonNull Call<RouteResponse> call, @NonNull Throwable t) {
                Log.e("aris", "Error", t);
            }
        });
    }


    private void drawRoute(RouteResponse routeResponse) {
        PolylineOptions polyLineOptions = new PolylineOptions();
        List<List<Double>> coordinates = routeResponse.getFeatures().get(0).getGeometry().getCoordinates();
        for (List<Double> coordinate : coordinates) {
            polyLineOptions.add(new LatLng(coordinate.get(1), coordinate.get(0)));
        }
        runOnUiThread(() -> poly = map.addPolyline(polyLineOptions));
    }

}