package com.example.lab10_ejerciciodedesarrollo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab10_ejerciciodedesarrollo.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Pantalla { ESPACIOS, CUENTA, EDITOR }
data class EventFlowState(
    val pantalla: Pantalla = Pantalla.ESPACIOS,
    val espacios: List<Espacio> = emptyList(),
    val busy: Boolean = false,
    val cargado: Boolean = false,
    val usuario: String? = null,
    val error: String? = null,
    val aviso: String? = null,
    val formulario: Formulario = Formulario(),
    val errores: Map<String,String> = emptyMap(),
    val intento: Int = 0,
    val borrar: Espacio? = null,
)
class EventFlowViewModel(private val api: EventFlowApi = ApiProvider.api): ViewModel() {
    private val mutable = MutableStateFlow(EventFlowState())
    val state = mutable.asStateFlow()
    // La sesión vive en memoria: no se guarda la contraseña ni el token en archivos.
    private var token: String? = null
    init { refrescar() }
    private fun operacion(block: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy=true,error=null,aviso=null) }
        viewModelScope.launch {
            try { block() }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) {
                val fallo = interpretarError(e)
                if(fallo.sesionInvalida) token = null
                mutable.update { it.copy(error=fallo.mensaje,errores=fallo.campos,
                    usuario=if(fallo.sesionInvalida) null else it.usuario) }
            } finally { mutable.update { it.copy(busy=false) } }
        }
    }
    fun refrescar() = operacion {
        val items = api.listar()
        mutable.update { it.copy(espacios=items,cargado=true) }
    }
    fun navegar(p: Pantalla) {
        if (!mutable.value.busy) mutable.update { it.copy(pantalla=p,error=null,aviso=null,borrar=null) }
    }
    fun entrar(usuario: String, password: String) {
        if (usuario.isBlank() || password.isEmpty()) {
            mutable.update { it.copy(error="Escribe tu usuario y contraseña.") }; return
        }
        operacion {
            val sesion = api.login(Credenciales(usuario.trim(),password))
            require(sesion.token.isNotBlank())
            token = sesion.token
            mutable.update { it.copy(usuario=usuario.trim(),pantalla=Pantalla.ESPACIOS,aviso="Sesión iniciada. Ya puedes gestionar tus espacios.") }
        }
    }
    fun salir() {
        if(mutable.value.busy) return
        token = null
        mutable.update { it.copy(usuario=null,pantalla=Pantalla.ESPACIOS,borrar=null,error=null,aviso="Sesión cerrada. Puedes seguir consultando los espacios.") }
    }
    private fun autorizado(): Boolean {
        if(token != null) return true
        mutable.update { it.copy(pantalla=Pantalla.CUENTA,error=null,aviso="Inicia sesión para registrar, editar o eliminar espacios.") }
        return false
    }
    fun nuevo() {
        if(mutable.value.busy || !autorizado()) return
        mutable.update { it.copy(pantalla=Pantalla.EDITOR,formulario=Formulario(),errores=emptyMap(),intento=0,error=null,aviso=null) }
    }
    fun editar(e: Espacio) {
        if(mutable.value.busy || !autorizado()) return
        operacion {
            val detalle = api.obtener(e.id)
            mutable.update { it.copy(pantalla=Pantalla.EDITOR,formulario=Formulario.de(detalle),errores=emptyMap(),intento=0) }
        }
    }
    fun cambiar(f: Formulario, campo: String) {
        if(!mutable.value.busy) mutable.update { it.copy(formulario=f,errores=it.errores-campo,error=null) }
    }
    fun guardar() {
        if(mutable.value.busy || !autorizado()) return
        val f = mutable.value.formulario
        val errores = f.errores()
        if(errores.isNotEmpty()) {
            mutable.update { it.copy(errores=errores,error="Revisa los campos indicados antes de guardar.",intento=it.intento+1) };return
        }
        operacion {
            val auth = "Token ${token!!}"
            if(f.id == 0) api.crear(auth,f.body()) else api.editar(auth,f.id,f.body())
            mutable.update { it.copy(pantalla=Pantalla.ESPACIOS,errores=emptyMap(),error=null,aviso=if(f.id == 0) "Espacio registrado correctamente." else "Espacio actualizado correctamente.") }
            actualizarDespuesDeEscribir()
        }
    }
    fun pedirBorrar(e: Espacio) {
        if(!mutable.value.busy && autorizado()) mutable.update { it.copy(borrar=e,error=null,aviso=null) }
    }
    fun cancelarBorrado() { if(!mutable.value.busy) mutable.update { it.copy(borrar=null,error=null) } }
    fun eliminar() {
        if(mutable.value.busy || !autorizado()) return
        val e = mutable.value.borrar ?: return
        operacion {
            api.eliminar("Token ${token!!}",e.id)
            mutable.update { it.copy(borrar=null,espacios=it.espacios.filterNot { x -> x.id == e.id },aviso="Espacio eliminado correctamente.") }
            actualizarDespuesDeEscribir()
        }
    }
    private suspend fun actualizarDespuesDeEscribir() {
        try {
            val items = api.listar()
            mutable.update { it.copy(espacios=items,cargado=true) }
        } catch(e: CancellationException) { throw e }
        catch(e: Exception) {
            mutable.update { it.copy(error="El cambio se guardó, pero no se pudo actualizar la lista. Pulsa Actualizar.") }
        }
    }
}
