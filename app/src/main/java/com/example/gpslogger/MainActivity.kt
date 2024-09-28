package com.example.gpslogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
//import androidx.compose.ui.tooling.preview.Preview
import com.example.gpslogger.ui.theme.GPSLoggerTheme
// Agregadas
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp


class MainActivity : ComponentActivity() {

    // Instancia de FirebaseAuth
    private lateinit var autenticacion: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar FirebaseAuth
        autenticacion = FirebaseAuth.getInstance()

        setContent {
            GPSLoggerTheme {
                PantallaInicioSesion { correo, contrasena ->
                    iniciarSesion(correo, contrasena)
                }
            }
        }
    }

    // Función para autenticar al usuario con Firebase
    private fun iniciarSesion(correo: String, contrasena: String) {
        autenticacion.signInWithEmailAndPassword(correo.trim(), contrasena)
            .addOnCompleteListener(this) { tarea ->
                if (tarea.isSuccessful) {
                    // Para probar hasta este punto, si el login es exitoso
                    Toast.makeText(this, "Inicio de sesión exitoso!", Toast.LENGTH_SHORT).show()
                    Thread.sleep(2000) // 1 segundo

                    // Si la autenticación es exitosa, navegar a MenuActivity
                    startActivity(Intent(this, MenuActivity::class.java))

                    finish()
                } else {
                    // Si falla, mostrar mensaje de error
                    Toast.makeText(this, "Credenciales incorrectas. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}

@Composable
fun PantallaInicioSesion(onIniciarSesionClick: (String, String) -> Unit) {
    // Variables de estado para los campos de entrada
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    // Interfaz de usuario
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            // Campo de Correo
            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo de Contraseña
            OutlinedTextField(
                value = contrasena,
                onValueChange = { contrasena = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de Iniciar Sesión
            Button(
                onClick = { onIniciarSesionClick(correo, contrasena) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Iniciar Sesión")
            }
        }
    }
}
