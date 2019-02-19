package com.nabaa96.cemnma;

import android.Manifest;
import android.content.Context;
import static com.nabaa96.cemnma.SavingWeatherInfo.SavingWeatherDetails.USER_WEATHER_PREF_KEY;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.nabaa96.cemnma.Model.UserWeather;
import com.nabaa96.cemnma.Model.WeatherResponse;
import com.nabaa96.cemnma.SavingWeatherInfo.SavingWeatherDetails;
import com.nabaa96.cemnma.api.Clint;
import com.nabaa96.cemnma.api.OpenWeatherMap;
import com.nabaa96.cemnma.common.Common;
import com.nabaa96.cemnma.db.DbHelper;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.nabaa96.cemnma.SavingWeatherInfo.SavingWeatherDetails.ShowWeatherDetails;
import static com.nabaa96.cemnma.SavingWeatherInfo.SavingWeatherDetails.preferences;
public class MainActivity extends AppCompatActivity implements LocationListener {
    public TextView currentDate, description, humidity, sunRise, temperature, country, sunSet;
    LinearLayout linear;
    private View view;
    private int apiTimes;
    UserWeather info;
    LocationManager locationManager;
    UserWeather userWeather;
    private DbHelper dbHelper;
    int REQUEST_CODE = 0;
    static double lat, lng;
    public static final String PREF_NAME = "app_pref";
    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            //for view all items that head in first time "animation"
            temperature.setVisibility(View.VISIBLE);
            country.setVisibility(View.VISIBLE);
            description.setVisibility(View.VISIBLE);
            currentDate.setVisibility(View.VISIBLE);
            sunSet.setVisibility(View.VISIBLE);
            sunRise.setVisibility(View.VISIBLE);
            humidity.setVisibility(View.VISIBLE);
            linear.setVisibility(View.VISIBLE);
            view.setVisibility(View.VISIBLE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int currentTime = Integer.parseInt(Common.getdatenow2());
        //for inteface
        if (currentTime >= 18 || currentTime <= 4) {
            setContentView(R.layout.night);
        } else {
            setContentView(R.layout.activity_main);
        }
        InitializeWidgets();

        info = new UserWeather();
        userWeather = new UserWeather();
        dbHelper = new DbHelper(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        handler.postDelayed(runnable, 3000);
        //getcurrent location
        getLocationManager();
    }

    private void getLocationManager() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.SYSTEM_ALERT_WINDOW,
            }, REQUEST_CODE);
        }

        // first we request location and may  onLocationChanged invoket or not this depand on if
        // location of user has change
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER
                , 0, 0, this);

        // here we get last location my wil be iraq or any where .. last location
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        // here if the last location unknowing ,we request again if not
        if (location != null) {
            onLocationChanged(location);

        } else {
            // request location again
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    0, 0, this, getMainLooper());
        }
    }

    private void InitializeWidgets() {
        view = findViewById(R.id.view);
        currentDate = findViewById(R.id.txtlastupdate);
        description = findViewById(R.id.txtdescription);
        humidity = findViewById(R.id.txthummunity);
        sunRise = findViewById(R.id.txttime);
        country = findViewById(R.id.txtcountry);
        temperature = findViewById(R.id.txtcalculs);
        sunSet = findViewById(R.id.txttime2);
        linear = findViewById(R.id.linear);
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        lat = location.getLatitude();
        lng = location.getLongitude();

        //checking if internet connection is valid
        if (hasInternet()) {
            GetWeatherInformation(lat, lng);
        } else {
            //if not,get last state
            getOfflineData();
        }
    }

    private void GetWeatherInformation(double lat, double lng) {

        final OpenWeatherMap apiServices = Clint.getRetrofit().create(OpenWeatherMap.class);
        Call<WeatherResponse> call = apiServices.getWeatherbylatlang(String.valueOf(lat), String.valueOf(lng), Common.API_KEY, "metric");
        call.enqueue(new Callback<WeatherResponse>() {

            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                apiTimes = info.getApiscall();
                apiTimes++;
                info.setApiscall(apiTimes);
                // check if response is success
                if (!response.isSuccessful() || response.body() == null)
                    return;

                //Initialization of fundamental weather variables
                WeatherResponse weatherResponse = response.body();
                String city = weatherResponse.getName();
                String country = weatherResponse.getSys().getCountry();
                String dateNow = Common.getdatenow();
                String description = weatherResponse.getWeathers().get(0).getDescription();
                String humidity = weatherResponse.getMain().getHumidity();
                String sunset = Common.unixTimeTDateTime(Double.valueOf(weatherResponse.getSys().getSunset()));
                String sunrise = Common.unixTimeTDateTime(Double.valueOf(weatherResponse.getSys().getSunrise()));
                String temp = weatherResponse.getMain().getTemp();

                //save in Sqlite DB, by object call "info"
                SavingWeatherDetails.SaveWeatherDetailsInDB(dbHelper, info, city, country, dateNow, description, humidity, sunset, sunrise, temp);
                //now save data using preferences :)
                SavingWeatherDetails.SaveWeatherDetailsInPref(getApplicationContext(), info);
                //set text view

                ShowWeatherDetails(MainActivity.this, city, country, dateNow, description, humidity, sunset, sunrise, temp);

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
        if (connectivityManager != null) {
            NetworkInfo networkInfo =  connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null) {
                if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                    return true;
                }
                Toast.makeText(getApplicationContext(), "check your internet please !", Toast.LENGTH_SHORT).show();
                return false;
            }
            Toast.makeText(getApplicationContext(), "check your internet please!", Toast.LENGTH_SHORT).show();
            return false;
        }
        Toast.makeText(getApplicationContext(), "check your internet please !", Toast.LENGTH_SHORT).show();
return false;


    }

    //if user dont have connection will data retrieve from database according to previous state of weather
    private void getOfflineData() {
    userWeather = dbHelper.getLastUserWeatherInfo();
        country.setText(userWeather.getCountry());
        currentDate.setText((userWeather.getLastupdate()));
        description.setText(userWeather.getDescription());
        humidity.setText(userWeather.getHumidity());
        sunRise.setText(userWeather.getTime2());
        sunSet.setText(userWeather.getTime());
        String temp = userWeather.getTemp();
        temperature.setText(String.format(Locale.US, "%s C°", temp));

   /*

   //to retrve from prefrences
   Gson gson = new Gson();
        SharedPreferences preferences=getSharedPreferences(PREF_NAME,MODE_PRIVATE);

        String json = preferences.getString(USER_WEATHER_PREF_KEY, "");
        UserWeather obj = gson.fromJson(json, UserWeather.class);

        country.setText(obj.getCountry());
        currentDate.setText((obj.getLastupdate()));
        description.setText(obj.getDescription());
        humidity.setText(obj.getHumidity());
        sunRise.setText(obj.getTime2());
        sunSet.setText(obj.getTime());
        String temp = obj.getTemp();
        temperature.setText(String.format(Locale.US, "%s C°", temp));
*/
    }
}

