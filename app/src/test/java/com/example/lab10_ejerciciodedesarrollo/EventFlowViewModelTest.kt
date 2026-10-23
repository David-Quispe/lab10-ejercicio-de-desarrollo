package com.example.lab10_ejerciciodedesarrollo

import com.example.lab10_ejerciciodedesarrollo.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class EventFlowViewModelTest {
    private val dispatcher=StandardTestDispatcher()
    @Before fun setup() {Dispatchers.setMain(dispatcher)}
    @After fun clean() {Dispatchers.resetMain()}
    private class Fake: EventFlowApi {
        var failList=false;var creates=0
        override suspend fun listar():List<Espacio> {if(failList) throw IOException();return listOf(Espacio(1,"Sala A","Bloque A",50,"disponible"))}
        override suspend fun obtener(id:Int)=Espacio(id,"Sala A","Bloque A",50,"disponible")
        override suspend fun login(credenciales:Credenciales)=Sesion("test")
        override suspend fun crear(auth:String,espacio:EspacioBody):Espacio {creates++;delay(100);return Espacio(2,espacio.nombre,espacio.ubicacion,espacio.aforo,espacio.estado)}
        override suspend fun editar(auth:String,id:Int,espacio:EspacioBody)=Espacio(id,espacio.nombre,espacio.ubicacion,espacio.aforo,espacio.estado)
        override suspend fun eliminar(auth:String,id:Int) {}
    }
    @Test fun lecturaPublicaYEscrituraRequiereSesion()=runTest(dispatcher) {
        val vm=EventFlowViewModel(Fake());advanceUntilIdle()
        assertEquals(1,vm.state.value.espacios.size)
        vm.nuevo();assertEquals(Pantalla.CUENTA,vm.state.value.pantalla)
    }
    @Test fun guardarInvalidoConservaDatosYErroresDeOtrosCampos()=runTest(dispatcher) {
        val vm=EventFlowViewModel(Fake());advanceUntilIdle();vm.entrar("test","test");advanceUntilIdle();vm.nuevo()
        vm.guardar();assertEquals(3,vm.state.value.errores.size)
        vm.cambiar(vm.state.value.formulario.copy(nombre="Sala nueva"),"nombre")
        assertEquals(setOf("ubicacion","aforo_maximo"),vm.state.value.errores.keys)
        assertEquals("Sala nueva",vm.state.value.formulario.nombre)
    }
    @Test fun doblePulsacionNoDuplicaYFalloDeRecargaNoReabreFormulario()=runTest(dispatcher) {
        val api=Fake();val vm=EventFlowViewModel(api);advanceUntilIdle();vm.entrar("test","test");advanceUntilIdle();vm.nuevo()
        vm.cambiar(Formulario(nombre="Nueva",ubicacion="A",aforo="10"),"nombre")
        api.failList=true;vm.guardar();vm.guardar();advanceUntilIdle()
        assertEquals(1,api.creates);assertEquals(Pantalla.ESPACIOS,vm.state.value.pantalla)
        assertTrue(vm.state.value.aviso!!.contains("registrado"));assertTrue(vm.state.value.error!!.contains("se guardó"))
    }
    @Test fun cerrarSesionRevocaAccionesDeEdicionEnElCliente()=runTest(dispatcher) {
        val vm=EventFlowViewModel(Fake());advanceUntilIdle();vm.entrar("test","test");advanceUntilIdle();vm.salir()
        assertNull(vm.state.value.usuario);vm.nuevo();assertEquals(Pantalla.CUENTA,vm.state.value.pantalla)
    }
}
