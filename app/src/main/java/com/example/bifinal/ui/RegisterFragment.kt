package com.example.bifinal.ui

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.bifinal.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class RegisterFragment : DialogFragment() {

    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate?> = arrayOfNulls(0)
        override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
    })

    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var rutEditText: EditText
    private lateinit var birthDateEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var neighborhoodEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var passwordConfirmationEditText: EditText
    private lateinit var registerButton: Button

    private var client: OkHttpClient
    private var isUpdatingRut = false

    init {
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        val builder = OkHttpClient.Builder()
        builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
        builder.hostnameVerifier { _, _ -> true }

        client = builder.build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.RoundedCornersDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        firstNameEditText = view.findViewById(R.id.first_nameEditText)
        lastNameEditText = view.findViewById(R.id.last_nameEditText)
        rutEditText = view.findViewById(R.id.rutEditText)
        birthDateEditText = view.findViewById(R.id.birth_dateEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        phoneNumberEditText = view.findViewById(R.id.phone_numberEditText)
        neighborhoodEditText = view.findViewById(R.id.neighborhoodEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        passwordConfirmationEditText = view.findViewById(R.id.password_confirmationEditText)
        registerButton = view.findViewById(R.id.registerButton)

        rutEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isUpdatingRut) {
                    isUpdatingRut = true

                    val cleanRut = s.toString().replace("-", "").replace(".", "")
                    val formattedRut: String = when {
                        cleanRut.length in 1..7 -> cleanRut
                        cleanRut.length >= 8 -> {
                            val number = cleanRut.substring(0, cleanRut.length - 1)
                            val dv = cleanRut.substring(cleanRut.length - 1)
                            "$number-$dv"
                        }
                        else -> ""
                    }

                    rutEditText.setText(formattedRut)
                    rutEditText.setSelection(formattedRut.length) // Mueve el cursor al final del texto

                    isUpdatingRut = false
                }
            }
        })

        rutEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) { // Si el EditText ha perdido el foco
                val rut = rutEditText.text.toString()
                if (!validateRut(rut)) {
                    Toast.makeText(context, "RUT inválido.", Toast.LENGTH_SHORT).show()
                    registerButton.isEnabled = false // Desactiva el botón de registro
                } else {
                    registerButton.isEnabled = true // Activa el botón de registro si el RUT es válido
                }
            }
        }


        registerButton.setOnClickListener {
            val firstName = firstNameEditText.text.toString()
            val lastName = lastNameEditText.text.toString()
            val rut = rutEditText.text.toString()
            val birthDate = birthDateEditText.text.toString()
            val email = emailEditText.text.toString()
            val phoneNumber = phoneNumberEditText.text.toString()
            val neighborhood = neighborhoodEditText.text.toString()
            val password = passwordEditText.text.toString()
            val passwordConfirmation = passwordConfirmationEditText.text.toString()

            if (password != passwordConfirmation) {
                Toast.makeText(context, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show()
            } else {
                registerUser(firstName, lastName, rut, birthDate, email, phoneNumber, neighborhood, password, passwordConfirmation)
            }
        }

        return view
    }

    private fun registerUser(firstName: String, lastName: String, rut: String, birthDate: String, email: String, phoneNumber: String, neighborhood: String, password: String, passwordConfirmation: String) {
        val json = JSONObject().apply {
            put("first_name", firstName)
            put("last_name", lastName)
            put("rut", rut)
            put("birth_date", birthDate)
            put("email", email)
            put("phone_number", phoneNumber)
            put("neighborhood", neighborhood)
            put("password", password)
            put("password_confirmation", passwordConfirmation)
        }
        Log.d("RegisterFragment", "Datos enviados: $json")

        val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("https://44.216.113.38/barrios_inteligentes/assets/php/register.php")
            .post(requestBody)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Registro exitoso.", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error en el registro: $responseBody", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("RegisterFragment", "Error al registrar usuario", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al registrar usuario", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = (resources.displayMetrics.widthPixels * 0.95).toInt()
            val height = (resources.displayMetrics.heightPixels * 0.90).toInt()
            dialog.window?.setLayout(width, height)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle("Registro de Usuario")
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE) // Esto elimina el título predeterminado del diálogo
        return dialog
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (showsDialog) {
            view?.let {
                it.post {
                    val window = dialog?.window
                    window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
                }
            }
        }
    }

    private fun validateFields(): Boolean {
        if (firstNameEditText.text.toString().isEmpty() ||
            lastNameEditText.text.toString().isEmpty() ||
            rutEditText.text.toString().isEmpty() ||
            birthDateEditText.text.toString().isEmpty() ||
            emailEditText.text.toString().isEmpty() ||
            phoneNumberEditText.text.toString().isEmpty() ||
            neighborhoodEditText.text.toString().isEmpty() ||
            passwordEditText.text.toString().isEmpty() ||
            passwordConfirmationEditText.text.toString().isEmpty()) {

            Toast.makeText(context, "Todos los campos son obligatorios.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    fun validateRut(rut: String): Boolean {
        try {
            val pattern = Regex("^(\\d{1,8}-[\\dkK]{1})$")
            if (!pattern.matches(rut)) return false

            val splitRut = rut.split("-")
            val number = splitRut[0].toInt()
            val dv = splitRut[1].toUpperCase()

            var sum = 0
            var factor = 2

            for (i in number.toString().reversed()) {
                sum += i.toString().toInt() * factor
                factor = if (factor == 7) 2 else factor + 1
            }

            val expectedDv = 11 - (sum % 11)
            val calculatedDv = when (expectedDv) {
                10 -> "K"
                11 -> "0"
                else -> expectedDv.toString()
            }

            return dv == calculatedDv
        } catch (e: Exception) {
            return false
        }
    }


}


