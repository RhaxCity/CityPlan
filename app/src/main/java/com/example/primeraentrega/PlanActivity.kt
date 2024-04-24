package com.example.primeraentrega

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.example.primeraentrega.Clases.Plan
import com.example.primeraentrega.databinding.ActivityPlanBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.min
import android.hardware.SensorEventListener
import org.osmdroid.views.overlay.TilesOverlay

class PlanActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding : ActivityPlanBinding

    //PARA POSICIONES
    private var posActualGEO = GeoPoint(4.0, 72.0)
    private var latActual:Double= 4.0
    private var longActual:Double= 72.0
    private var posEncuentroGEO = GeoPoint(-122.084, 37.4219983 )
    private var latEncuentro:Double= -122.0
    private var longEncuentro:Double= 37.0
    private var pasosAvtivado=true
    private var EstoyEnElPlan=true
    private lateinit var roadManager: RoadManager
    private var firstTime=true

    private val localPermissionName=android.Manifest.permission.ACCESS_FINE_LOCATION;
    lateinit var location: FusedLocationProviderClient

    //MAPA
    lateinit var map : MapView
    private lateinit var geocoder: Geocoder

    //Sensores
    private lateinit var sensorManager: SensorManager
    //Para contar pasos
    private var stepSensor : Sensor? = null
    private lateinit var stepSensorEventListener: SensorEventListener
    private var running = false
    private var totalSteps = 0f
    private var previousTotalSteps = 0f

    //SENSOR luz
    private lateinit var lightSensor : Sensor
    private lateinit var lightEventListener: SensorEventListener
    //Sensor Temperatura
    private  var temperatureSensor: Sensor? = null
    private lateinit var tempEventListener: SensorEventListener


    val permissionRequest= registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {
            locationSettings()
        })

    //evaluar el gps . si esta prendido o no no existe
    fun locationSettings()
    {
        val builder= LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            startLocationUpdates()
        }
        task.addOnFailureListener{
            if(it is ResolvableApiException)
            {
                try{
                    val isr: IntentSenderRequest = IntentSenderRequest.Builder(it.resolution).build()
                    locationSettings.launch(isr)
                }
                catch (sendEx: IntentSender.SendIntentException)
                {
                    //ignore the error
                }

            }
            else
            {
                Toast.makeText(getApplicationContext(), "there is no gps hardware", Toast.LENGTH_LONG).show();
            }
        }
    }

    val locationSettings= registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        ActivityResultCallback {
            if(it.
                resultCode ==
                RESULT_OK){

                startLocationUpdates()
            }else{
                Toast.makeText(getApplicationContext(), "GPS TURNED OFF", Toast.LENGTH_LONG).show();
            }
        })

    //LOCALIZACION
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallBack: LocationCallback

    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                localPermissionName
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            location.requestLocationUpdates(
                locationRequest,
                locationCallBack,
                Looper.getMainLooper()
            )
        } else {
            Toast.makeText(getApplicationContext(), "NO HAY PERMISO", Toast.LENGTH_LONG).show();
        }
    }

    private  fun createLocationCallback():LocationCallback
    {
        val locationCallback=object: LocationCallback()//clase anonima en kotlin
        //heredar y sobreescribir sobre la misma linea
        {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                val last=result.lastLocation
                if(last!=null)
                {
                    //Toast.makeText(getApplicationContext(), "($last.latitude , $last.longitude)", Toast.LENGTH_LONG).show();
                    latActual=last.latitude
                    longActual=last.longitude
                    posActualGEO=GeoPoint(latActual, longActual)
                    if(EstoyEnElPlan)
                    {
                        myLocationOnMap(posActualGEO)
                        if(firstTime)
                        {
                            firstTime=false
                            map.controller.animateTo(posActualGEO)
                            map.controller.setZoom(19.0)
                        }
                    }
                }
            }
        }

        return locationCallback
    }
    fun gestionarPermiso()
    {
        if(ActivityCompat.checkSelfPermission(this, localPermissionName)== PackageManager.PERMISSION_DENIED)
        {
            if(shouldShowRequestPermissionRationale(localPermissionName))
            {
                Toast.makeText(getApplicationContext(), "The app requires access to location", Toast.LENGTH_LONG).show();
            }
            permissionRequest.launch(localPermissionName)
        }
        else
        {

            startLocationUpdates()
        }
    }


    fun gestionarPermisoActividad() {
        val permissionName = android.Manifest.permission.ACTIVITY_RECOGNITION

        if (ActivityCompat.checkSelfPermission(this, permissionName) == PackageManager.PERMISSION_DENIED) {
            if (shouldShowRequestPermissionRationale(permissionName)) {
                // Mostrar un mensaje explicativo si es necesario
                Toast.makeText(getApplicationContext(), "La aplicación requiere permiso de reconocimiento de actividad", Toast.LENGTH_LONG).show()
            }
            permissionRequest.launch(permissionName)
        }
    }

    private lateinit var idPlan : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idPlan=intent.getStringExtra("idPlan").toString()

        Log.e(TAG, "revisar $idPlan")

        configurarMapa()

        configurarLocalizacion()

        configurarConFireBase()

        configurarBotones();

        activarOMS()

        configurarSensores()


    }
    private fun configurarSensores(){
        //Sensores
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        //Sensor Luz
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!
        lightEventListener = createLightSensorListener()
        //Sensor Pasos
        loadData()
        resetSteps()


    }

    val db = Firebase.firestore
    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference

    private fun configurarConFireBase() {

        Log.d(ContentValues.TAG, "entreee $idPlan")

        val docRef = db.collection("Planes").document(idPlan)
        docRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    Log.d(ContentValues.TAG, "encontrado - ${documentSnapshot.id} => ${documentSnapshot.data}")
                    // Aquí puedes acceder a los datos del documento utilizando document.data
                    val plan = documentSnapshot.toObject<Plan>()
                    //AQUI SE OBTIENE LA INFORMACION PARA PONER
                    //TITULO DEL PLAN
                    var titulo= plan?.titulo
                    binding.tituloPlan.setText(titulo)
                    //UBICACION DEL PLAN
                    if (plan != null) {
                        Log.d(ContentValues.TAG, "${plan.latitude} y tambien ${plan.longitude} ")
                        latEncuentro=plan.latitude
                        longEncuentro=plan.longitude
                        posEncuentroGEO=GeoPoint(latEncuentro, longEncuentro)
                        ponerUbicacionPlan()
                    }
                    //SI TIENE LO DE NUMERO DE PASOS
                    if (plan != null) {
                        pasosAvtivado=plan.AmigoMasActivo
                        if(!pasosAvtivado){
                            // Hacer invisible el elemento binding.hazDado
                            binding.hazDado.visibility = View.INVISIBLE

                            // Hacer invisible el elemento binding.pasoscantText
                            binding.pasoscantText.visibility = View.INVISIBLE
                        }
                    }

                    val pathReferencePin = plan?.let { storageRef.child(it.fotopin) }

                    val ONE_MEGABYTE: Long = 1024 * 1024
                    pathReferencePin?.getBytes(ONE_MEGABYTE)?.addOnSuccessListener { bytes ->
                        // Los bytes de la imagen se han recuperado exitosamente
                        val imageStream = ByteArrayInputStream(bytes)
                        //loadImage(imageStream)
                    }?.addOnFailureListener {
                        // Manejar cualquier error que ocurra durante la recuperación de la imagen
                    }

                    //ESTO ES DEL USUARIO
                    if(EstoyEnElPlan)
                    {
                        binding.switchPasos.isChecked=true
                        binding.aunsiguesText.setText("Aun sigues en el plan")
                    }
                    else
                    {
                        binding.aunsiguesText.setText("Estas fuera del plan")
                        binding.switchPasos.isChecked=false
                        // Hacer invisible el elemento binding.hazDado
                        binding.hazDado.visibility = View.INVISIBLE

                        // Hacer invisible el elemento binding.pasoscantText
                        binding.pasoscantText.visibility = View.INVISIBLE

                        binding.mostrarRutabutton.isVisible= false
                        binding.milocalizacion.isVisible=false
                        stopLocationUpdates()
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.d(ContentValues.TAG, "get failed with ", exception)
            }
    }
    fun createStepSensorListener() : SensorEventListener {
        return object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (running && event != null && event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                    // Incrementar el contador de pasos cada vez que se detecta un paso
                    binding.pasoscantText.text = (binding.pasoscantText.text.toString().toInt() + 1).toString()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No necesitas hacer nada aquí para este caso
            }
        }
    }
    fun createLightSensorListener() : SensorEventListener{
        val ret : SensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if(this@PlanActivity::map.isInitialized){
                    if (event != null && event.sensor.type == Sensor.TYPE_LIGHT) {
                        if(event.values[0] < 1500){
                            // Cambiar a modo oscuro
                            map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
                        }else{
                            // Cambiar a modo claro
                            map.getOverlayManager().getTilesOverlay().setColorFilter(null);
                        }
                    }
                }
            }
            override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            }
        }
        return ret
    }

    fun createTemperatureSensorListener() : SensorEventListener {
        val ret : SensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                    val temperatura = event.values[0]
                    val resource = when {
                        temperatura < 0 -> {
                            R.drawable.nevando
                        }
                        temperatura < 15 -> {
                            R.drawable.muynublado
                        }
                        temperatura < 20 -> {
                            R.drawable.parcialmentenublado
                        }
                        else -> {
                            R.drawable.soleado
                        }
                    }
                    binding.imagenTemperatura.setImageResource(resource)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No necesitas hacer nada aquí para este caso
            }
        }
        return ret
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        running = true
        // Registrar el SensorEventListener para el sensor de pasos
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor == null) {
            Toast.makeText(this, "No se detectó sensor de pasos", Toast.LENGTH_SHORT).show()
        } else {
            Log.i("Sensor", "Hay podómetro para pasos")
            stepSensorEventListener = createStepSensorListener()
            sensorManager.registerListener(stepSensorEventListener, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }

        // Sensor temperatura
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        if (temperatureSensor == null) {
            Toast.makeText(this, "No se detectó sensor de temperatura", Toast.LENGTH_SHORT).show()
        } else {
            Log.i("Sensor", "Hay sensor de temperatura")
            tempEventListener = createTemperatureSensorListener()
            sensorManager.registerListener(tempEventListener, temperatureSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Registrar el SensorEventListener para el sensor de luz
        sensorManager.registerListener(lightEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)

        map.onResume()
        map.controller.setZoom(19.0)
        map.controller.animateTo(posActualGEO)
        startLocationUpdates()
    }
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(running){
            totalSteps = event!!.values[0]
            val currentSteps = totalSteps.toInt() - previousTotalSteps.toInt()
            binding.pasoscantText.text = ("$currentSteps")

        }
    }
    fun resetSteps(){
        previousTotalSteps = totalSteps
        binding.pasoscantText.text = 0.toString()
        saveData()
    }
    fun saveData(){
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("Key", previousTotalSteps)
        editor.apply()
    }

    private fun loadData(){
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val savedNumber = sharedPreferences.getFloat("KEY", 0f)
        Log.d("PlanActivity","$savedNumber")
        previousTotalSteps = savedNumber
    }
    private fun stopLocationUpdates() {
        location.removeLocationUpdates(locationCallBack)
    }

    private fun  configurarBotones()
    {
        binding.configuraciones.setOnClickListener{

            val intent=Intent(baseContext,EditarPlanActivity::class.java)
            intent.putExtra("idPlan",idPlan)
            startActivity(intent)
        }

        binding.botonCamara.setOnClickListener{
            startActivity(Intent(baseContext,GaleriaActivity::class.java))
        }

        binding.botonVolverAPlanes.setOnClickListener{
            startActivity(Intent(baseContext,PlanesActivity::class.java))
        }

    }

    private var switchRuta=false
    private fun configurarLocalizacion() {

        location= LocationServices.getFusedLocationProviderClient(this);
        locationRequest=createLocationRequest()
        locationCallBack=createLocationCallback()

        //primero gestionar los permisos
        gestionarPermiso()
        gestionarPermisoActividad()
        ponerUbicacionPlan()



        binding.mostrarRutabutton.setOnClickListener{
            //muestra la ruta con oms bonus
            if(!switchRuta) {
                switchRuta=true
                binding.mostrarRutabutton.setText("Quitar ruta")
                mostrarRuta(posActualGEO, posEncuentroGEO)
            }
            else
            {
                switchRuta=false
                //quita la ruta si esta existe
                if(roadOverlay != null){
                    map.getOverlays().remove(roadOverlay);
                }
                binding.mostrarRutabutton.setText("Mostrar ruta")
            }
        }

        binding.puntoEncuentro.setOnClickListener{
            //lo centra a la ubicacion del punto de encuentro
            map.controller.setZoom(19.0)
            map.controller.animateTo(posEncuentroGEO)
        }

        binding.milocalizacion.setOnClickListener{
            //lo centra a la ubicacion de mi ubicacion actual
            //centrarse
            map.controller.setZoom(19.0)
            map.controller.animateTo(posActualGEO)
        }

        //ver si esta prendido o apagado
        binding.switchPasos.setOnClickListener {
            if(binding.switchPasos.isChecked)
            {
                if(pasosAvtivado)
                {
                    binding.hazDado.visibility = View.VISIBLE
                    // Hacer invisible el elemento binding.pasoscantText
                    binding.pasoscantText.visibility = View.VISIBLE
                }
                binding.mostrarRutabutton.setText("Mostrar ruta")
                binding.milocalizacion.isVisible=true
                binding.mostrarRutabutton.isVisible= true
                binding.aunsiguesText.setText("Aun sigues en el plan")
                EstoyEnElPlan=true
                startLocationUpdates()
                myLocationOnMap(posActualGEO)
                map.controller.setZoom(19.0)
                map.controller.animateTo(posActualGEO)
            }
            else
            {

                binding.hazDado.visibility = View.INVISIBLE
                binding.milocalizacion.isVisible=false
                // Hacer invisible el elemento binding.pasoscantText
                binding.pasoscantText.visibility = View.INVISIBLE

                binding.mostrarRutabutton.isVisible= false
                binding.aunsiguesText.setText("Estas fuera del plan")
                stopLocationUpdates()
                if(myLocationMarker!=null)
                    map.overlays.remove(myLocationMarker)
                if(roadOverlay != null){
                    map.getOverlays().remove(roadOverlay);
                }
            }
        }
    }

    private fun ponerUbicacionPlan() {
        Log.d(ContentValues.TAG, "$posEncuentroGEO A VER QUE ")
        planLocationOnMap(posEncuentroGEO)
    }

    private fun createLocationRequest():LocationRequest
    {
        val request=LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 7000)
            .setMinUpdateIntervalMillis(2000)
            .setWaitForAccurateLocation(true)
            .build()

        return request
    }


    //MARCADORES
    private var  myLocationMarker: Marker? = null
    fun myLocationOnMap(p: GeoPoint){
        if(myLocationMarker!=null)
            map.overlays.remove(myLocationMarker)
        val address = findAddress(LatLng(p.latitude, p.longitude))
        val snippet : String
        if(address!=null) {
            snippet = address
        }else{
            snippet = ""
        }
        addMarker(p, snippet, 0)
    }

    private var  planLocationMarker: Marker? = null
    fun planLocationOnMap(p: GeoPoint){
        if(planLocationMarker!=null)
            map.overlays.remove(planLocationMarker)
        val address = findAddress(LatLng(p.latitude, p.longitude))
        val snippet : String
        if(address!=null) {
            snippet = address
        }else{
            snippet = ""
        }
        addMarker(p, snippet, 1)
    }
    fun findAddress (location : LatLng):String?{
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 2)
        if(addresses != null && !addresses.isEmpty()){
            val addr = addresses.get(0)
            val locname = addr.getAddressLine(0)
            return locname
        }
        return null
    }

    fun addMarker(p: GeoPoint, snippet : String, tipo : Int){

        //MY LOCATION
        if(tipo==0){
            myLocationMarker = createMarker(p, "Tu ubicacion", snippet, R.drawable.pinyo)

            if (myLocationMarker != null) {
                map.getOverlays().add(myLocationMarker)
            }
        }
        else //localizacion plan
        {
            Log.i("MapsApp", "Route length: $p km")
            planLocationMarker = createMarkerEncuentro(p, "Tu destino", snippet, R.drawable.iconopin)

            if (planLocationMarker != null) {
                map.getOverlays().add(planLocationMarker)
            }
        }
    }

    val sizeInDp = 70
    //val sizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizeInDp.toFloat(), resources.displayMetrics).toInt()
    @SuppressLint("UseCompatLoadingForDrawables")
    fun createMarkerEncuentro(p: GeoPoint, title: String, desc: String, iconID : Int) : Marker? {
        var marker : Marker? = null;
        if(map!=null) {
            marker = Marker(map);
            if (title != null) marker.setTitle(title);
            if (desc != null) marker.setSubDescription(desc);
            if (iconID != 0) {
                val MAX_ICON_SIZE = 200
                Thread(Runnable {
                    val originalBitmap = BitmapFactory.decodeResource(resources, iconID)

                    // Redimensionar la imagen manteniendo la forma circular
                    val resizedBitmap = if (originalBitmap.width > MAX_ICON_SIZE || originalBitmap.height > MAX_ICON_SIZE) {
                        val maxSize = min(originalBitmap.width, originalBitmap.height)
                        val scaleFactor = MAX_ICON_SIZE.toFloat() / maxSize
                        val scaledWidth = (originalBitmap.width * scaleFactor).toInt()
                        val scaledHeight = (originalBitmap.height * scaleFactor).toInt()
                        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)

                        // Recortar el bitmap para mantener la forma circular
                        val outputBitmap = Bitmap.createBitmap(scaledBitmap.width, scaledBitmap.height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(outputBitmap)
                        val paint = Paint().apply {
                            isAntiAlias = true
                            shader = BitmapShader(scaledBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                        }
                        val diameter = min(scaledBitmap.width, scaledBitmap.height).toFloat()
                        canvas.drawCircle(scaledBitmap.width / 2f, scaledBitmap.height / 2f, diameter / 2, paint)

                        outputBitmap
                    } else {
                        val maxSize = min(originalBitmap.width, originalBitmap.height)
                        val scaleFactor = MAX_ICON_SIZE.toFloat() / maxSize
                        val scaledWidth = (originalBitmap.width * scaleFactor).toInt()
                        val scaledHeight = (originalBitmap.height * scaleFactor).toInt()
                        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)

                        // Recortar el bitmap para mantener la forma circular
                        val outputBitmap = Bitmap.createBitmap(scaledBitmap.width, scaledBitmap.height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(outputBitmap)
                        val paint = Paint().apply {
                            isAntiAlias = true
                            shader = BitmapShader(scaledBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                        }
                        val diameter = min(scaledBitmap.width, scaledBitmap.height).toFloat()
                        canvas.drawCircle(scaledBitmap.width / 2f, scaledBitmap.height / 2f, diameter / 2, paint)

                        outputBitmap
                    }

                    // Comprimir y reducir la calidad de la imagen
                    val compressedBitmapStream = ByteArrayOutputStream()
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, compressedBitmapStream) // Aquí puedes ajustar el nivel de compresión (0-100)

                    // Convertir el bitmap comprimido en un BitmapDrawable
                    val compressedBitmap = BitmapFactory.decodeStream(ByteArrayInputStream(compressedBitmapStream.toByteArray()))
                    val drawable = BitmapDrawable(resources, compressedBitmap)

                    // Establecer el BitmapDrawable como el icono del Marker
                    marker.setIcon(drawable)

                }).start()
            }

            marker.setPosition(p)
            Log.i("MARKER ENCUENTRO", "Route length: "+p+" km")
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            marker.setAnchor(
                Marker.
                ANCHOR_CENTER, Marker.
                ANCHOR_BOTTOM);
        }
        return marker
    }

    fun createMarker(p: GeoPoint, title: String, desc: String, iconID : Int) : Marker? {
        var marker : Marker? = null;
        if(map!=null) {
            marker = Marker(map);
            if (title != null) marker.setTitle(title);
            if (desc != null) marker.setSubDescription(desc);
            if (iconID != 0) {
                val bitmap = getBitmapFromDrawable(iconID, 60)
                val drawable = BitmapDrawable(resources, bitmap)
                marker.icon = drawable
            }
            marker.setPosition(p)
            Log.i("MARKER", "Route length: "+p+" km")
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.setAnchor(
                Marker.
                ANCHOR_CENTER, Marker.
                ANCHOR_BOTTOM);
        }
        return marker
    }
    private fun getBitmapFromDrawable(iconID: Int, sizeInDp: Int): Bitmap {
        val sizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizeInDp.toFloat(), resources.displayMetrics).toInt()
        val drawable = BitmapFactory.decodeResource(resources, iconID)
        return Bitmap.createScaledBitmap(drawable, sizeInPixels, sizeInPixels, false)
    }
    private fun scaleBitmap(bitmap: Bitmap, size: Int): Bitmap {
        val scale = size.toFloat() / bitmap.width.toFloat()
        val width = (bitmap.width * scale).toInt()
        val height = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.isAntiAlias = true
        paint.shader = shader
        canvas.drawCircle(bitmap.width / 2f, bitmap.height / 2f, (bitmap.width / 2).toFloat(), paint)
        return output
    }

    //MAPA
    private fun configurarMapa() {
        Configuration.getInstance().load(this,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))
        geocoder = Geocoder(baseContext)
        map = binding.osmMap
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        //map.overlays.add(createOverlayEvents())
    }


    //OMS Y CREAR RUTA
    private fun activarOMS() {
        roadManager = OSRMRoadManager(this, "ANDROID")
    }

    private var roadOverlay: Polyline? = null
    private fun mostrarRuta(start : GeoPoint, finish : GeoPoint){

        Thread(Runnable {var routePoints = ArrayList<GeoPoint>()
            routePoints.add(start)
            routePoints.add(finish)
            val road = roadManager.getRoad(routePoints)
            Log.i("MapsApp", "Route length: "+road.mLength+" km")
            Log.i("MapsApp", "Duration: "+road.mDuration/60+" min")
            if(map!=null){
                if(roadOverlay != null){
                    map.getOverlays().remove(roadOverlay);
                }
                roadOverlay = RoadManager.buildRoadOverlay(road)
                roadOverlay!!.getOutlinePaint().setColor(
                    Color.
                    RED)
                roadOverlay!!.getOutlinePaint().setStrokeWidth(10F)
                map.getOverlays().add(roadOverlay)
            }

        }).start()
    }
}