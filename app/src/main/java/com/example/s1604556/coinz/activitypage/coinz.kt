package com.example.s1604556.coinz.activitypage

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.example.s1604556.coinz.R
import com.example.s1604556.coinz.bank.BankObject
import com.example.s1604556.coinz.bank.Bankscreen
import com.example.s1604556.coinz.downloader.DownloadCompleteRunner
import com.example.s1604556.coinz.downloader.DownloadFileTask
import com.example.s1604556.coinz.wallet.Wallet
import com.example.s1604556.coinz.wallet.WalletObject
import com.example.s1604556.coinz.wallet.WalletScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.JsonObject
import com.google.gson.JsonParser
//import com.example.s1604556.coinz.R.id.toolbar
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox.getInstance
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback


import kotlinx.android.synthetic.main.activity_coinz.*

import java.time.LocalDate

class coinz : AppCompatActivity(), OnMapReadyCallback, LocationEngineListener,PermissionsListener {
    //main page for this app, this is the map viewing page
    //the displaying mapbox functions are taken directly from lecture note
    private val tag = "MainActivity"
    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var coinList=ArrayList<Coin>()


    private lateinit var originLocation : Location
    private lateinit var permissionsManager : PermissionsManager
    private lateinit var locationEngine : LocationEngine
    private lateinit var locationLayerPlugin : LocationLayerPlugin
    //if map and current location is not ready, this is false so player cannot collect coins now
    private var _initializing : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coinz)
        //setSupportActionBar(toolbar)


        getInstance(this,"~~~~~~~~~~~API key goes here")

        mapView = findViewById(R.id.mapboxMapView)

        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

        //get the date for today
        val date: String
        if (LocalDate.now().dayOfMonth<10) {
            date = "${LocalDate.now().year}/${LocalDate.now().monthValue}/0${LocalDate.now().dayOfMonth}"
            Log.d("testing", "'$date'")
        }else{
            date = "${LocalDate.now().year}/${LocalDate.now().monthValue}/${LocalDate.now().dayOfMonth}"
            Log.d("testing", "'$date'")
        }

        //use the date today to create a new download link so tie coinzmap are up to date
        val downloadlink = "http://homepages.inf.ed.ac.uk/stg/coinz/$date/coinzmap.geojson"

        //download geojson file from the link
        DownloadFileTask(DownloadCompleteRunner).execute(downloadlink)


    }



    override fun onMapReady(mapboxMap: MapboxMap?){
        if (mapboxMap == null){
            Log.d(tag, "[onMapReady] mapboxMap is null")
        }else{
            //when preparing map, we call the this function to create the coinslist needed for displaying
            createCoinList()


            map = mapboxMap
            //set user interface options
            map?.uiSettings?.isCompassEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true

            //make location info available
            enableLocation()


            var ic: com.mapbox.mapboxsdk.annotations.Icon
            val removelist = ArrayList<Coin>()


            //for all the coins in the coin list, if any of these matches the collected coins, add to the removelist
            for (coin in coinList){
                for (id in WalletObject.collectedID){
                    if (coin.id==id){
                        removelist.add(coin)
                    }
                }
            }

            //remove those collected from coinlist
            for (coin in removelist){
                coinList.remove(coin)
            }

            //for every coin in the coinlist, make a marker to the colour required and add the marker to the map
            for (coin in coinList){

                ic = when (coin.colour) {
                    "\"#008000\"" -> IconFactory.getInstance (this).fromResource(R.drawable.green)
                    "\"#ffdf00\"" -> IconFactory.getInstance (this).fromResource(R.drawable.yellow)
                    "\"#0000ff\"" -> IconFactory.getInstance (this).fromResource(R.drawable.blue)

                    else -> IconFactory.getInstance (this).fromResource(R.drawable.red)
                }

                map?.addMarker(MarkerOptions().position(coin.position).title(coin.id).snippet(coin.currency+coin.value).icon(ic))

            }

        }
    }




    private fun enableLocation(){
        if (PermissionsManager.areLocationPermissionsGranted(this)){
            Log.d(tag,"Permissions are granted")
            initialiseLocationEngine()
            initialiseLocationLayer()
        }else{
            Log.d(tag,"Permissions are not granted")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun initialiseLocationEngine() {
        locationEngine = LocationEngineProvider(this)
                .obtainBestLocationEngineAvailable()
        locationEngine.apply {
            interval = 5000 //preferably every 5s
            fastestInterval = 1000 // at most every second
            priority = LocationEnginePriority.HIGH_ACCURACY
            activate()
        }
        val lastLocation = locationEngine.lastLocation
        if (lastLocation != null) {
            originLocation = lastLocation
            setCameraPosition(lastLocation)
        } else {
            locationEngine.addLocationEngineListener(this)
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun initialiseLocationLayer() {
        if (mapView == null) {
            Log.d(tag, "mapView is null")
        } else {
            if (map == null) {
                Log.d(tag, "map is null")
            } else {
                locationLayerPlugin = LocationLayerPlugin(mapView!!, map!!,locationEngine)
                locationLayerPlugin.apply {
                    setLocationLayerEnabled(true)
                    cameraMode = CameraMode.TRACKING
                    renderMode = RenderMode.NORMAL
                }

            }

        }
    }

    private fun setCameraPosition(location: Location){
        val latLng = LatLng (location.latitude, location.longitude)
        map?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    override fun onLocationChanged(location: Location?){
        if (location == null){
            Log.d(tag,"[onLocationChanged] location is null")
        }else{
            //if originallocation is not found or initialized, we proceed to obtain current location
            //otherwise, meausre the distance between current position and previous position and store it for achievement uses
            if(::originLocation.isInitialized){
                val prevLatLng=LatLng(originLocation.latitude,originLocation.longitude)
                val currentLatLng=LatLng(location.latitude,location.longitude)
                val auth = FirebaseAuth.getInstance()
                WalletObject.Distance=WalletObject.Distance+(prevLatLng.distanceTo(currentLatLng))
                val distance = FirebaseDatabase.getInstance().reference
                        .child("users").child(auth.currentUser?.uid!!).child("distanceTravelled")
                distance.setValue(WalletObject.Distance)
                originLocation = location
                setCameraPosition(originLocation)
                //making sure initialising is successful, so that button press does not crash the app
                _initializing= true
            }else {
                originLocation = location
                setCameraPosition(originLocation)
                _initializing = true                                     //making sure initialising is successful, so that button press does not crash the app
            }
        }
    }

    @SuppressWarnings ("MissingPermission")
    override fun onConnected(){
        Log.d(tag,"[onConnected] requesting location updates")
        locationEngine.requestLocationUpdates()
    }

    override fun onExplanationNeeded(permissionsToExplain:
     MutableList<String>?){
        Log.d(tag,"Permissions: $permissionsToExplain")
        //present popup message or dialog
    }

    override fun onPermissionResult(granted: Boolean){
        Log.d(tag, "[onPermissionResult] granted == $granted")
        if (granted){
            enableLocation()
        }else{
            //open a dialogue with the user
        }
    }




    override  fun onStart(){

        super.onStart()
        mapView?.onStart()


        collect.setOnClickListener {
            if (!_initializing) {
                //so if initializing is still false. if collect button is pressed, we tell user that they need to wait
                Toast.makeText(this@coinz, "Please wait while locate your position", Toast.LENGTH_SHORT).show()
                //this part is to make sure button are not allowing double clicking, avoding starting multiple same activity or crashes
                //this apply to all postDelay used, not repeating this comment for other buttons
                collect.isClickable=false
                Handler().postDelayed({
                    collect.isClickable=true
                },500)

            }else{
                //if current postion is found, we get the player position and call Wallet Object to detect any coins that are within 25m radius
                val playerposition=LatLng(originLocation.latitude,originLocation.longitude)
                //return a list with left coins
                val newlist= WalletObject.collectingCoins(playerposition,coinList)
                collect.isClickable=false
                Handler().postDelayed({
                    collect.isClickable=true
                },500)

                if(WalletObject.wallet.currentNo==WalletObject.wallet.limit) {
                    //if we have reached the wallet limit, display a message to tell the user
                    Toast.makeText(this@coinz, "Your have reached your wallet limit, considering update the limit", Toast.LENGTH_SHORT).show()
                    renewMap(newlist)
                }else{
                    //renew map with remaining coins
                    renewMap(newlist)
                }
            }

        }

        wallet.setOnClickListener{
            //start wallet activity
            val intent = Intent(this, WalletScreen::class.java)
            startActivity(intent)
            wallet.isClickable=false
            Handler().postDelayed({
                wallet.isClickable=true
            },2000)
        }

        bank.setOnClickListener{
            //start bank activity
            val intent = Intent(this, Bankscreen::class.java)
            startActivity(intent)
            bank.isClickable=false
            Handler().postDelayed({
                bank.isClickable=true
            },2000)

        }

        achievement.setOnClickListener{
            //start achievement activity
            val intent = Intent(this, Achievement::class.java)
            startActivity(intent)
            achievement.isClickable=false
            Handler().postDelayed({
                bank.isClickable=true
            },2000)
        }

    }



    private fun renewMap(coins:ArrayList<Coin>){
        var ic: com.mapbox.mapboxsdk.annotations.Icon
        //first clear the map, then add new makers to the map
        map?.clear()
        for (coin in coins){

            ic = when (coin.colour) {
                "\"#008000\"" -> IconFactory.getInstance (this).fromResource(R.drawable.green)
                "\"#ffdf00\"" -> IconFactory.getInstance (this).fromResource(R.drawable.yellow)
                "\"#0000ff\"" -> IconFactory.getInstance (this).fromResource(R.drawable.blue)

                else -> IconFactory.getInstance (this).fromResource(R.drawable.red)
            }

            map?.addMarker(MarkerOptions().position(coin.position).title(coin.id).snippet(coin.currency+coin.value).icon(ic))

        }
        coinList=coins

    }

    private fun createCoinList(){
        //as in lecture slides, extract information from geojson file and make it into a list of coins
        val fc = FeatureCollection.fromJson(DownloadCompleteRunner.result)
        val feature =fc.features()
        //extract exchange rate
        val parser = JsonParser()
        val jobject=parser.parse(DownloadCompleteRunner.result) as JsonObject
        val ratelist =jobject.get("rates") as JsonObject
        BankObject.quidRate= ratelist.get("QUID").toString().toDouble()
        BankObject.dollarRate=ratelist.get("DOLR").toString().toDouble()
        BankObject.penyRate=ratelist.get("PENY").toString().toDouble()
        BankObject.shilRate=ratelist.get("SHIL").toString().toDouble()


        for (item in feature!!){
            val p=item.geometry() as Point
            val c=LatLng(p.latitude(),p.longitude())
            val j=item.properties()
            val currency= j?.get("currency").toString().drop(1).dropLast(1)

            val id= j?.get("id").toString().drop(1).dropLast(1)
            val value=j?.get("value").toString().drop(1).dropLast(1)

            val markerColour=j?.get("marker-color").toString()

            coinList.add(Coin(id, currency, value, c, markerColour))
        }

    }

}
