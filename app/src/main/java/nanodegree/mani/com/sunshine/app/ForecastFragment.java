package nanodegree.mani.com.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class ForecastFragment extends Fragment {

    public ArrayAdapter<String> mArrayAdapter;
    public ListView forecastListView;


       public ForecastFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.forecastfragment,menu);

    }


    public  boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_refresh:
                updateWeather();
                return true;
            case R.id.settings:
                Intent intent=new Intent(getActivity(),SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_viewMap:
                viewOnMap();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(getContext());
        String pinCode = preferences.getString(getString(R.string.pref_location_key),getString(R.string.pref_location_default_value));
        new FetchWeatherTask().execute(pinCode);


        //Create adapter
        forecastListView =(ListView)rootView.findViewById(R.id.listView_forecast);
        forecastListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent detailIntent = new Intent(getActivity(),DetailActivity.class);
                detailIntent.putExtra("EXTRA_DETAIL",mArrayAdapter.getItem(position));
                startActivity(detailIntent);
            }
        });
       // List<String> weekForecast = new ArrayList<String>(Arrays.asList(strings));
        mArrayAdapter=new ArrayAdapter<String>(getActivity(),R.layout.list_item_forecast,R.id.list_item_forecast_textView,new ArrayList<String>());
        //Bind adapter to List view

        forecastListView.setAdapter(mArrayAdapter);

        return rootView;

    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();

    }

    public void updateWeather()
    {
        SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(getContext());
        String pinCode = preferences.getString(getString(R.string.pref_location_key),getString(R.string.pref_location_default_value));
        new FetchWeatherTask().execute(pinCode);

    }
    public void viewOnMap()
    {
        SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(getContext());
        String pinCode = preferences.getString(getString(R.string.pref_location_key),getString(R.string.pref_location_default_value));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW);
        Uri mapUri = Uri.parse("geo:0,0?").buildUpon().appendQueryParameter("q",pinCode).build();
        mapIntent.setData(mapUri);
        startActivity(mapIntent);
    }

    public class FetchWeatherTask extends AsyncTask<String,Void,String[]>
        {

            @Override
            protected void onPostExecute(String[] strings) {
                super.onPostExecute(strings);
                mArrayAdapter.clear();
                mArrayAdapter.addAll(strings);


            }

            protected String[] doInBackground(String... pinCodes) {
                //Network Call
                // These two need to be declared outside the try/catch
                // so that they can be closed in the finally block.
                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;

                // Will contain the raw JSON response as a string.
                String forecastJsonStr = null;


                //Build URi
                String baseUri = "http://api.openweathermap.org/data/2.5/forecast/daily?";

                int value_count = 7;
                String value_units = "metric";
                String value_appid = "6cf4c3a2ec7423909f538124b73d30f5";
                String mode = "json";

                String param_query = "q";
                String param_units = "units";
                String param_count = "cnt";
                String param_appid = "appid";
                String param_mode = "mode";

                Uri builtUri = Uri.parse(baseUri).buildUpon()
                        .appendQueryParameter(param_query, pinCodes[0])
                        .appendQueryParameter(param_mode, mode)
                        .appendQueryParameter(param_units, value_units)
                        .appendQueryParameter(param_count, Integer.toString(value_count))
                        .appendQueryParameter(param_appid, value_appid)
                        .build();


                String myUrl = builtUri.toString();
                Log.d("URI", myUrl);

                try {
                    // Construct the URL for the OpenWeatherMap query
                    // Possible parameters are available at OWM's forecast API page, at
                    // http://openweathermap.org/API#forecast
                    URL url = new URL(myUrl);

                    // Create the request to OpenWeatherMap, and open the connection
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // Read the input stream into a String
                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        // Nothing to do.
                        forecastJsonStr = null;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                        // But it does make debugging a *lot* easier if you print out the completed
                        // buffer for debugging.
                        buffer.append(line + "\n");
                    }

                    if (buffer.length() == 0) {
                        // Stream was empty.  No point in parsing.
                        forecastJsonStr = null;
                    }
                    forecastJsonStr = buffer.toString();
                } catch (IOException e) {
                    Log.e("PlaceholderFragment", "Error ", e);
                    // If the code didn't successfully get the weather data, there's no point in attempting
                    // to parse it.
                    forecastJsonStr = null;
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (final IOException e) {
                            Log.e("PlaceholderFragment", "Error closing stream", e);
                        }
                    }
                }
                Log.d("Response", forecastJsonStr);
                String[] weatherData = new String[0];
                try {
                    weatherData = getWeatherDataFromJson(forecastJsonStr, 7);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return weatherData;
            }

            /* The date/time conversion code is going to be moved outside the asynctask later,
        * so for convenience we're breaking it out into its own method now.
        */
            private String getReadableDateString(long time){
                // Because the API returns a unix timestamp (measured in seconds),
                // it must be converted to milliseconds in order to be converted to valid date.
                SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
                return shortenedDateFormat.format(time);
            }

            /**
             * Prepare the weather high/lows for presentation.
             */
            private String formatHighLows(double high, double low) {

                SharedPreferences preferences=PreferenceManager.getDefaultSharedPreferences(getContext());
                String unitType= preferences.getString(getString(R.string.pref_units_key),getString(R.string.pref_units_default_value));
                if(!unitType.equals(getString(R.string.pref_units_default_value)))
                {
                    high=high*1.8+32;
                    low=low*1.8+32;
                }
                // For presentation, assume the user doesn't care about tenths of a degree.
                long roundedHigh = Math.round(high);
                long roundedLow = Math.round(low);

                String highLowStr = roundedHigh + "/" + roundedLow;
                return highLowStr;
            }

            /**
             * Take the String representing the complete forecast in JSON Format and
             * pull out the data we need to construct the Strings needed for the wireframes.
             *
             * Fortunately parsing is easy:  constructor takes the JSON string and converts it
             * into an Object hierarchy for us.
             */
            private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                    throws JSONException {

                // These are the names of the JSON objects that need to be extracted.
                final String OWM_LIST = "list";
                final String OWM_WEATHER = "weather";
                final String OWM_TEMPERATURE = "temp";
                final String OWM_MAX = "max";
                final String OWM_MIN = "min";
                final String OWM_DESCRIPTION = "main";

                JSONObject forecastJson = new JSONObject(forecastJsonStr);
                JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

                // OWM returns daily forecasts based upon the local time of the city that is being
                // asked for, which means that we need to know the GMT offset to translate this data
                // properly.

                // Since this data is also sent in-order and the first day is always the
                // current day, we're going to take advantage of that to get a nice
                // normalized UTC date for all of our weather.

                Time dayTime = new Time();
                dayTime.setToNow();

                // we start at the day returned by local time. Otherwise this is a mess.
                int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

                // now we work exclusively in UTC
                dayTime = new Time();

                String[] resultStrs = new String[numDays];
                for(int i = 0; i < weatherArray.length(); i++) {
                    // For now, using the format "Day, description, hi/low"
                    String day;
                    String description;
                    String highAndLow;

                    // Get the JSON object representing the day
                    JSONObject dayForecast = weatherArray.getJSONObject(i);

                    // The date/time is returned as a long.  We need to convert that
                    // into something human-readable, since most people won't read "1400356800" as
                    // "this saturday".
                    long dateTime;
                    // Cheating to convert this to UTC time, which is what we want anyhow
                    dateTime = dayTime.setJulianDay(julianStartDay+i);
                    day = getReadableDateString(dateTime);

                    // description is in a child array called "weather", which is 1 element long.
                    JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                    description = weatherObject.getString(OWM_DESCRIPTION);

                    // Temperatures are in a child object called "temp".  Try not to name variables
                    // "temp" when working with temperature.  It confuses everybody.
                    JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                    double high = temperatureObject.getDouble(OWM_MAX);
                    double low = temperatureObject.getDouble(OWM_MIN);

                    highAndLow = formatHighLows(high, low);
                    resultStrs[i] = day + " - " + description + " - " + highAndLow;
                }

                for (String s : resultStrs) {
                    Log.v("result strings", "Forecast entry: " + s);
                }
                return resultStrs;

            }





        }

}














































