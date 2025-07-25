package com.martahp.batucadafina.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.util.Log
import com.martahp.batucadafina.databinding.ActivityMainBinding
import com.martahp.batucadafina.utils.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //comprobamos la sesión ANTES de inflar el layout
        val sessionManager = SessionManager(applicationContext)
        if (sessionManager.isLoggedIn()) {
            val username = sessionManager.getLoggedInUsername()
            if (username != null) {
                Log.i(
                    "MainActivity",
                    "Sesión activa encontrada para: $username. Navegando a UserHome."
                )
                val intent = Intent(this, UserHomeActivity::class.java)
                intent.putExtra(UserHomeActivity.EXTRA_USERNAME, username)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() // Cerrar MainActivity para que no quede en la pila
                return // Salir de onCreate para no mostrar la UI de MainActivity
            } else {
                sessionManager.clearLoginSession()
                Log.w(
                    "MainActivity",
                    "Sesión marcada como activa pero sin username. Limpiando sesión."
                )
            }
        }

        //inflar el layout SIEMPRE antes que nada, si no da error
        //en este caso como arrastramos la info de recordar sesion, no la inflamos hasta que se ha hecho lo anterior
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //configureToolbar()
        configureListeners()
    }

    private fun configureListeners() {
        binding.buttonSignIn.setOnClickListener {
            val intent = Intent(this, LoginUserActivity::class.java)
            startActivity(intent)
        }
        binding.textViewSignUp.setOnClickListener {
            val intent = Intent(this, CreateAccountActivity::class.java)
            startActivity(intent)
        }
        binding.textViewBasicTunerAccess.setOnClickListener {
            val intent = Intent(this, BasicTunerActivity::class.java)
            startActivity(intent)
        }
    }

//    private fun configureToolbar() {
//        setSupportActionBar(binding.toolbarMain)
////        supportActionBar?.setDisplayHomeAsUpEnabled(true)
////        supportActionBar?.setDisplayShowHomeEnabled(true)
////        binding.toolbarMain.setNavigationOnClickListener {
////            onBackPressedDispatcher.onBackPressed()
////        } eliminamos la flecha de atrás
//    }

}