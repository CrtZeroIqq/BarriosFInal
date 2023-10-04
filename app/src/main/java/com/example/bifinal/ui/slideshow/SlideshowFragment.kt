package com.example.bifinal.ui.slideshow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.bifinal.R
import com.example.bifinal.databinding.FragmentSlideshowBinding
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.maps.android.heatmaps.HeatmapTileProvider
import okhttp3.*
import java.io.IOException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SlideshowFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null
    private var heatmapOverlay: TileOverlay? = null
    private val binding get() = _binding!!

    private val client by lazy {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val slideshowViewModel = ViewModelProvider(this).get(SlideshowViewModel::class.java)

        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textSlideshow
        slideshowViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { map ->
            // Ubicaciones de Iquique y Arica
            val iquique = LatLng(-20.220833, -70.143056)
            val arica = LatLng(-18.478253, -70.312599)

            // Calcular la ubicación central
            val centralLat = (iquique.latitude + arica.latitude) / 2
            val centralLng = (iquique.longitude + arica.longitude) / 2
            val centralLocation = LatLng(centralLat, centralLng)

            // Mover la cámara a la ubicación central
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(centralLocation, 8f)) // 8f es el nivel de zoom, ajusta según tus necesidades

            binding.spinnerDenunciaType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    val tipoDenuncia = parent.getItemAtPosition(position).toString()
                    loadHeatMapData(map, tipoDenuncia)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }

        return root
    }

    private fun loadHeatMapData(map: GoogleMap, tipoDenuncia: String) {
        val request = Request.Builder()
            .url("https://44.216.113.38/barrios_inteligentes/assets/php/heatmap.php?tipo_denuncia=$tipoDenuncia")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    val responseBody = it.string()
                    println("Response: $responseBody") // Imprime la respuesta para depuración

                    try {
                        val type = object : TypeToken<List<Denuncia>>() {}.type
                        val denuncias: List<Denuncia> = Gson().fromJson(responseBody, type)

                        val latLngs = denuncias.mapNotNull {
                            try {
                                LatLng(it.latitud.toDouble(), it.longitud.toDouble())
                            } catch (e: NumberFormatException) {
                                null
                            }
                        }

                        activity?.runOnUiThread {
                            if (latLngs.isNotEmpty()) {
                                addHeatMap(map, latLngs)
                            } else {
                                // Opcional: Mostrar un mensaje al usuario indicando que no hay datos para mostrar
                                Toast.makeText(context, "No hay datos para mostrar", Toast.LENGTH_SHORT).show()
                                heatmapOverlay?.remove()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace() // Imprime el error para depuración
                    }
                }
            }
        })
    }

    private fun addHeatMap(map: GoogleMap, latLngs: List<LatLng>) {
        // Elimina el mapa de calor anterior si existe
        heatmapOverlay?.remove()

        val provider = HeatmapTileProvider.Builder()
            .data(latLngs)
            .build()

        heatmapOverlay = map.addTileOverlay(TileOverlayOptions().tileProvider(provider))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class Denuncia(val latitud: String, val longitud: String, val tipo_denuncia: String)
}
