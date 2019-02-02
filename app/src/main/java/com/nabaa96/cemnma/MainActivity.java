package com.nabaa96.cemnma;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.nabaa96.cemnma.common.Common;
import com.nabaa96.cemnma.Model.UserWeather;
import com.nabaa96.cemnma.Model.WeatherResponse;
import com.nabaa96.cemnma.api.Clint;
import com.nabaa96.cemnma.api.OpenWeatherMap;
import com.nabaa96.cemnma.db.DbHelper;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity implements LocationListener {
    private TextView  txtlastupdate, txtdescription, txthumminity, txttime, txtcalc, txtcountry, txttime2;
    LinearLayout linear;
    private View view;
    private int apiTimes;
    UserWeather info;
    private static final String TAG = "MainActivity";
    LocationManager locationManager;
    UserWeather userWeather;
    String provider;
    private DbHelper dbHelper;
    int MY_PERMTION = 0;
    static double lat, lng;
    private SharedPreferences preferences;
    public static final String PREF_NAME = "app_pref";
    Handler handler = new Handler();


   Runnable runnable = new Runnable() {
       @Override
        public void run() {
           //for view all items that head in first time "anmation"
            txtcalc.setVisibility(View.VISIBLE);
            txtcountry.setVisibility(View.VISIBLE);
            txtdescription.setVisibility(View.VISIBLE);
            txtlastupdate.setVisibility(View.VISIBLE);
            txttime2.setVisibility(View.VISIBLE);
            txttime.setVisibility(View.VISIBLE);
            txthumminity.setVisibility(View.VISIBLE);
            linear.setVisibility(View.VISIBLE);
             view.setVisibility(View.VISIBLE);

       }
   };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.night);
        view=findViewById(R.id.view);
        txtlastupdate = findViewById(R.id.txtlastupdate);
        txtdescription = findViewById(R.id.txtdescription);
        txthumminity = findViewById(R.id.txthummunity);
        txttime = findViewById(R.id.txttime);
        txtcountry = findViewById(R.id.txtcountry);
        txtcalc = findViewById(R.id.txtcalculs);
        txttime2 = findViewById(R.id.txttime2);
        linear = findViewById(R.id.linear);
        info=new UserWeather();
        userWeather=new UserWeather();
        dbHelper = new DbHelper(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), false);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(MainActivity.this, new String[]{

                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.SYSTEM_ALERT_WINDOW,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, MY_PERMTION);
        }

        handler.postDelayed(runnable, 3000);

        // first we request location and may  onLocationChanged invoket or not this depand on if
        // location of user has change
        locationManager.requestLocationUpdates(provider,
                400, 1,
                this);


        // here we get last location my wil be iraq or any where .. last location
        Location location = locationManager.getLastKnownLocation(provider);

        // here if the last location unknowing ,we request again if not
        if (location != null) {
            onLocationChanged(location);

        } else {
            // requst location again
            locationManager.requestLocationUpdates(provider,
                    200, 100, this, getMainLooper());
        }


     // TODO: 02/02/19  add button to let user change location manully
    }

       @Override
       protected void onPause() {

           super.onPause();
           Toast.makeText(this, "onPause", Toast.LENGTH_SHORT).show();
           if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

               ActivityCompat.requestPermissions(MainActivity.this, new String[]{

                       Manifest.permission.INTERNET,
                       Manifest.permission.ACCESS_COARSE_LOCATION,
                       Manifest.permission.ACCESS_FINE_LOCATION,
                       Manifest.permission.ACCESS_NETWORK_STATE,
                       Manifest.permission.SYSTEM_ALERT_WINDOW,
                       Manifest.permission.WRITE_EXTERNAL_STORAGE

               }, MY_PERMTION);
               locationManager.removeUpdates(this);

           }

       }


       @Override
       protected void onResume() {
           super.onResume();

           // TODO: 31/01/19 api calls
           if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
               ActivityCompat.requestPermissions(MainActivity.this, new String[]{

                       Manifest.permission.INTERNET,
                       Manifest.permission.ACCESS_COARSE_LOCATION,
                       Manifest.permission.ACCESS_FINE_LOCATION,
                       Manifest.permission.ACCESS_NETWORK_STATE,
                       Manifest.permission.SYSTEM_ALERT_WINDOW,
                       Manifest.permission.WRITE_EXTERNAL_STORAGE

               }, MY_PERMTION);
           }
           locationManager.requestLocationUpdates(provider,
                   400, 1,
                   this);
           // listener for location change and MainActivity implements this listener

       }

    @Override
    public void onLocationChanged(Location location) {
        lat = location.getLatitude();
        lng = location.getLongitude();
        Log.d(TAG, "onLocationChanged: lat = " + lat);
        Log.d(TAG, "onLocationChanged: lng = " + lng);
        if (hasInternet()) {
            showweather(lat, lng);
        } else {
            getOfflineData();
        }
    }

    private void showweather(double lat, double lng) {

        Log.d(TAG, "showweather: lat:" + lat + "lng :" + lng);
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        final OpenWeatherMap apiservices = Clint.getRetrofit().create(OpenWeatherMap.class);
        Call<WeatherResponse> call = apiservices.getWeatherbylatlang(String.valueOf(lat), String.valueOf(lng), Common.API_KEY, "metric");
        call.enqueue(new Callback<WeatherResponse>() {

            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
 apiTimes=info.getApiscall();
                apiTimes++;
info.setApiscall(apiTimes);

// check if response is succes

                if (!response.isSuccessful() || response.body() == null)
                    return;

//initization of fundumantal weather varables
                WeatherResponse weatherResponse = response.body();
                String city = weatherResponse.getName();
                String country = weatherResponse.getSys().getCountry();
                String dateNow = Common.getdatenow();
                String description = weatherResponse.getWeathers().get(0).getDescription();
                String humminty = weatherResponse.getMain().getHumidity();
                String sunset = Common.unixTimeTDateTime(Double.valueOf(weatherResponse.getSys().getSunset()));
                String sunrise = Common.unixTimeTDateTime(Double.valueOf(weatherResponse.getSys().getSunrise()));
                String temp = weatherResponse.getMain().getTemp();

//set text view
                txtcountry.setText(String.format("%s/%s", country,city));
                txtlastupdate.setText(dateNow);
                txtdescription.setText(description);
                txthumminity.setText(humminty);
                txttime.setText(sunset);
                txttime2.setText(sunrise);
                txtcalc.setText(String.format("%s C°", temp));

//save in sqlite DB, by opject call "info"

                info.setTemp(temp);
                info.setCity(city);
                info.setCountry(country);
                info.setLastupdate(dateNow);
                info.setDescription(description);
                info.setHumidity(humminty);
                info.setTime(sunset);
                info.setTime2(sunrise);
                dbHelper.saveUserWeatherInfo(info);

//now save data using prefrences :)
                SharedPreferences.Editor prefsEditor = preferences.edit();
                Gson gson = new Gson();
                String json = gson.toJson(info);
                prefsEditor.putString(PREF_NAME, json);
                prefsEditor.apply();
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "no fetch", Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {


    }

    @Override
    public void onProviderDisabled(String provider) {


    }

    //for check internet connection
    private boolean hasInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(CONNECTIVITY_SERVICE);

        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info == null)
            return false;

        return info.isConnected();


    }

    //if user dont have connection will data retrive from database accourding to priviouse state of weather
    private void getOfflineData() {
        userWeather = dbHelper.getLastUserWeatherInfo();

        Log.d(TAG, "getOfflineData: " + userWeather);

        txtcountry.setText(userWeather.getCountry());
        txtlastupdate.setText((Common.getdatenow()));
        txtdescription.setText(userWeather.getDescription());
        txthumminity.setText(userWeather.getHumidity());
        txttime.setText(userWeather.getTime2());
        txttime2.setText(userWeather.getTime());
        String temp = userWeather.getTemp();
        txtcalc.setText(String.format(Locale.US, "%s C°", temp));
    }
}

