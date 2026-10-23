package com.example.lab10_ejerciciodedesarrollo

import com.example.lab10_ejerciciodedesarrollo.data.*
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class EspaciosTest {
    @Test fun formularioRechazaVaciosAforoCeroYEstadoDesconocido() {
        val e=Formulario(aforo="0",estado="otro").errores()
        assertEquals(setOf("nombre","ubicacion","aforo_maximo","estado_operativo"),e.keys)
    }
    @Test fun rechazaAforoDecimalNegativoYFueraDeRango() {
        listOf("-1","2.5","2147483648").forEach { assertTrue(Formulario(nombre="Sala",ubicacion="A",aforo=it).errores().containsKey("aforo_maximo")) }
    }
    @Test fun limitaNombreYUbicacionA120Caracteres() {
        val f=Formulario(nombre="a".repeat(121),ubicacion="b".repeat(121),aforo="20")
        assertEquals(setOf("nombre","ubicacion"),f.errores().keys)
    }
    @Test fun bodyRecortaEspaciosYUsaCamposDeDjango() {
        val f=Formulario(nombre=" Sala A ",ubicacion=" Bloque B ",aforo="120",estado="mantenimiento")
        assertTrue(f.errores().isEmpty())
        val json=Gson().toJsonTree(f.body()).asJsonObject
        assertEquals("Sala A",json["nombre"].asString)
        assertEquals(120,json["aforo_maximo"].asInt)
        assertEquals("mantenimiento",json["estado_operativo"].asString)
        assertFalse(json.has("id"))
    }
    @Test fun busquedaIgnoraTildesYCombinaEstadoConUbicacion() {
        val items=listOf(Espacio(1,"Salón Norte","Bloque A",80,"disponible"),Espacio(2,"Salón Sur","Bloque B",90,"inactivo"))
        assertEquals(listOf(1),filtrar(items,"SALON","disponible").map {it.id})
        assertEquals(listOf(2),filtrar(items,"bloque b","").map {it.id})
        assertTrue(filtrar(items,"salon","mantenimiento").isEmpty())
    }
    @Test fun retrofitEnviaVerbosRutasYTokenCorrectos() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            val api=Retrofit.Builder().baseUrl(server.url("/api/")).addConverterFactory(GsonConverterFactory.create()).build().create(EventFlowApi::class.java)
            val json="""{"id":7,"nombre":"Sala test","ubicacion":"B","aforo_maximo":22,"estado_operativo":"disponible"}"""
            server.enqueue(MockResponse().setBody("[$json]"));assertEquals(22,api.listar().single().aforo)
            server.takeRequest().let {assertEquals("GET",it.method);assertEquals("/api/espacios/",it.path);assertNull(it.getHeader("Authorization"))}
            server.enqueue(MockResponse().setBody("""{"token":"test-token"}"""));api.login(Credenciales("operador","solo-test"))
            server.takeRequest().let {assertEquals("POST",it.method);assertEquals("/api/token/",it.path)}
            val b=EspacioBody("Sala test","B",22,"disponible")
            server.enqueue(MockResponse().setResponseCode(201).setBody(json));api.crear("Token test-token",b)
            server.takeRequest().let {assertEquals("POST",it.method);assertEquals("Token test-token",it.getHeader("Authorization"));assertTrue(it.body.readUtf8().contains("aforo_maximo"))}
            server.enqueue(MockResponse().setBody(json));api.obtener(7)
            assertEquals("/api/espacios/7/",server.takeRequest().path)
            server.enqueue(MockResponse().setBody(json));api.editar("Token test-token",7,b)
            server.takeRequest().let {assertEquals("PUT",it.method);assertEquals("/api/espacios/7/",it.path)}
            server.enqueue(MockResponse().setResponseCode(204));api.eliminar("Token test-token",7)
            server.takeRequest().let {assertEquals("DELETE",it.method);assertEquals("Token test-token",it.getHeader("Authorization"))}
        } finally {server.shutdown()}
    }
    @Test fun explicaProteccionDeRecursosYErroresDeCampos() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            val api=Retrofit.Builder().baseUrl(server.url("/api/")).addConverterFactory(GsonConverterFactory.create()).build().create(EventFlowApi::class.java)
            server.enqueue(MockResponse().setResponseCode(400).setBody("""{"detail":"No se puede eliminar un espacio con recursos asignados."}"""))
            val e=runCatching {api.eliminar("Token test",1)}.exceptionOrNull()!!
            assertTrue(interpretarError(e).mensaje.contains("recursos asignados"))
            server.enqueue(MockResponse().setResponseCode(400).setBody("""{"aforo_maximo":["Debe ser mayor que cero."]}"""))
            val e2=runCatching {api.crear("Token test",EspacioBody("s","a",0,"disponible"))}.exceptionOrNull()!!
            assertEquals("Debe ser mayor que cero.",interpretarError(e2).campos["aforo_maximo"])
        } finally {server.shutdown()}
    }
}
