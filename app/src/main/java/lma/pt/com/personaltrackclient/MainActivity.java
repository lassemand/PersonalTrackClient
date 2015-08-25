package lma.pt.com.personaltrackclient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;


public class MainActivity extends Activity implements LocationListener {

    private EditText etUsername, etPassword, etIp;
    private Location location;
    private String provider;
    private LocationManager locationManager;
    private String inputLine = null;
    private Spinner spinner;

    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        etUsername = (EditText) findViewById(R.id.username);
        etPassword = (EditText) findViewById(R.id.password);
        etIp = (EditText) findViewById(R.id.ip);

        etUsername.setText(preferences.getString("username", ""));
        etPassword.setText(preferences.getString("password", ""));
        etIp.setText(preferences.getString("ip", ""));
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean enabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!enabled) {
            Toast.makeText(this, "Not enabled", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            System.out.println("Provider " + provider + " has been selected.");
            onLocationChanged(location);
        } else {
            Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        spinner = (Spinner) findViewById(R.id.spinner);
    }

    /* Request updates at startup */
    @Override
    protected void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }

    private void request(View v){
        new AsyncTask<String, Void, Boolean>(){

            @Override
            protected Boolean doInBackground(String... strings) {
                try {
                    URL connection = new URL("http://" + etIp.getText().toString() + ":8080/PersonalTrack/rest/token/retrieve");
                    HttpURLConnection con = (HttpURLConnection) connection.openConnection();
                    con.setRequestProperty("Content-type", "text/plain");
                    con.setRequestProperty("'Accept'", "application/json");
                    con.setRequestMethod("POST");
                    con.setDoOutput(true);
                    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                    JSONObject object = new JSONObject();
                    object.put("username", etUsername.getText().toString());
                    object.put("password", etPassword.getText().toString());
                    wr.writeBytes(object.toString());
                    wr.close();

                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            con.getInputStream()));
                    boolean valid = (inputLine = in.readLine()) != null;

                    in.close();
                    return valid;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean valid) {
                super.onPostExecute(valid);
                if(valid) {
                    Toast.makeText(MainActivity.this, "Valid token retrieved", Toast.LENGTH_SHORT).show();

                }
                else{
                    Toast.makeText(MainActivity.this, "Request token again", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private void initiateSpinnerData(){
        Log.i("inputLine", inputLine);
    }

    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    public void save(View v){
        preferences.edit().putString("username", etUsername.getText().toString()).apply();
        preferences.edit().putString("password", etPassword.getText().toString()).apply();
        preferences.edit().putString("ip", etIp.getText().toString()).apply();
    }
    public void update(View v){
        new AsyncTask<String, Void, Integer>(){

            @Override
            protected Integer doInBackground(String... strings) {
                try {
                            if (location != null) {
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();
                                JSONObject object = new JSONObject(inputLine);
                                String token = object.getString("token");
                                String username = object.getString("username");
                                URL connection = new URL("http://" + etIp.getText().toString() + ":8080/PersonalTrack/rest/devicelocation/updateDeviceLocation?eventdevice=zacreoz0s6vhzofids14");
                                HttpURLConnection con = (HttpURLConnection) connection.openConnection();
                                con.setRequestProperty("Content-type", "text/plain");
                                con.setRequestProperty("Accept", "application/json");
                                con.setRequestProperty("Token", token + ":" + username);
                                con.setRequestMethod("POST");
                                con.setDoOutput(true);
                                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                                object = new JSONObject();
                                object.put("latitude", latitude);
                                object.put("longitude", longitude);
                                object.put("modified", new Date().getTime());
                                wr.writeBytes(object.toString());
                                wr.close();
                                int response = con.getResponseCode();
                                return response;
                            }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return -1;
            }

            @Override
            protected void onPostExecute(Integer integer) {
                super.onPostExecute(integer);
                if(integer.intValue() == 200) Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_LONG).show();
                else Toast.makeText(MainActivity.this, "Fail: " + integer, Toast.LENGTH_LONG).show();
            }
        }.execute();
    }

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(this, "onLocationChanged", Toast.LENGTH_SHORT).show();
        this.location = location;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
