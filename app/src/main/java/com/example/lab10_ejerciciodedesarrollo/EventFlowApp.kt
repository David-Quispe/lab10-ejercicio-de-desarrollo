package com.example.lab10_ejerciciodedesarrollo

import android.content.Context
import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lab10_ejerciciodedesarrollo.data.*
import com.example.lab10_ejerciciodedesarrollo.ui.theme.EventFlowTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFlowApp(vm: EventFlowViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("appearance", Context.MODE_PRIVATE) }
    var dark by rememberSaveable { mutableStateOf(prefs.getBoolean("dark",false)) }
    BackHandler(s.pantalla != Pantalla.ESPACIOS) { if(!s.busy) vm.navegar(Pantalla.ESPACIOS) }
    SideEffect {
        (context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window,window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    EventFlowTheme(dark) {
        Scaffold(
            topBar = { TopAppBar(
                title = { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    if(s.pantalla != Pantalla.EDITOR) Icon(Icons.Default.LocationOn,null,tint=MaterialTheme.colorScheme.primary)
                    Text("EventFlow",fontWeight=FontWeight.Bold,letterSpacing=(-0.8).sp)
                } },
                navigationIcon = { if(s.pantalla == Pantalla.EDITOR) IconButton(onClick={vm.navegar(Pantalla.ESPACIOS)},enabled=!s.busy) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Volver a espacios") } },
                actions = { IconButton(onClick={dark=!dark;prefs.edit().putBoolean("dark",dark).apply()}) { Icon(painterResource(if(dark) R.drawable.ic_theme_sun else R.drawable.ic_theme_moon),if(dark) "Activar tema claro" else "Activar tema oscuro") } },
                colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background),
            ) },
            bottomBar = { if(s.pantalla != Pantalla.EDITOR) {
                Column {
                    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                    NavigationBar(containerColor=MaterialTheme.colorScheme.background,tonalElevation=0.dp) {
                        NavigationBarItem(selected=s.pantalla==Pantalla.ESPACIOS,onClick={vm.navegar(Pantalla.ESPACIOS)},enabled=!s.busy,
                            icon={Icon(Icons.Default.LocationOn,null)},label={Text("Espacios")})
                        NavigationBarItem(selected=s.pantalla==Pantalla.CUENTA,onClick={vm.navegar(Pantalla.CUENTA)},enabled=!s.busy,
                            icon={Icon(Icons.Default.AccountCircle,null)},label={Text(if(s.usuario == null) "Acceder" else "Mi cuenta")})
                    }
                }
            } },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding),contentAlignment=Alignment.TopCenter) {
                Box(Modifier.widthIn(max=760.dp).fillMaxSize()) {
                    when(s.pantalla) {
                        Pantalla.ESPACIOS -> EspaciosScreen(s,vm::refrescar,vm::nuevo,vm::editar,vm::pedirBorrar)
                        Pantalla.CUENTA -> CuentaScreen(s,vm::entrar,vm::salir)
                        Pantalla.EDITOR -> EditorScreen(s,vm::cambiar,vm::guardar) {vm.navegar(Pantalla.ESPACIOS)}
                    }
                }
            }
        }
        s.borrar?.let { espacio ->
            AlertDialog(
                onDismissRequest=vm::cancelarBorrado,
                icon={Icon(Icons.Default.Delete,null,tint=MaterialTheme.colorScheme.error)},
                title={Text("¿Eliminar este espacio?")},
                text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(espacio.nombre,fontWeight=FontWeight.Bold)
                    Text("Se eliminará su registro. Esta acción no se puede deshacer.")
                    s.error?.let { Mensaje(it,true) }
                    if(s.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }},
                confirmButton={Button(onClick=vm::eliminar,enabled=!s.busy,colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error,contentColor=MaterialTheme.colorScheme.onError)) {Text("Eliminar")}},
                dismissButton={TextButton(onClick=vm::cancelarBorrado,enabled=!s.busy) {Text("Conservar")}},
            )
        }
    }
}

