package com.example.gpslogger

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.gpslogger.ui.theme.GPSLoggerTheme
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MenuActivity : ComponentActivity() {

    // Cliente de ubicación
    private lateinit var clienteUbicacion: FusedLocationProviderClient

    // Referencia a la base de datos de Firebase
    private val baseDatos = FirebaseDatabase.getInstance().reference

    // Usuario actual, sino desconocido
    private val idUsuario = FirebaseAuth.getInstance().currentUser?.uid ?: "usuario_desconocido"

    // Lanzador de permisos
    private val lanzadorPermisoUbicacion = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { otorgado ->
        if (otorgado) {
            obtenerCoordenadas()
        } else {
           Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar FusedLocationProviderClient
        clienteUbicacion = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            GPSLoggerTheme {
                PantallaMenu(
                    onObtenerCoordenadasClick = {
                        verificarPermisoUbicacion()
                    },
                    onCerrarSesionClick = {
                        cerrarSesion()
                    }
                )
            }
        }
    }

    // Función para verificar y solicitar permisos
    private fun verificarPermisoUbicacion() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso otorgado, obtener coordenadas
                obtenerCoordenadas()
            }
            else -> {
                // Solicitar permiso
                lanzadorPermisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // Función para obtener coordenadas
    private fun obtenerCoordenadas() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        clienteUbicacion.lastLocation.addOnSuccessListener { ubicacion ->
            if (ubicacion != null) {
                val latitud = ubicacion.latitude
                val longitud = ubicacion.longitude
                val timestamp = System.currentTimeMillis()

                // Guardar en Firebase
                guardarCoordenadasEnFirebase(latitud, longitud, timestamp)
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función para guardar coordenadas en Firebase
    private fun guardarCoordenadasEnFirebase(latitud: Double, longitud: Double, timestamp: Long) {
        val datos = mapOf(
            "latitud" to latitud,
            "longitud" to longitud,
            "timestamp" to timestamp
        )

        baseDatos.child("usuarios").child(idUsuario).push().setValue(datos)
            .addOnCompleteListener { tarea ->
                if (tarea.isSuccessful) {
                    Toast.makeText(this, "Coordenadas guardadas.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al guardar en Firebase.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Función para manejar el cierre de sesión
    private fun cerrarSesion() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

@Composable
fun PantallaMenu(
    onObtenerCoordenadasClick: () -> Unit,
    onCerrarSesionClick: () -> Unit
) {
    // Estado para almacenar la lista de coordenadas
    var listaCoordenadas by remember { mutableStateOf<List<DatosCoordenadas>>(emptyList()) }

    // Cargar datos desde Firebase al iniciar y agregar un listener para ver si hay cambios
    LaunchedEffect(Unit) {
        val idUsuario = FirebaseAuth.getInstance().currentUser?.uid ?: "usuario_desconocido"
        val baseDatos = FirebaseDatabase.getInstance().reference
        baseDatos.child("usuarios").child(idUsuario).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = mutableListOf<DatosCoordenadas>()
                for (child in snapshot.children) {
                    val latitud = child.child("latitud").getValue(Double::class.java)
                    val longitud = child.child("longitud").getValue(Double::class.java)
                    val timestamp = child.child("timestamp").getValue(Long::class.java)
                    if (latitud != null && longitud != null && timestamp != null) {
                        lista.add(DatosCoordenadas(latitud, longitud, timestamp))
                    }
                }
                listaCoordenadas = lista
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar el error
                Log.e("FirebaseError", "Error al leer datos: ${error.message}", error.toException())

            }
        })
    }

    // Interfaz de usuario
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Botón para obtener coordenadas
            Button(
                onClick = onObtenerCoordenadasClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Obtener coordenadas GPS")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para cerrar sesión
            Button(
                onClick = onCerrarSesionClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Cerrar Sesión")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mostrar las coordenadas en una tabla
            TablaCoordenadas(listaCoordenadas)
        }
    }
}

@Composable
fun TablaCoordenadas(listaCoordenadas: List<DatosCoordenadas>) {
    Column {
        // Encabezados
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Latitud",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "Longitud",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "Fecha y Hora",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filas de datos
        for (coordenada in listaCoordenadas) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = coordenada.latitud.toString(),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = coordenada.longitud.toString(),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = coordenada.obtenerFechaFormateada(),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// Data class para almacenar coordenadas
data class DatosCoordenadas(val latitud: Double, val longitud: Double, val timestamp: Long) {
    @SuppressLint("SimpleDateFormat")
    fun obtenerFechaFormateada(): String {
        val fecha = java.util.Date(timestamp)
        val formato = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        return formato.format(fecha)
    }
}
