package com.tsapra.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize


import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.motion.widget.Debug.getLocation
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.squareup.picasso.Picasso
import com.tsapra.weatherapp.databinding.ActivityMainBinding
import com.tsapra.weatherapp.ui.theme.WeatherAppTheme
import org.json.JSONException
import java.io.IOException
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var binding:ActivityMainBinding
    private lateinit var weatherModel:ArrayList<WeatherModel>
    private lateinit var weatherAdapter:WeatherAdapter
    //private lateinit var locationManager:LocationManager
    private val permissionId=2
    private lateinit var cityName:String
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        weatherModel= arrayListOf()
        weatherAdapter=WeatherAdapter(this,weatherModel)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.weatherRV.adapter=weatherAdapter
        getLocation()



        binding.searchBtn.setOnClickListener{
            val city= binding.cityInputEdit.text
            if(city!!.isEmpty()){
                Toast.makeText(this,"Please enter city name",Toast.LENGTH_LONG).show()
            }else{
                binding.cityName.text= city.toString().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                getWeatherInfo(city.toString())
            }
        }

    }

    private fun isLocationEnabled():Boolean{
        val locationManager:LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun checkPermission() : Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ){
            return true
        }
        return false
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            permissionId
        )
    }
    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode== permissionId){
            if (grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                getLocation()
            }
        }
    }
    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun getLocation() {
        if(checkPermission()){
            if(isLocationEnabled()){
                mFusedLocationClient.lastLocation.addOnCompleteListener(this){ task->
                    val location: Location? =task.result
                    if(location!=null){
                        val geocoder= Geocoder(this, Locale.getDefault())
                        val list:List<Address>? =
                            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        cityName= "${list!![0].locality}"
                        binding.cityName.text = cityName.capitalize()
                        getWeatherInfo(cityName)

                    }
                }
            }else{
                Toast.makeText(this, "Please turn on location", Toast.LENGTH_LONG).show()
                val intent= Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
                getLocation()
            }
        }else{
            requestPermissions()
        }
    }

    private fun getWeatherInfo(cityName:String){
        val applicationInfo=applicationContext.packageManager.getApplicationInfo(
            applicationContext.packageName,
            PackageManager.GET_META_DATA
        )
        val apiKey="ead821a2bc8f46e2822130209230607"
        val url= "http://api.weatherapi.com/v1/forecast.json?key=$apiKey&q=$cityName&days=1&aqi=yes&alerts=yes"
        val jsonObjectRequest= JsonObjectRequest(Request.Method.GET,url, null, Response.Listener{
            response->
            weatherModel.clear()
            try{
                val temperature= response.getJSONObject("current").getString("temp_c")
                binding.temperature.text= resources.getString(R.string.temp_celsius,temperature)
                val isDay= response.getJSONObject("current").getInt("is_day")
                if(isDay==0){
                    Picasso.get()
                        .load("https://images.unsplash.com/photo-1572986566502-5399b7025160?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8NDd8fG1vb24lMjBzdGFyc3xlbnwwfHwwfHx8MA%3D%3D&auto=format&fit=crop&w=500&q=60")
                        .into(binding.bg)
                }else{
                    Picasso.get()
                        .load("https://plus.unsplash.com/premium_photo-1678177202541-b2fd87fe8c8c?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1304&q=80")
                        .into(binding.bg)
                }
                val condition= response.getJSONObject("current").getJSONObject("condition").getString("text")
                binding.weatherCondition.text=condition
                val icon=response.getJSONObject("current").getJSONObject("condition").getString("icon")
                Picasso.get().load("https:".plus(icon)).into(binding.weatherIcon)
                val windSpeed= response.getJSONObject("current").getString("wind_kph")
                binding.windSpeed.text= resources.getString(R.string.wind_speed, windSpeed)
                val humidity= response.getJSONObject("current").getString("humidity")
                binding.humidity.text= resources.getString(R.string.humidity, humidity)
                val clouds= response.getJSONObject("current").getString("cloud")
                binding.cloudy.text=resources.getString(R.string.clouds, clouds)
                val realFeel= response.getJSONObject("current").getString("feelslike_c")
                binding.realFeel.text= resources.getString(R.string.real_feel, realFeel)

                val forecastObj= response.getJSONObject("forecast")
                val forecast= forecastObj.getJSONArray("forecastday").getJSONObject(0)
                val hourArray= forecast.getJSONArray("hour")
                for (i in 0 until hourArray.length()){
                    val hourObj= hourArray.getJSONObject(i)
                    val time=hourObj.getString("time")
                    val temperature=hourObj.getString("temp_c")
                    val icon= hourObj.getJSONObject("condition").getString("icon")
                    val wind= hourObj.getString("wind_kph")
                    weatherModel.add(WeatherModel(time,temperature,icon,wind))

                }
                weatherAdapter.notifyDataSetChanged()


            }catch(e: JSONException){
                e.printStackTrace()


        }
        }){
            Toast.makeText(this,it.message,Toast.LENGTH_LONG).show()

        }
        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(jsonObjectRequest)

    }
}