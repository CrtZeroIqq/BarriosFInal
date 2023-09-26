package com.example.bifinal.ui.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.bifinal.databinding.FragmentGalleryBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.*
import java.io.IOException
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class GalleryFragment : Fragment() {


    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    private val user by lazy { arguments?.getString("nav_header_subtitle") ?: "" }
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val client by lazy { getUnsafeOkHttpClient() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View {
        val galleryViewModel = ViewModelProvider(this).get(GalleryViewModel::class.java)

        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textGallery
        galleryViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Set up the ImageButtons' click listeners
        binding.asalto.setOnClickListener { sendReport("Asalto") }
        binding.violencia.setOnClickListener { sendReport("Violencia Intrafamiliar") }
        binding.colision.setOnClickListener { sendReport("Colision Vehicular") }
        binding.homicidio.setOnClickListener { sendReport("Homicidio") }
        binding.corteluz.setOnClickListener { sendReport("Corte de Luz") }
        binding.corteagua.setOnClickListener { sendReport("Corte de Agua") }
        binding.basura.setOnClickListener { sendReport("Basura en las Calles") }
        binding.calles.setOnClickListener { sendReport("Calles en Mal Estado") }
        binding.luminarias.setOnClickListener { sendReport("Luminarias Apagadas") }
        Log.d("GalleryFragment", "Email recuperado: $user")

        return root
    }

    private fun sendReport(type: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val lat = it.latitude.toString()
                    val long = it.longitude.toString()

                    // Construct the URL for the API call
                    val url = "https://44.216.113.38/barrios_inteligentes/assets/php/denuncia.php?user=$user&timestamp=$timestamp&type=$type&lat=$lat&long=$long"

                    // Make the API call using the constructed URL
                    val request = Request.Builder()
                        .url(url)
                        .build()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            // Handle the error
                            e.printStackTrace()
                        }

                        override fun onResponse(call: Call, response: Response) {
                            if (response.isSuccessful) {
                                val responseBody = response.body?.string()
                                // TODO: Handle the successful response, if needed
                                println(responseBody)
                            } else {
                                // Handle the unsuccessful response
                                println("Error: ${response.code}")
                            }
                        }
                    })
                }
            }
        } else {
            // Request location permission
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, you can fetch the location now
                    sendReport("YourReportType")  // Replace "YourReportType" with the actual report type
                } else {
                    // Permission denied
                    Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            val builder = OkHttpClient.Builder()
            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }

            return builder.build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1234
    }
}
