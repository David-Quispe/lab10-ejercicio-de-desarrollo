package com.example.lab10_ejerciciodedesarrollo.data

import com.google.gson.annotations.SerializedName
import com.google.gson.JsonParser
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.IOException
import java.text.Normalizer
import java.util.Locale

val estados = linkedMapOf("disponible" to "Disponible", "mantenimiento" to "Mantenimiento", "inactivo" to "Inactivo")
data class Espacio(
    val id: Int = 0,
    val nombre: String,
    val ubicacion: String,
    @SerializedName("aforo_maximo") val aforo: Int,
    @SerializedName("estado_operativo") val estado: String,
)
data class EspacioBody(val nombre: String, val ubicacion: String,
    @SerializedName("aforo_maximo") val aforo: Int,
    @SerializedName("estado_operativo") val estado: String)
data class Credenciales(val username: String, val password: String)
data class Sesion(val token: String)
interface EventFlowApi {
    @GET("espacios/") suspend fun listar(): List<Espacio>
    @GET("espacios/{id}/") suspend fun obtener(@Path("id") id: Int): Espacio
    @POST("token/") suspend fun login(@Body credenciales: Credenciales): Sesion
    @POST("espacios/") suspend fun crear(@Header("Authorization") auth: String, @Body espacio: EspacioBody): Espacio
    @PUT("espacios/{id}/") suspend fun editar(@Header("Authorization") auth: String, @Path("id") id: Int, @Body espacio: EspacioBody): Espacio
    @DELETE("espacios/{id}/") suspend fun eliminar(@Header("Authorization") auth: String, @Path("id") id: Int)
}
object ApiProvider {
    const val BASE_URL = "http://172.22.159.90:8000/api/"
    val api: EventFlowApi by lazy {
        Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build().create(EventFlowApi::class.java)
    }
}
data class Formulario(val id: Int = 0, val nombre: String = "", val ubicacion: String = "", val aforo: String = "", val estado: String = "disponible") {
    fun errores(): Map<String,String> = buildMap {
        if (nombre.isBlank()) put("nombre", "Escribe el nombre del espacio.")
        else if (nombre.trim().length > 120) put("nombre", "Usa un máximo de 120 caracteres.")
        if (ubicacion.isBlank()) put("ubicacion", "Indica el bloque, piso o ubicación.")
        else if (ubicacion.trim().length > 120) put("ubicacion", "Usa un máximo de 120 caracteres.")
        if (aforo.toIntOrNull()?.let { it > 0 } != true) put("aforo_maximo", "Ingresa un número entero mayor que cero.")
        if (estado !in estados) put("estado_operativo", "Selecciona un estado válido.")
    }
    fun body() = EspacioBody(nombre.trim(), ubicacion.trim(), aforo.toInt(), estado)
    companion object { fun de(e: Espacio) = Formulario(e.id,e.nombre,e.ubicacion,e.aforo.toString(),e.estado) }
}
private fun normalizar(s: String) = Normalizer.normalize(s,Normalizer.Form.NFD).replace("\\p{M}+".toRegex(), "").lowercase(Locale.ROOT)
fun filtrar(espacios: List<Espacio>, busqueda: String, estado: String): List<Espacio> {
    val q = normalizar(busqueda.trim())
    return espacios.filter { (estado.isBlank() || it.estado == estado) && normalizar(it.nombre + " " + it.ubicacion).contains(q) }
}
data class Fallo(val mensaje: String, val campos: Map<String,String> = emptyMap(), val sesionInvalida: Boolean = false)
fun interpretarError(error: Throwable): Fallo {
    if (error is HttpException) {
        val fields = runCatching {
            val json = JsonParser.parseString(error.response()?.errorBody()?.string().orEmpty()).asJsonObject
            json.entrySet().associate { (key,value) -> key to if (value.isJsonArray) value.asJsonArray.joinToString(" ") { it.asString } else if (value.isJsonPrimitive) value.asString else "Revisa este campo." }
        }.getOrDefault(emptyMap())
        val mensaje = when(error.code()) {
            400 -> fields["detail"] ?: fields["non_field_errors"] ?: "Revisa los campos indicados."
            401,403 -> "No se pudo autorizar la operación. Inicia sesión con una cuenta válida."
            404 -> "Este espacio ya no está disponible. Actualiza la lista."
            else -> "El servidor no pudo completar la operación. Inténtalo de nuevo."
        }
        return Fallo(mensaje,fields.filterKeys { it in setOf("nombre","ubicacion","aforo_maximo","estado_operativo") },error.code() == 401)
    }
    return Fallo(if(error is IOException) "No hay conexión con EventFlow. Comprueba que Django y la red estén disponibles." else "No se pudo completar la operación. Inténtalo de nuevo.")
}
