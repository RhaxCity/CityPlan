package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.primeraentrega.databinding.ActivityCrearGrupoBinding
import com.example.primeraentrega.databinding.ActivityVerGruposBinding

class CrearGrupoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrearGrupoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearGrupoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inicializarBotones()
    }

    private fun inicializarBotones() {
        binding.buttonAgregarMiembros.setOnClickListener {
            startActivity(Intent(baseContext, AgregarContactosActivity::class.java))
        }

        binding.buttonGuardar.setOnClickListener {
            startActivity(Intent(baseContext, ChatActivity::class.java))
        }

        binding.botonHome.setOnClickListener {
            startActivity(Intent(baseContext, VerGruposActivity::class.java))
        }

        binding.botonConfiguracion.setOnClickListener {
            startActivity(Intent(baseContext, ConfiguracionActivity::class.java))
        }


    }
}