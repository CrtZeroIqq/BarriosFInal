package com.example.bifinal.ui.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.bifinal.databinding.FragmentGalleryBinding
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    private val client = OkHttpClient()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val galleryViewModel =
            ViewModelProvider(this).get(GalleryViewModel::class.java)

        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root

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
        // ... (repeat for other ImageButtons)

        galleryViewModel.text.observe(viewLifecycleOwner) {
            binding.textGallery.text = it
        }
        return root
    }

    private fun sendReport(type: String) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permission
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1234
            )
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val user = "nombreUsuario" // Replace with the logged-in user's name
                val timestamp = System.currentTimeMillis().toString()
                val lat = location.latitude.toString()
                val long = location.longitude.toString()

                val url = "https://tu-url.com/api.php?user=$user&timestamp=$timestamp&type=$type&lat=$lat&long=$long"

                CoroutineScope(Dispatchers.IO).launch {
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        // Handle successful response
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Denuncia registrada con éxito.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Handle error
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Error al registrar denuncia.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
