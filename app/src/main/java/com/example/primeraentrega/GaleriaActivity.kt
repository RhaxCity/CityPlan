package com.example.primeraentrega

import PhotoGalleryAdapter
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.primeraentrega.Clases.GridSpacingItemDecoration
import com.example.primeraentrega.databinding.ActivityGaleriaBinding
import com.example.primeraentrega.Clases.Usuario
import com.google.firebase.auth.FirebaseAuth
import java.io.File


class GaleriaActivity : AppCompatActivity() {

    private lateinit var binding : ActivityGaleriaBinding
    private lateinit var uriCamera : Uri
    private val REQUEST_CAMERA_PERMISSION = 100
    private val photoList = mutableListOf<Uri>()
    private lateinit var photoGalleryAdapter: PhotoGalleryAdapter
    private var isFabOpen=false
    private var rotation=false
    private lateinit var idGrupo : String

    val getContentGallery = registerForActivityResult(
        ActivityResultContracts.GetContent(),
        ActivityResultCallback {
            loadImage(it)
        })

    val getContentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
        ActivityResultCallback {
            if(it){
                loadImage(uriCamera)
            }
        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGaleriaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idGrupo = intent.getStringExtra("idGrupo").toString()

        val file = File(getFilesDir(), "picFromCamera")
        uriCamera = FileProvider.getUriForFile(baseContext, baseContext.packageName + ".fileprovider", file)

        // Configurar RecyclerView
        val layoutManager = GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return 1 // Devolver 1 para que cada elemento ocupe una columna
            }
        }
        binding.photoGalleryRecyclerView.layoutManager = layoutManager

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.spacing) // Definir el espaciado entre las imágenes
        binding.photoGalleryRecyclerView.addItemDecoration(GridSpacingItemDecoration(3, spacingInPixels, true))

        photoGalleryAdapter = PhotoGalleryAdapter(photoList)
        binding.photoGalleryRecyclerView.adapter = photoGalleryAdapter

        binding.buttonSeleccionarFoto.setOnClickListener {
            getContentGallery.launch("image/*")
        }

        val usuario: Usuario = Usuario()
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Grupos_bar -> {
                    // Respond to navigation item 1 click
                    startActivity(Intent(baseContext, VerGruposActivity::class.java))
                    true
                }
                R.id.cuenta_bar -> {
                    // Respond to navigation item 2 click
                    var intent = Intent(baseContext, PerfilConfActivity::class.java)
                    intent.putExtra("user", usuario)
                    startActivity(intent)
                    true
                }
                R.id.salir_bar -> {
                    // Respond to navigation item 3 click
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(baseContext, IniciarSesionActivity::class.java))
                    true
                }
                else -> false
            }
        }

        initShowout(binding.pasadosView)
        initShowout(binding.nuevoView)
        initShowout(binding.activoView)
        initShowout(binding.planesView)
        binding.fabMenuPlan.setOnClickListener {
            if (!isFabOpen) {
                showFabMenu();
            } else {
                closeFabMenu();
            }
        }

        fabClicks()
    }


    private fun fabClicks() {
        binding.fabPlanesPasados.setOnClickListener {
            var intent = Intent(baseContext, PlanesPasadosActivity::class.java)
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
        }

        binding.fabCrearPlan.setOnClickListener {
            var intent = Intent(baseContext, CrearPlanActivity::class.java)
            intent.putExtra("pantalla", "planes")
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
        }

        binding.fabMisPlanes.setOnClickListener {
            var intent = Intent(baseContext, PlanesActivity::class.java)
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
        }

        binding.fabPlanActivo.setOnClickListener {
            var intent = Intent(baseContext, PlanActivity::class.java)
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
        }
    }

    private fun initShowout (v: View){
        v.apply {
            visibility = View. GONE
            translationY= height.toFloat()
            alpha = 0f
        }
    }

    private fun closeFabMenu() {
        rotation=rotateFAB()
        isFabOpen=false
        cerrar(binding.pasadosView)
        cerrar(binding.nuevoView)
        cerrar(binding.activoView)
        cerrar(binding.planesView)
    }

    private fun cerrar(view: View) {
        view.apply {
            visibility= View.VISIBLE
            alpha=1f
            translationY=0f
            animate()
                .setDuration(200)
                .translationY(0f)
                .setListener(object : AnimatorListenerAdapter(){
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        visibility= View.GONE
                    }
                })
                .alpha(0f)
                .start()
        }
    }

    private fun showFabMenu() {
        rotation=rotateFAB()
        isFabOpen=true

        //motrar info
        mostrar(binding.pasadosView)
        mostrar(binding.nuevoView)
        mostrar(binding.activoView)
        mostrar(binding.planesView)

    }

    private fun mostrar(view: View) {
        view.apply {
            visibility= View.VISIBLE
            alpha=0f
            translationY=height.toFloat()
            animate()
                .setDuration(200)
                .translationY(0f)
                .setListener(object : AnimatorListenerAdapter(){})
                .alpha(1f)
                .start()
        }
    }

    private fun rotateFAB():Boolean {
        binding.fabMenuPlan.animate()
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter(){})
            .rotation(if(!isFabOpen) 180f else 0f)

        return isFabOpen
    }

    private fun loadImage(uri : Uri?) {
        // Agregar la Uri recibida a la lista de fotos
        if (uri != null) {
            photoList.add(uri)
        }
        // Notificar al adaptador que se ha agregado una nueva foto
        photoGalleryAdapter.notifyItemInserted(photoList.size - 1)
        // Crear una nueva Uri para la próxima foto
        val file = File(getFilesDir(), "picFromCamera")
        uriCamera = FileProvider.getUriForFile(baseContext, baseContext.packageName + ".fileprovider", file)
    }

}