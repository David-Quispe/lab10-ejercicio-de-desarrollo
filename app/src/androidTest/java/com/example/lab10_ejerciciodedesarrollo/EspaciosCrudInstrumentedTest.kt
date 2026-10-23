package com.example.lab10_ejerciciodedesarrollo

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lab10_ejerciciodedesarrollo.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class EspaciosCrudInstrumentedTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun esperar(texto:String) {
        compose.waitUntil(20000) {compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()}
        compose.waitForIdle()
    }
    private fun captura(nombre:String) {
        compose.waitForIdle();Thread.sleep(1200)
        val i=InstrumentationRegistry.getInstrumentation()
        val file=File(i.targetContext.getExternalFilesDir(null),"evidencias/$nombre.png")
        file.parentFile!!.mkdirs()
        i.uiAutomation.takeScreenshot().useBitmap { bitmap -> file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) } }
    }
    private fun Bitmap.useBitmap(block:(Bitmap)->Unit) {try {block(this)} finally {recycle()}}
    private fun campo(label:String)=compose.onNode(hasSetTextAction() and hasText(label))
    @Test fun cicloCompletoConEspacioTemporal() {
        val args=InstrumentationRegistry.getArguments()
        val user=requireNotNull(args.getString("eventflowUser")) {"Indica eventflowUser al ejecutar la prueba."}
        val pass=requireNotNull(args.getString("eventflowPassword")) {"Indica eventflowPassword al ejecutar la prueba."}
        val api=ApiProvider.api
        val nombre="Sala Demo Móvil ${System.currentTimeMillis()%10000}"
        var id:Int?=null
        var auth:String?=null
        try {
            esperar("Auditorio Principal");captura("01-consulta-publica")
            compose.onNodeWithText("Acceder").performClick();esperar("Iniciar sesión");captura("02-acceso-operador")
            campo("Usuario").performTextInput(user)
            campo("Contraseña").performTextInput(pass)
            compose.onNodeWithText("Iniciar sesión").performClick()
            esperar("Sesión iniciada. Ya puedes gestionar tus espacios.");captura("03-sesion-iniciada")
            auth=runBlocking {"Token ${api.login(Credenciales(user,pass)).token}"}
            compose.onNodeWithText("Nuevo").performScrollTo().performClick();esperar("Nombre del espacio");captura("04-formulario-nuevo")
            compose.onNodeWithText("Guardar").performClick();esperar("Escribe el nombre del espacio.");captura("05-validacion")
            campo("Nombre del espacio").performScrollTo().performTextInput(nombre)
            campo("Ubicación o bloque").performScrollTo().performTextInput("Bloque móvil, primer piso")
            campo("Aforo máximo").performScrollTo().performTextInput("64")
            campo("Aforo máximo").performImeAction()
            compose.onNodeWithText("Guardar").performClick();esperar("Espacio registrado correctamente.")
            id=runBlocking {api.listar().single {it.nombre==nombre}.id}
            campo("Buscar nombre o ubicación").performScrollTo().performTextInput(nombre)
            campo("Buscar nombre o ubicación").performImeAction()
            compose.onNodeWithTag("espacio-$id").performScrollTo();captura("06-espacio-creado")
            compose.onNodeWithText("Editar").performClick();esperar("Nombre del espacio")
            campo("Aforo máximo").performScrollTo().performTextClearance()
            campo("Aforo máximo").performTextInput("80");campo("Aforo máximo").performImeAction()
            compose.onNodeWithText("Mantenimiento").performScrollTo().performClick();captura("07-editar-estado-aforo")
            compose.onNodeWithText("Guardar").performClick();esperar("Espacio actualizado correctamente.")
            val actualizado=runBlocking {api.obtener(id)}
            assertEquals(80,actualizado.aforo);assertEquals("mantenimiento",actualizado.estado)
            campo("Buscar nombre o ubicación").performScrollTo().performTextInput(nombre)
            campo("Buscar nombre o ubicación").performImeAction()
            compose.onNodeWithTag("espacio-$id").performScrollTo();captura("08-espacio-actualizado")
            compose.onNodeWithContentDescription("Eliminar $nombre").performClick();esperar("¿Eliminar este espacio?");captura("09-confirmar-eliminacion")
            compose.onNodeWithText("Eliminar",substring=false).performClick();esperar("Espacio eliminado correctamente.")
            assertFalse(runBlocking {api.listar().any {it.id==id}});captura("10-espacio-eliminado")
            id=null
            compose.onNodeWithText("Mi cuenta").performClick();esperar("Cerrar sesión")
            compose.onNodeWithText("Cerrar sesión").performClick();esperar("Sesión cerrada. Puedes seguir consultando los espacios.")
        } finally {
            // Limpieza limitada al registro creado por esta prueba, incluso si falla una aserción.
            if(id!=null && auth!=null) runBlocking {runCatching {api.eliminar(auth,id)}}
        }
    }
}