@Composable
private fun Mensaje(text: String, error: Boolean=false) {
    val c = MaterialTheme.colorScheme
    Surface(color=if(error) c.errorContainer else c.secondaryContainer,contentColor=if(error) c.onErrorContainer else c.onSecondaryContainer,shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Icon(if(error) Icons.Default.Info else Icons.Default.Check,null,Modifier.size(20.dp))
            Text(text,style=MaterialTheme.typography.bodyMedium)
        }
    }
}
@Composable
private fun Estado(estado: String) {
    val c = MaterialTheme.colorScheme
    val color = when(estado) {"disponible" -> c.secondaryContainer;"mantenimiento" -> c.primaryContainer;else -> c.surfaceContainerHigh}
    val ink = when(estado) {"disponible" -> c.onSecondaryContainer;"mantenimiento" -> c.onPrimaryContainer;else -> c.onSurface}
    Surface(color=color,contentColor=ink,shape=RoundedCornerShape(8.dp)) {
        Text(estados[estado] ?: estado,Modifier.padding(horizontal=10.dp,vertical=6.dp),style=MaterialTheme.typography.labelMedium)
    }
}
@Composable
private fun EspaciosScreen(s: EventFlowState, refresh:()->Unit,nuevo:()->Unit,editar:(Espacio)->Unit,borrar:(Espacio)->Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val lista = remember(s.espacios,query,filter) { filtrar(s.espacios,query,filter) }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(start=24.dp,end=24.dp,top=12.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {
            Text("GESTIÓN DE ESPACIOS",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary,letterSpacing=1.8.sp)
            Spacer(Modifier.height(8.dp))
            Text("Todo listo para\nel próximo evento.",style=MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(10.dp))
            Text("Ubicación, aforo y estado. Todo en un lugar.",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Surface(color=MaterialTheme.colorScheme.secondaryContainer,contentColor=MaterialTheme.colorScheme.onSecondaryContainer,shape=RoundedCornerShape(20.dp)) {
                Row(Modifier.fillMaxWidth().padding(20.dp),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if(s.cargado) "${s.espacios.count {it.estado == "disponible"}} disponibles" else "Consultando espacios",style=MaterialTheme.typography.titleLarge)
                        Text(if(s.cargado) "De ${s.espacios.size} espacios registrados" else "Conectando con EventFlow",style=MaterialTheme.typography.bodyMedium)
                    }
                    Plano(Modifier.size(60.dp))
                }
            }
        }
        s.aviso?.let { item {Mensaje(it)} }
        if(s.borrar == null) s.error?.let { item {Mensaje(it,true)} }
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("Tus espacios",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
                IconButton(onClick=refresh,enabled=!s.busy) {Icon(Icons.Default.Refresh,"Actualizar espacios")}
                FilledTonalButton(onClick=nuevo,enabled=!s.busy,contentPadding=PaddingValues(horizontal=14.dp),modifier=Modifier.heightIn(min=48.dp)) {
                    Icon(Icons.Default.Add,null,Modifier.size(20.dp));Spacer(Modifier.width(6.dp));Text("Nuevo")
                }
            }
        }
        item {
            OutlinedTextField(value=query,onValueChange={query=it},label={Text("Buscar nombre o ubicación")},singleLine=true,
                leadingIcon={Icon(Icons.Default.Search,null)},trailingIcon={if(query.isNotEmpty()) IconButton(onClick={query=""}) {Icon(Icons.Default.Close,"Limpiar búsqueda")}},
                modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),
                keyboardActions=KeyboardActions(onSearch={focus.clearFocus();keyboard?.hide()}))
        }
        item {
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                items(listOf("" to "Todos")+estados.toList()) { (key,label) ->
                    FilterChip(selected=filter==key,onClick={filter=key;focus.clearFocus();keyboard?.hide()},label={Text(label)},modifier=Modifier.heightIn(min=48.dp),shape=RoundedCornerShape(12.dp))
                }
            }
        }
        if(s.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if(lista.isEmpty() && !s.busy) item {
            Column(Modifier.fillMaxWidth().padding(vertical=24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text(if(s.cargado && s.espacios.isEmpty()) "Tu primer espacio empieza aquí" else if(!s.cargado) "No pudimos cargar tus espacios" else "Sin coincidencias",style=MaterialTheme.typography.titleLarge)
                Text(if(!s.cargado) "Comprueba la conexión y pulsa Actualizar." else if(s.espacios.isEmpty()) "Registra un espacio para empezar a organizarlo." else "Prueba otro nombre, ubicación o estado.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(query.isNotEmpty() || filter.isNotEmpty()) TextButton(onClick={query="";filter=""}) {Text("Limpiar filtros")}
            }
        }
        items(lista,key={it.id}) { e ->
            Surface(color=MaterialTheme.colorScheme.surfaceContainer,shape=RoundedCornerShape(20.dp),modifier=Modifier.testTag("espacio-${e.id}")) {
                Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        Text("ESPACIO ${e.id.toString().padStart(2,'0')}",Modifier.weight(1f),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,letterSpacing=1.sp)
                        Estado(e.estado)
                    }
                    Text(e.nombre,style=MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(e.ubicacion,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("${e.aforo}",style=MaterialTheme.typography.titleLarge)
                            Text("personas",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick={editar(e)},enabled=!s.busy) {Icon(Icons.Default.Edit,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("Editar")}
                        IconButton(onClick={borrar(e)},enabled=!s.busy) {Icon(Icons.Default.Delete,"Eliminar ${e.nombre}",tint=MaterialTheme.colorScheme.error)}
                    }
                }
            }
        }
        item { Text(if(s.usuario == null) "Consulta pública. Accede como operador para gestionar los espacios." else "Operador: ${s.usuario}",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
@Composable
private fun Plano(modifier: Modifier) {
    val color = MaterialTheme.colorScheme.onSecondaryContainer
    Canvas(modifier) {
        val w=size.width;val h=size.height
        fun line(x:Float,y:Float,x2:Float,y2:Float) {drawLine(color,Offset(w*x,h*y),Offset(w*x2,h*y2),strokeWidth=2.dp.toPx())}
        line(.08f,.12f,.9f,.12f);line(.9f,.12f,.9f,.9f);line(.9f,.9f,.58f,.9f);line(.35f,.9f,.08f,.9f);line(.08f,.9f,.08f,.12f)
        line(.08f,.48f,.45f,.48f);line(.65f,.48f,.9f,.48f);line(.52f,.12f,.52f,.3f)
    }
}

@Composable
private fun CuentaScreen(s: EventFlowState,login:(String,String)->Unit,logout:()->Unit) {
    var usuario by rememberSaveable { mutableStateOf("") }
    // La contraseña no usa rememberSaveable para no escribirla en el estado guardado.
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val submit = { focus.clearFocus();keyboard?.hide();login(usuario,password); Unit }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Icon(Icons.Default.AccountCircle,null,Modifier.size(48.dp),tint=MaterialTheme.colorScheme.primary)
        Text(if(s.usuario == null) "Tu equipo,\nbien conectado." else "Hola, ${s.usuario}.",style=MaterialTheme.typography.headlineLarge)
        Text(if(s.usuario == null) "Accede con tu cuenta de EventFlow para registrar y actualizar espacios." else "Tu sesión de operador está activa. Puedes registrar, editar y eliminar espacios.",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
        s.aviso?.let {Mensaje(it)}
        s.error?.let {Mensaje(it,true)}
        if(s.usuario == null) {
            OutlinedTextField(value=usuario,onValueChange={usuario=it},enabled=!s.busy,label={Text("Usuario")},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Next))
            OutlinedTextField(value=password,onValueChange={password=it},enabled=!s.busy,label={Text("Contraseña")},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),visualTransformation=if(visible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon={TextButton(onClick={visible=!visible}) {Text(if(visible) "Ocultar" else "Ver")}},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={submit()}))
            Button(onClick=submit,enabled=!s.busy,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),shape=RoundedCornerShape(14.dp)) {
                if(s.busy) {CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp);Spacer(Modifier.width(12.dp))}
                Text(if(s.busy) "Conectando…" else "Iniciar sesión")
            }
            Text("La consulta de espacios es pública. Solo necesitas una cuenta para hacer cambios.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Mensaje("Conectado como ${s.usuario}")
            OutlinedButton(onClick=logout,enabled=!s.busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {Text("Cerrar sesión")}
        }
    }
}

@Composable
private fun EditorScreen(s:EventFlowState,cambiar:(Formulario,String)->Unit,save:()->Unit,cancel:()->Unit) {
    val f=s.formulario
    val focus=LocalFocusManager.current
    val keyboard=LocalSoftwareKeyboardController.current
    val nombreFocus=remember {FocusRequester()};val lugarFocus=remember {FocusRequester()};val aforoFocus=remember {FocusRequester()}
    LaunchedEffect(s.intento) { if(s.intento>0) when {"nombre" in s.errores -> nombreFocus.requestFocus();"ubicacion" in s.errores -> lugarFocus.requestFocus();"aforo_maximo" in s.errores -> aforoFocus.requestFocus()} }
    Column(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            Text(if(f.id==0) "Un nuevo espacio.\nMás posibilidades." else "Cada detalle\ncuenta.",style=MaterialTheme.typography.headlineLarge)
            Text(if(f.id==0) "Completa los datos para registrarlo en EventFlow." else "Actualiza la información de ${f.nombre}.",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(f.id!=0) Text("ESPACIO #${f.id} · Identificador de solo lectura",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
            s.error?.let {Mensaje(it,true)}
            OutlinedTextField(value=f.nombre,onValueChange={cambiar(f.copy(nombre=it),"nombre")},label={Text("Nombre del espacio")},singleLine=true,enabled=!s.busy,isError="nombre" in s.errores,supportingText={Text(s.errores["nombre"] ?: "Hasta 120 caracteres")},modifier=Modifier.fillMaxWidth().focusRequester(nombreFocus),shape=RoundedCornerShape(14.dp),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Next))
            OutlinedTextField(value=f.ubicacion,onValueChange={cambiar(f.copy(ubicacion=it),"ubicacion")},label={Text("Ubicación o bloque")},singleLine=true,enabled=!s.busy,isError="ubicacion" in s.errores,supportingText={Text(s.errores["ubicacion"] ?: "Ejemplo: Bloque A, segundo piso")},modifier=Modifier.fillMaxWidth().focusRequester(lugarFocus),shape=RoundedCornerShape(14.dp),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Next))
            OutlinedTextField(value=f.aforo,onValueChange={cambiar(f.copy(aforo=it),"aforo_maximo")},label={Text("Aforo máximo")},singleLine=true,enabled=!s.busy,isError="aforo_maximo" in s.errores,supportingText={Text(s.errores["aforo_maximo"] ?: "Número de personas mayor que cero")},modifier=Modifier.fillMaxWidth().focusRequester(aforoFocus),shape=RoundedCornerShape(14.dp),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number,imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={focus.clearFocus();keyboard?.hide()}))
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Estado operativo",style=MaterialTheme.typography.titleMedium)
                estados.forEach { (key,label) ->
                    FilterChip(selected=f.estado==key,onClick={focus.clearFocus();keyboard?.hide();cambiar(f.copy(estado=key),"estado_operativo")},enabled=!s.busy,label={Text(label)},leadingIcon={if(f.estado==key) Icon(Icons.Default.Check,null)},modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),shape=RoundedCornerShape(12.dp))
                }
                s.errores["estado_operativo"]?.let {Text(it,color=MaterialTheme.colorScheme.error)}
            }
        }
        if(s.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick=cancel,enabled=!s.busy,modifier=Modifier.weight(1f).heightIn(min=52.dp),shape=RoundedCornerShape(14.dp),contentPadding=PaddingValues(12.dp)) {Text("Cancelar")}
            Button(onClick={focus.clearFocus();keyboard?.hide();save()},enabled=!s.busy,modifier=Modifier.weight(1.2f).heightIn(min=52.dp),shape=RoundedCornerShape(14.dp),contentPadding=PaddingValues(12.dp)) {Text(if(s.busy) "Guardando…" else "Guardar")}
        }
    }
}
