package com.uwi.btmap

import android.annotation.SuppressLint
import android.graphics.Color.parseColor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression.*
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.reroute.RerouteController
import kotlin.math.log

class MainActivity :
    AppCompatActivity(),
    OnMapReadyCallback,
    PermissionsListener,
    MapboxMap.OnMapLongClickListener{

    private val TAG = "Main Activity"
    
    private var mapboxNavigation: MapboxNavigation? = null

    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private lateinit var mapboxMap: MapboxMap

    private val ORIGIN_COLOR = "#32A852"
    private val DESTINATION_COLOR = "#F84D4D"

    private var mapView: MapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))

        setContentView(R.layout.activity_main)

//        mapview reference setup
        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

//        navigation object setup
        val mapboxNavigationOptions = MapboxNavigation
                .defaultNavigationOptionsBuilder(this, getString(R.string.mapbox_access_token))
                .build()

        mapboxNavigation = MapboxNavigation(mapboxNavigationOptions)
}

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            this.mapboxMap = mapboxMap
//            show user location
            enableLocationComponent(it)

//            add sources which are responsible for showing data on the map
//            click_source for the clicked location marker
            it.addSource(GeoJsonSource("CLICK_SOURCE"))
            it.addSource(GeoJsonSource(
                    "ROUTE_LINE_SOURCE_ID",
                    GeoJsonOptions().withLineMetrics(true)
            ))

            it.addImage("ICON_ID",
                    BitmapUtils.getBitmapFromDrawable(
                            ContextCompat.getDrawable(
                                    this,
                                    R.drawable.mapbox_marker_icon_default
                    )
                )!!
            )

//            add the layers the sources will be displayed on
            it.addLayerBelow(
                    LineLayer("ROUTE_LAYER_ID", "ROUTE_LINE_SOURCE_ID")
                            .withProperties(
                                    lineCap(Property.LINE_CAP_ROUND),
                                    lineJoin(Property.LINE_JOIN_ROUND),
                                    lineWidth(6f),
                                    lineOpacity(1f),
                                    lineColor("#2E4FC9")
//                                    lineGradient(
//                                            interpolate(
//                                                    linear(),
//                                                    lineProgress(),
//                                                    stop(0f, color(parseColor(ORIGIN_COLOR))),
//                                                    stop(1f, color(parseColor(DESTINATION_COLOR)))
//                                            )
//                                    )
                            ),
                    "mapbox-location-shadow-layer"
            )

            it.addLayerAbove(
                    SymbolLayer("CLICK_LAYER","CLICK_SOURCE")
                            .withProperties(
                                    iconImage("ICON_ID")
                            ),
                    "ROUTE_LAYER_ID"
            )

            mapboxMap.addOnMapLongClickListener(this)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style){
        mapboxMap.getStyle {
            if(PermissionsManager.areLocationPermissionsGranted(this)){
                val customLocationComponentOptions = LocationComponentOptions.builder(this)
                        .trackingGesturesManagement(true)
                        .accuracyColor(ContextCompat.getColor(this,R.color.mapbox_blue))
                        .build()

                val locationComponentActivationOptions =
                        LocationComponentActivationOptions.builder(this, loadedMapStyle)
                                .locationComponentOptions(customLocationComponentOptions)
                                .build()

                mapboxMap.locationComponent.apply {
                    activateLocationComponent(locationComponentActivationOptions)
                    isLocationComponentEnabled = true
                    cameraMode = CameraMode.TRACKING
                    renderMode = RenderMode.COMPASS

                    val lat = mapboxMap.locationComponent.lastKnownLocation?.latitude
                    val lng = mapboxMap.locationComponent.lastKnownLocation?.longitude

                    val position = CameraPosition.Builder()
                            .zoom(12.0)
                            .tilt(0.0)
                            .target(LatLng(lat!!,lng!!))
                            .build()

                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position))
                }
            }else{
                permissionsManager = PermissionsManager(this)
                permissionsManager.requestLocationPermissions(this)
            }
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(this, "We need location services :/",Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if(granted){
            enableLocationComponent(mapboxMap.style!!)
        }else{
            Toast.makeText(this, "permissions not granted",Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onMapLongClick(point: LatLng): Boolean {
        val destinationLocation: Point = Point.fromLngLat(point.longitude,point.latitude)


        mapboxMap?.getStyle{
            val clickPointSource = it.getSourceAs<GeoJsonSource>("CLICK_SOURCE")
//            set the location of the clicked location marker
            clickPointSource?.setGeoJson(destinationLocation)
        }

//        generate the route
        mapboxMap?.locationComponent?.lastKnownLocation?.let{ originLatLng ->

            val originLocation: Point = Point.fromLngLat(originLatLng.longitude,originLatLng.latitude)

            val routeOptions : RouteOptions = RouteOptions.builder()
                    .applyDefaultParams()
                    .accessToken(getString(R.string.mapbox_access_token))
                    .coordinates(originLocation,null,destinationLocation)
                    .alternatives(true)
                    .profile(DirectionsCriteria.PROFILE_DRIVING)
                    .build()


            mapboxNavigation?.requestRoutes(
                    routeOptions,
                    routesReqCallback
            )
        }
        return true
    }

    private val routesReqCallback = object : RoutesRequestCallback{
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()){
                mapboxMap?.getStyle {
                    val clickPointSource = it.getSourceAs<GeoJsonSource>("ROUTE_LINE_SOURCE_ID")
                    val routeLineString = LineString.fromPolyline(
                            routes[0].geometry()!!,
                            6
                    )
                    Log.d(TAG, "onRoutesReady: routeLineString: "+routeLineString.toString())
//                    add the returned route to the route line source
                    clickPointSource?.setGeoJson(routeLineString)
                }
            }else{
                Log.d(TAG, "onRoutesReady: No routes found")
            }
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            Log.d(TAG, "onRoutesRequestCanceled: routes request cancelled")
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            Log.e(TAG, "onRoutesRequestFailure: routes request failed: ", throwable)
        }

    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }


}