# Explicación del código — ESN Messenger

## Índice
1. [Estructura del proyecto](#1-estructura-del-proyecto)
2. [Tecnologías utilizadas](#2-tecnologías-utilizadas)
3. [Jetpack Compose — la base de la UI](#3-jetpack-compose--la-base-de-la-ui)
4. [Firebase — backend de la app](#4-firebase--backend-de-la-app)
5. [Navegación entre pantallas](#5-navegación-entre-pantallas)
6. [Pantalla de Login](#6-pantalla-de-login)
7. [Pantalla de Registro](#7-pantalla-de-registro)
8. [Pantalla de Onboarding](#8-pantalla-de-onboarding)
9. [Pantalla principal — HomeScreen](#9-pantalla-principal--homescreen)
10. [Pantalla de Perfil](#10-pantalla-de-perfil)
11. [Pantalla de Restaurantes](#11-pantalla-de-restaurantes)
12. [Pantalla de Chat](#12-pantalla-de-chat)
13. [Flujo completo de la app](#13-flujo-completo-de-la-app)

---

## 1. Estructura del proyecto

```
app/src/main/java/com/example/esnmessenger/
│
├── MainActivity.kt               # Punto de entrada de la app
│
├── navigation/
│   └── NavGraph.kt               # Define todas las rutas de navegación
│
├── screens/                      # Una pantalla = un archivo
│   ├── LoginScreen.kt
│   ├── RegisterScreen.kt
│   ├── OnboardingScreen.kt
│   ├── HomeScreen.kt
│   ├── ProfileScreen.kt
│   ├── RestaurantsScreen.kt
│   └── ChatScreen.kt
│
├── viewmodel/                    # Lógica de negocio separada de la UI
│   ├── RestaurantsViewModel.kt
│   └── ChatViewModel.kt
│
├── model/                        # Clases de datos (estructuras)
│   ├── Restaurant.kt
│   └── Message.kt
│
├── network/                      # Llamadas a APIs externas
│   ├── JamixService.kt
│   └── JamixModels.kt
│
└── ui/theme/                     # Colores, tipografía y tema visual
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

La app sigue una arquitectura **MVVM** (Model - View - ViewModel):
- **Model**: clases de datos (`Restaurant`, `Message`) y Firebase
- **View**: las pantallas en `screens/` escritas con Compose
- **ViewModel**: lógica entre la UI y los datos (`RestaurantsViewModel`, `ChatViewModel`)

---

## 2. Tecnologías utilizadas

### Kotlin
Lenguaje de programación principal. Es el estándar moderno para Android, más conciso y seguro que Java.

### Jetpack Compose
Framework de Google para construir interfaces de usuario en Android. En vez de usar archivos XML para diseñar pantallas, todo se escribe directamente en Kotlin con funciones especiales llamadas `@Composable`.

### Firebase
Plataforma de Google que proporciona servicios de backend sin necesidad de montar un servidor propio:
- **Firebase Auth**: gestión de usuarios (registro, login, Google Sign-In)
- **Firebase Firestore**: base de datos en la nube donde se guardan los perfiles, mensajes, etc.

### Retrofit
Librería para hacer llamadas HTTP a APIs externas. Se usa para obtener los menús de los restaurantes universitarios desde la API de Jamix.

### Navigation Compose
Librería oficial de Android para gestionar la navegación entre pantallas dentro de una app Compose.

---

## 3. Jetpack Compose — la base de la UI

### ¿Qué es un @Composable?
Una función marcada con `@Composable` es un bloque de interfaz de usuario. En vez de crear vistas en XML, defines cómo se ve algo directamente en código:

```kotlin
@Composable
fun Saludo(nombre: String) {
    Text(text = "Hola, $nombre!")
}
```

Cuando los datos cambian, Compose automáticamente vuelve a dibujar solo las partes afectadas. A esto se le llama **recomposición**.

### Estado — remember y mutableStateOf
El **estado** es cualquier dato que puede cambiar y que afecta a lo que se muestra en pantalla. Se declara así:

```kotlin
var nombre by remember { mutableStateOf("") }
```

- `mutableStateOf("")`: crea un valor observable. Cuando cambia, Compose redibuja la UI.
- `remember`: hace que el valor sobreviva a las recomposiciones. Sin él, se resetearía cada vez.

Ejemplo práctico del LoginScreen:
```kotlin
var email by remember { mutableStateOf("") }
var password by remember { mutableStateOf("") }
var isLoading by remember { mutableStateOf(false) }
```

Cuando el usuario escribe en el campo de email, `email` cambia → Compose redibuja el campo → el texto aparece actualizado.

### LaunchedEffect
Se usa para ejecutar código que no es UI cuando el composable aparece en pantalla (por ejemplo, cargar datos de Firebase):

```kotlin
LaunchedEffect(Unit) {
    // Esto se ejecuta una sola vez cuando la pantalla aparece
    FirebaseFirestore.getInstance().collection("users").document(uid).get()
        .addOnSuccessListener { doc -> profile = doc.data }
}
```

El parámetro `Unit` significa "ejecuta esto una sola vez". Si pusieras una variable, se re-ejecutaría cada vez que esa variable cambiara.

### ViewModel
Un ViewModel es una clase que contiene la lógica y los datos de una pantalla, separados de la UI. Sobrevive a los cambios de configuración (como rotar el móvil).

```kotlin
class RestaurantsViewModel : ViewModel() {
    // Los datos que la pantalla necesita
    private val _menus = MutableStateFlow(...)
    val menus: StateFlow<List<DailyMenu>> = _menus

    // La lógica para obtener esos datos
    fun fetchAllMenus() { ... }
}
```

En la pantalla se usa así:
```kotlin
@Composable
fun RestaurantsScreen(viewModel: RestaurantsViewModel = viewModel()) {
    val menus by viewModel.menus.collectAsState()
    // menus se actualiza automáticamente cuando el ViewModel cambia los datos
}
```

### StateFlow vs mutableStateOf
- `mutableStateOf`: para estado local dentro de un composable (campos de texto, flags de UI)
- `StateFlow`: para estado en un ViewModel que la pantalla observa

---

## 4. Firebase — backend de la app

### Firebase Auth
Gestiona los usuarios. Cuando alguien se registra o hace login, Firebase crea una sesión y devuelve un objeto `FirebaseUser` con información básica del usuario.

```kotlin
val auth = FirebaseAuth.getInstance()

// Registrar usuario nuevo
auth.createUserWithEmailAndPassword(email, password)
    .addOnCompleteListener { task ->
        if (task.isSuccessful) {
            // Usuario creado correctamente
            val uid = task.result?.user?.uid
        }
    }

// Iniciar sesión
auth.signInWithEmailAndPassword(email, password)
    .addOnCompleteListener { task ->
        if (task.isSuccessful) onLoginSuccess()
    }

// Cerrar sesión
auth.signOut()

// Obtener usuario actual
val currentUser = auth.currentUser // null si no hay sesión
```

Cada usuario tiene un **UID** (identificador único). Es la clave que usamos para relacionar el usuario de Auth con sus datos en Firestore.

### Firebase Firestore
Base de datos NoSQL orientada a documentos. La estructura es:

```
Firestore
└── users/                        ← Colección
    ├── uid123/                   ← Documento (un usuario)
    │   ├── name: "Marcos"
    │   ├── email: "marcos@..."
    │   ├── university: "OaMK"
    │   ├── major: "IT"
    │   ├── year: "3rd year"
    │   ├── studentType: "International"
    │   ├── interests: ["Sports", "Music"]
    │   └── photoBase64: "..."
    └── uid456/                   ← Otro usuario
        └── ...
```

**Leer un documento:**
```kotlin
FirebaseFirestore.getInstance()
    .collection("users")
    .document(uid)          // accede al documento del usuario
    .get()
    .addOnSuccessListener { doc ->
        val nombre = doc.getString("name")
        val intereses = doc.get("interests") as? List<String>
    }
```

**Escribir/actualizar datos:**
```kotlin
// set() sobreescribe todo el documento
firestore.collection("users").document(uid).set(mapOf("name" to "Marcos", ...))

// update() solo actualiza los campos indicados
firestore.collection("users").document(uid).update("name", "Marcos")
```

**Buscar por campo:**
```kotlin
firestore.collection("users")
    .whereEqualTo("email", "marcos@example.com")
    .limit(1)
    .get()
    .addOnSuccessListener { snapshot ->
        val user = snapshot.documents.firstOrNull()
    }
```

### Google Sign-In
Permite iniciar sesión con una cuenta de Google. El proceso es:
1. Se abre una pantalla de selección de cuenta de Google
2. Google devuelve un token de identidad (`idToken`)
3. Se pasa ese token a Firebase Auth para crear/iniciar sesión

```kotlin
val credential = GoogleAuthProvider.getCredential(account.idToken, null)
auth.signInWithCredential(credential)
    .addOnCompleteListener { task ->
        if (task.isSuccessful) { /* sesión iniciada */ }
    }
```

---

## 5. Navegación entre pantallas

La navegación se gestiona en `NavGraph.kt`. Funciona como un mapa de rutas:

```kotlin
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val ONBOARDING = "onboarding?email={email}"
    const val CHAT = "chat/{otherUserId}"
}
```

Cada ruta tiene una pantalla asociada:

```kotlin
NavHost(navController = navController, startDestination = ...) {
    composable(Routes.LOGIN) {
        LoginScreen(
            onLoginSuccess = { navController.navigate(Routes.HOME) },
            onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
        )
    }
    composable(Routes.HOME) {
        HomeScreen(onLogout = { navController.navigate(Routes.LOGIN) })
    }
}
```

El `navController` es el objeto que controla la navegación. Para ir a otra pantalla:
```kotlin
navController.navigate(Routes.HOME)
// Con parámetro:
navController.navigate("chat/uid123")
// Limpiar el historial al navegar (para que el usuario no pueda volver atrás):
navController.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
```

### ProfileCheckScreen
Al abrir la app, si hay un usuario con sesión activa, se comprueba si ya completó el Onboarding:
- Si tiene perfil en Firestore → va directamente a Home
- Si no tiene perfil → va a Onboarding

```kotlin
LaunchedEffect(uid) {
    firestore.collection("users").document(uid).get()
        .addOnSuccessListener { doc ->
            if (doc.exists()) onHasProfile()
            else onNoProfile()
        }
}
```

---

## 6. Pantalla de Login

**Archivo:** `LoginScreen.kt`

Permite al usuario iniciar sesión con email/contraseña o con Google.

**Estado de la pantalla:**
```kotlin
var email by remember { mutableStateOf("") }
var password by remember { mutableStateOf("") }
var isLoading by remember { mutableStateOf(false) }
var errorMessage by remember { mutableStateOf("") }
var showForgotDialog by remember { mutableStateOf(false) }
```

**Flujo de login con email:**
1. El usuario rellena email y contraseña
2. Al pulsar "Sign In", se llama a `auth.signInWithEmailAndPassword()`
3. `isLoading = true` → aparece un spinner en el botón
4. Si tiene éxito → `onLoginSuccess()` → navega a Home
5. Si falla → `errorMessage` se actualiza → aparece el mensaje de error

**Forgot password:**
Se abre un `AlertDialog` donde el usuario introduce su email. Se llama a `auth.sendPasswordResetEmail()` y Firebase envía un correo de recuperación.

**Google Sign-In:**
- Si el usuario ya tiene cuenta → inicia sesión
- Si es nuevo (`isNewUser == true`) → se borra la cuenta recién creada y se muestra error. El registro con Google se hace desde RegisterScreen.

---

## 7. Pantalla de Registro

**Archivo:** `RegisterScreen.kt`

Permite crear una cuenta nueva con email/contraseña o Google.

**Validaciones antes de registrar:**
```kotlin
when {
    email.isBlank() || password.isBlank() -> errorMessage = "Please fill in all fields"
    password != confirmPassword -> errorMessage = "Passwords don't match"
    password.length < 6 -> errorMessage = "Password must be at least 6 characters"
    else -> { /* proceder con el registro */ }
}
```

Tras registrarse correctamente, navega a **OnboardingScreen** pasando el email como parámetro, para que el usuario complete su perfil.

---

## 8. Pantalla de Onboarding

**Archivo:** `OnboardingScreen.kt`

Se muestra una sola vez, justo después del registro. Recoge información del perfil en 3 pasos:

- **Paso 1 — About You**: nombre completo, universidad, tipo de estudiante (International/Local)
- **Paso 2 — Academic Details**: carrera y año de estudio
- **Paso 3 — Interests**: selección de intereses (Sports, Music, etc.)

El estado del paso actual se controla con:
```kotlin
var step by remember { mutableIntStateOf(1) }
```

Al finalizar el paso 3, todos los datos se guardan en Firestore bajo `users/{uid}`:
```kotlin
val profile = hashMapOf(
    "email" to email,
    "name" to name,
    "university" to university,
    "studentType" to studentType,
    "major" to major,
    "year" to year,
    "interests" to selectedInterests.toList()
)
firestore.collection("users").document(uid).set(profile)
```

Una vez guardado, navega a HomeScreen. Esta pantalla no se vuelve a mostrar porque `ProfileCheckScreen` detecta que el documento ya existe en Firestore.

---

## 9. Pantalla principal — HomeScreen

**Archivo:** `HomeScreen.kt`

Es la pantalla central de la app tras el login. Tiene una barra de navegación inferior con 3 pestañas:

```kotlin
private enum class HomeTab { Messages, Restaurants, Profile }
```

Cada pestaña carga su pantalla correspondiente:
```kotlin
when (selectedTab) {
    HomeTab.Messages -> MessagesTab(...)
    HomeTab.Restaurants -> RestaurantsScreen()
    HomeTab.Profile -> ProfileScreen()
}
```

**MessagesTab:**
Permite buscar un usuario por email para abrir un chat. Busca en Firestore el documento cuyo campo `email` coincida con el introducido y, si lo encuentra, navega al chat pasando el UID del otro usuario.

**Nombre en el header:**
Al cargar la pestaña, se lee el campo `name` del documento del usuario en Firestore. Si está disponible, se muestra el nombre en vez del email.

**Sign Out:**
Al pulsar "Sign out" aparece un `AlertDialog` de confirmación. Si confirma, se llama a `auth.signOut()` y se navega al Login.

---

## 10. Pantalla de Perfil

**Archivo:** `ProfileScreen.kt`

Muestra y permite editar los datos del usuario. Tiene dos modos: **lectura** y **edición**.

### Carga del perfil
Al aparecer la pantalla, se lee el documento del usuario de Firestore:
```kotlin
LaunchedEffect(Unit) {
    FirebaseFirestore.getInstance().collection("users").document(uid).get()
        .addOnSuccessListener { doc ->
            profile = doc.data
            photoBase64 = doc.getString("photoBase64")
        }
}
```

### Foto de perfil
La foto se almacena directamente en Firestore como una cadena de texto en formato **Base64** (una forma de convertir una imagen binaria en texto). El proceso es:

1. El usuario selecciona una imagen de la galería con `ActivityResultContracts.GetContent()`
2. La imagen se escala a máximo 300×300 píxeles y se comprime al 65% de calidad JPEG
3. El resultado se codifica en Base64 (texto)
4. Se guarda en Firestore en el campo `photoBase64`

```kotlin
// Comprimir y codificar
val stream = context.contentResolver.openInputStream(uri)
val original = BitmapFactory.decodeStream(stream)
val scaled = Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
val output = ByteArrayOutputStream()
scaled.compress(Bitmap.CompressFormat.JPEG, 65, output)
val base64 = Base64.encodeToString(output.toByteArray(), Base64.DEFAULT)
```

Para mostrar la foto, se hace el proceso inverso:
```kotlin
val bytes = Base64.decode(base64, Base64.DEFAULT)
val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
```

La compresión es necesaria porque los documentos de Firestore tienen un límite de 1MB.

### Modo edición
Al pulsar el icono de lápiz en el header, `isEditing = true` y se muestra `EditProfileContent` en lugar de `ViewProfileContent`.

El formulario de edición pre-rellena todos los campos con los valores actuales. Al guardar, solo se actualizan los campos modificables (no el email):
```kotlin
val updates = mapOf(
    "name" to name,
    "university" to university,
    "studentType" to studentType,
    "major" to major,
    "year" to year,
    "interests" to selectedInterests.toList()
)
firestore.collection("users").document(uid).update(updates)
```

---

## 11. Pantalla de Restaurantes

**Archivos:** `RestaurantsScreen.kt`, `RestaurantsViewModel.kt`, `model/Restaurant.kt`, `network/JamixService.kt`

Muestra el menú diario de los restaurantes universitarios de OaMK, obtenido desde la API de **Jamix**.

### Los restaurantes
Están definidos como constantes en `Restaurant.kt`:
```kotlin
val OAMK_RESTAURANTS = listOf(
    Restaurant(1, "Ravintola Mara", "Linnanmaa", kitchenId = 49),
    Restaurant(2, "Ravintola Alwari", "Kontinkangas", kitchenId = 73),
    Restaurant(3, "Ravintola Foobar", "Linnanmaa", kitchenId = 69)
)
```

El `kitchenId` es el identificador que usa la API de Jamix para identificar cada cocina.

### El ViewModel
`RestaurantsViewModel` gestiona la lógica:

```kotlin
class RestaurantsViewModel : ViewModel() {
    private val _menus = MutableStateFlow(OAMK_RESTAURANTS.map { DailyMenu(it) })
    val menus: StateFlow<List<DailyMenu>> = _menus

    private val _isWeekend = MutableStateFlow(false)
    val isWeekend: StateFlow<Boolean> = _isWeekend

    init {
        fetchAllMenus() // se llama automáticamente al crear el ViewModel
    }

    fun fetchAllMenus() {
        // 1. Comprobar si es fin de semana
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            _isWeekend.value = true
            return // no llamar a la API
        }
        // 2. Para cada restaurante, pedir su menú a la API
        OAMK_RESTAURANTS.forEach { restaurant ->
            viewModelScope.launch {
                val response = JamixService.instance.getMenu(...)
                // 3. Transformar la respuesta en objetos MealOption
                // 4. Actualizar _menus
            }
        }
    }
}
```

`viewModelScope.launch` lanza una coroutine — una operación asíncrona que no bloquea el hilo principal. La llamada a la API se hace en segundo plano y cuando termina actualiza el estado.

### Detectar fin de semana
Los restaurantes universitarios cierran en fin de semana. En vez de hacer llamadas a la API que devolverían menú vacío, se detecta el día antes de llamar:

```kotlin
val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
    _isWeekend.value = true
    return
}
```

La pantalla observa `isWeekend` y muestra un mensaje informativo en lugar de las tarjetas.

### La API de Jamix y Retrofit
Retrofit permite definir las llamadas HTTP como funciones de Kotlin:

```kotlin
interface JamixApi {
    @GET("MenuData")
    suspend fun getMenu(
        @Query("customerId") customerId: Int,
        @Query("kitchenId") kitchenId: Int,
        @Query("date") date: String,
        @Query("date2") date2: String
    ): List<JamixDayResponse>
}
```

`suspend` indica que es una función que puede pausarse y reanudarse (asíncrona), debe llamarse desde una coroutine.

---

## 12. Pantalla de Chat

**Archivos:** `ChatScreen.kt`, `ChatViewModel.kt`, `model/Message.kt`

Permite el chat en tiempo real entre dos usuarios usando Firestore.

Cada conversación se identifica por una combinación de los UIDs de los dos usuarios. Los mensajes se guardan en Firestore y Firestore notifica en tiempo real cuando hay mensajes nuevos gracias a `addSnapshotListener`.

---

## 13. Flujo completo de la app

```
App se abre
    │
    ├─ ¿Hay sesión activa?
    │   │
    │   ├─ NO → LoginScreen
    │   │           │
    │   │           ├─ Login exitoso → HomeScreen
    │   │           └─ "Registrarse" → RegisterScreen
    │   │                               │
    │   │                               └─ Registro exitoso → OnboardingScreen
    │   │                                                       │
    │   │                                                       └─ Onboarding completo → HomeScreen
    │   │
    │   └─ SÍ → ProfileCheckScreen (comprueba si tiene perfil en Firestore)
    │               │
    │               ├─ Tiene perfil → HomeScreen
    │               └─ No tiene perfil → OnboardingScreen
    │
    └─ HomeScreen (3 pestañas)
            │
            ├─ Messages → buscar usuario por email → ChatScreen
            ├─ Restaurants → menús del día de OaMK
            └─ Profile → ver/editar perfil + cambiar foto
```

### Resumen de datos en Firestore

| Colección | Documento | Campos |
|-----------|-----------|--------|
| `users` | `{uid}` | name, email, university, studentType, major, year, interests, photoBase64 |
| `messages` | `{conversationId}` | (gestionado por el equipo de Chat) |

---

*Documento generado para la memoria del TFG — ESN Messenger*
