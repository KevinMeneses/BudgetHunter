# Configurar Google Sign-In (Fase 0)

Guía para crear las credenciales OAuth que necesita el inicio de sesión con Google. Sin esto el
código funciona pero el botón "Continuar con Google" no aparece: la app se construye con
`GOOGLE_SERVER_CLIENT_ID` vacío y se reporta a sí misma como no disponible.

Al terminar vas a tener **un valor** que copiar en dos lugares:

| Valor | Dónde va |
|---|---|
| **Web client ID** | `local.properties` de la app (`GOOGLE_SERVER_CLIENT_ID`) y `.env` **local** del backend (`GOOGLE_OAUTH_CLIENT_IDS`), que `deploy.sh` sube al servidor |

El cliente Android no se copia a ningún archivo: solo tiene que existir para que Google reconozca la
app. El de iOS sí se usa — va en la configuración de Xcode y en la lista del backend (sección 5).

Tiempo aproximado: 15 minutos.

---

## 1. Crear el proyecto en Google Cloud

1. Entra a [console.cloud.google.com](https://console.cloud.google.com).
2. En el selector de proyectos (arriba a la izquierda) → **New Project**.
3. Nombre: `BudgetHunter`. Sin organización está bien.
4. **Create**, y espera a que el selector cambie al proyecto nuevo.

> Asegúrate de que el proyecto correcto esté seleccionado antes de cada paso siguiente. Crear
> credenciales en el proyecto equivocado es el error más fácil de cometer aquí.

---

## 2. Configurar la pantalla de consentimiento

Google reorganizó esta sección: lo que antes era *OAuth consent screen* ahora vive en
**APIs & Services → Google Auth Platform**, dividido en pestañas *Branding*, *Audience*,
*Data access* y *Clients*. Si tu consola todavía muestra el menú viejo, es la misma información.

### 2.1 Branding

1. **APIs & Services → Google Auth Platform → Branding** (si es la primera vez, te va a pedir
   **Get started**).
2. **App name**: `BudgetHunter`. Es el nombre que el usuario ve en la pantalla de Google, así que
   ponlo tal cual quieres que se lea.
3. **User support email**: tu correo.
4. **Developer contact information**: tu correo.
5. Logo y dominios son opcionales; déjalos vacíos por ahora.
6. **Save**.

### 2.2 Audience

1. Pestaña **Audience**.
2. **User type**: **External**. (*Internal* solo existe si tienes Google Workspace y limitaría el
   acceso a tu propio dominio.)
3. El proyecto queda en estado **Testing**. Eso está bien para desarrollo, pero implica que
   **solo pueden entrar los correos que agregues explícitamente**.
4. En **Test users** → **Add users** → agrega tu cuenta de Gmail y cualquier otra con la que vayas
   a probar. Hasta 100.
5. **Save**.

> Cuando vayas a publicar la app, vuelve aquí y usa **Publish app**. Como solo pedimos los scopes
> básicos, Google no requiere proceso de verificación. Si no publicas, cualquier usuario que no
> esté en la lista de test users recibirá un error al intentar entrar.

### 2.3 Data access (scopes)

1. Pestaña **Data access**.
2. **No agregues ningún scope.** Los tres que necesitamos —`openid`, `email`, `profile`— son los
   básicos y se incluyen solos. Agregar cualquier otro dispara el proceso de verificación de
   Google, que toma semanas.

---

## 3. Crear el Web client ID ← el importante

Este es el que la app le pasa a Google como `serverClientId`, y es el que hace que el ID token
venga emitido *para nuestro backend*.

1. **Google Auth Platform → Clients** → **Create client**.
   (Ruta equivalente en el menú clásico: *APIs & Services → Credentials → Create credentials →
   OAuth client ID*.)
2. **Application type**: **Web application**.
3. **Name**: `BudgetHunter Backend`.
4. **Authorized JavaScript origins** y **Authorized redirect URIs**: **déjalos vacíos**. Solo
   verificamos ID tokens; nunca corremos un flujo de redirección de navegador.
5. **Create**.
6. Copia el **Client ID**. Se ve así:
   ```
   123456789012-abcdefghijklmnopqrstuvwxyz123456.apps.googleusercontent.com
   ```

> Este valor **no es secreto**. Viaja dentro del APK por diseño. El *client secret* que Google te
> muestra al lado sí lo es, pero nosotros no lo usamos: bórralo de tu portapapeles y no lo pongas
> en ningún archivo.

---

## 4. Crear el Android client ID

Este no se copia a ningún lado. Existe para que Google pueda verificar que quien pide el token es
de verdad tu app, comparando la firma del APK.

### 4.1 Obtener el SHA-1 de debug

```bash
keytool -list -v \
  -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android
```

De la salida copia la línea **`SHA1:`** — son 20 pares hexadecimales separados por dos puntos:

```
SHA1: A1:B2:C3:D4:E5:F6:...
```

### 4.2 Registrarlo

1. **Google Auth Platform → Clients** → **Create client**.
2. **Application type**: **Android**.
3. **Name**: `BudgetHunter Android (debug)`.
4. **Package name**: `com.meneses.budgethunter`
5. **SHA-1 certificate fingerprint**: el valor del paso anterior.
6. **Create**.

> ⚠️ **Antes de publicar en Play Store** vas a necesitar registrar otros dos SHA-1: el de tu
> keystore de release (el que está en el secret `SIGNING_KEY` del repo) y —más importante— el de
> **Play App Signing**, que encuentras en Play Console → *Test and release* → *Setup* →
> *App integrity* → *App signing key certificate*. Google re-firma el AAB al publicarlo, así que
> el certificado que ven tus usuarios no es el tuyo. Registrar solo el de subida produce el
> síntoma clásico: funciona en debug y falla para todos los usuarios de Play.
>
> No hace falta ahora porque la app todavía no está publicada, pero anótalo.

---

## 5. iOS

El SDK de Google ya está agregado al proyecto Xcode por Swift Package Manager; lo que falta son
tus valores. Sin ellos la app compila igual y el botón simplemente no aparece en iOS.

### 5.1 Crear el iOS client ID

1. **Google Auth Platform → Clients** → **Create client**.
2. **Application type**: **iOS**.
3. **Name**: `BudgetHunter iOS`.
4. **Bundle ID**: `com.meneses.budgethunter`
5. **Create**, y copia el **Client ID**.

### 5.2 Ponerlo en la app

Edita `iosApp/Config.xcconfig` (está en `.gitignore`; es el que usa la configuración Debug y el
mismo donde ya tienes `GEMINI_API_KEY`):

```
GOOGLE_IOS_CLIENT_ID = 123456789012-abcdef.apps.googleusercontent.com
GOOGLE_REVERSED_CLIENT_ID = com.googleusercontent.apps.123456789012-abcdef
```

El segundo es el mismo ID **al revés**: quita `.apps.googleusercontent.com` del final y ponle
`com.googleusercontent.apps.` delante. Es el esquema de URL por el que la hoja de Google le
devuelve el control a la app; si está mal, el login se abre pero nunca termina.

⚠️ Sin comillas. En un `.xcconfig`, `//` inicia un comentario — no afecta a estos valores porque
ninguno lo contiene, pero no pegues URLs completas.

> La configuración **Release** lee `iosApp/Configuration/Config.xcconfig` (el commiteado), que
> trae las dos claves vacías. Es el mismo arreglo que ya tiene `GEMINI_API_KEY`: para compilar
> Release con Google habría que poner los valores también ahí.

### 5.3 Agregarlo al backend

El token que emite el SDK de iOS lleva el **iOS client ID** en `aud`, no el web. Agrégalo a la
lista en el `.env` **local** del backend, separado por coma:

```
GOOGLE_OAUTH_CLIENT_IDS=<web-client-id>,<ios-client-id>
```

y despliega con `./deploy.sh`. Sin este paso, iOS abre la hoja de Google, elige cuenta, y el
backend rechaza el token con 401 — que parece un problema de Google y no lo es.

> No configuramos `GIDServerClientID`. Hay ambigüedad sobre si cambia el `aud` del token en iOS,
> pero como el backend acepta ambos IDs, el token valida en cualquiera de los dos casos.

### 5.4 Probar

Abre `iosApp/iosApp.xcodeproj` en Xcode (la primera vez descarga el paquete de Google; necesita
red) y corre en un simulador. El botón "Continuar con Google" debe aparecer; si no, el
`GOOGLE_IOS_CLIENT_ID` no llegó o no tiene forma de client ID.

---

## 6. Poner el Web client ID en la app

Edita `local.properties` en la raíz del proyecto Android (ya tiene la línea vacía esperando):

```properties
GOOGLE_SERVER_CLIENT_ID=123456789012-abc...apps.googleusercontent.com
```

Ese archivo está en `.gitignore`, así que no se sube. Reconstruye:

```bash
./gradlew :composeApp:installDebug
```

Si el botón sigue sin aparecer, el valor no llegó: revisa que no haya espacios ni comillas.

### En CI

Los workflows ya leen el secret `GOOGLE_SERVER_CLIENT_ID`. Créalo en GitHub →
*Settings* → *Secrets and variables* → *Actions* → **New repository secret**, con el mismo valor.

Si no lo creas, CI no falla — el secret llega vacío y la app se construye sin el botón.

---

## 7. Poner el Web client ID en el backend

El backend vive en el repo aparte: `~/Documents/BudgetHunter/BudgetHunterBackend`.

**No edites el `.env` del droplet a mano.** `deploy.sh` sube tu `.env` local y sobrescribe el del
servidor, así que cualquier cambio hecho directamente allá se pierde en el siguiente despliegue. El
archivo que editas es el local.

> Hasta septiembre de 2026 esto no era cierto: el script preparaba el `.env` pero lo subía con
> `scp -r deploy-package/*`, que no incluye archivos ocultos, así que nunca llegaba. El servidor
> conservaba una copia puesta a mano tiempo atrás. Corregido en `1f6f861`; si trabajas con una
> versión anterior del script, revisa que el `.env` del servidor tenga la variable.

### 7.1 Editar el `.env` local

Abre `~/Documents/BudgetHunter/BudgetHunterBackend/.env` y agrega **una sola línea**:

```properties
GOOGLE_OAUTH_CLIENT_IDS=123456789012-abc...apps.googleusercontent.com
```

⚠️ Una línea literal: **sin comillas, sin barra invertida al final, sin comandos pegados**. Docker
Compose lee este archivo como pares clave/valor, no como script de shell — una `\` al final se
vuelve parte del client ID y el token deja de validar.

Verifica que quedó bien formado:

```bash
cd ~/Documents/BudgetHunter/BudgetHunterBackend
grep -c '^GOOGLE_OAUTH_CLIENT_IDS=[0-9]\{6,\}-[a-z0-9]\{10,\}\.apps\.googleusercontent\.com$' .env
```

Debe imprimir `1`. Si imprime `0`, algo sobra o falta en la línea.

### 7.2 Correr la migración en el servidor

Antes de desplegar el jar nuevo. `deploy.sh` no corre migraciones y el paquete que sube no incluye
`database/migrations/`, así que el SQL se envía por `ssh` desde tu máquina:

```bash
cd ~/Documents/BudgetHunter/BudgetHunterBackend
source .env.server   # trae SERVER_IP y SERVER_USER

ssh $SERVER_USER@$SERVER_IP \
  "cd /opt/budgethunter && docker compose exec -T postgres psql -U budgethunter_user -d budgethunter" \
  < database/migrations/001_add_google_sso.sql
```

Debe responder con `BEGIN`, tres `ALTER TABLE`, `CREATE INDEX` y `COMMIT`. Confirma el resultado:

```bash
ssh $SERVER_USER@$SERVER_IP \
  "cd /opt/budgethunter && docker compose exec -T postgres psql -U budgethunter_user -d budgethunter \
   -c '\\d users'"
```

Deben aparecer `google_subject` y `auth_provider`, y `password` **sin** `not null`.

El script es idempotente (`ADD COLUMN IF NOT EXISTS`, `CREATE UNIQUE INDEX IF NOT EXISTS`), así que
volver a correrlo no hace daño.

### 7.3 Desplegar

```bash
./deploy.sh
```

Toma el `SERVER_IP` de `.env.server`, compila, sube el jar junto con tu `.env`, levanta los
contenedores y verifica `/actuator/health` — primero dentro del servidor y luego por HTTPS.

Comprueba que la variable llegó de verdad al contenedor, no solo al archivo:

```bash
ssh $SERVER_USER@$SERVER_IP \
  "cd /opt/budgethunter && docker compose exec -T backend printenv GOOGLE_OAUTH_CLIENT_IDS"
```

### 7.4 Comprobar que quedó configurado

Un token basura debe dar **401**, no 500 ni 404:

```bash
curl -s -o /dev/null -w '%{http_code}\n' \
  -X POST https://budgethunter.duckdns.org/api/users/sign_in_with_google \
  -H 'Content-Type: application/json' \
  -d '{"idToken":"basura"}'
```

- `401` → todo bien, el endpoint existe y está rechazando como debe.
- `404` → el jar desplegado es el viejo; el despliegue no pasó.
- `500` → revisa los logs: `ssh $SERVER_USER@$SERVER_IP 'cd /opt/budgethunter && docker compose logs backend --tail=50'`

> El mensaje del 401 distingue los dos casos: `"Invalid Google ID token"` significa que la
> configuración está bien y el token simplemente no sirve; `"Google sign in is not configured"`
> significa que `GOOGLE_OAUTH_CLIENT_IDS` llegó vacío.

Si dejas la variable vacía, el resto del backend funciona normal y solo este endpoint responde 401
— el arranque no se rompe.

---

## 8. Probar

Necesitas un emulador o dispositivo con **Google Play Services y una cuenta de Google
configurada**. Una imagen AOSP sin Play Store no tiene proveedor de credenciales y siempre falla.

Con el backend local corriendo y `BACKEND_URL=http://10.0.2.2:8080` en `local.properties`:

1. **Cuenta nueva** — toca "Continuar con Google", elige tu cuenta → debe llegar a la lista de
   presupuestos. En la base: `SELECT email, auth_provider, password FROM users;` → `GOOGLE` y
   `password` en null.
2. **Cancelar** — toca el botón y cierra el selector → no debe aparecer ningún mensaje de error y
   el spinner debe apagarse.
3. **Vinculación** — regístrate con correo y contraseña usando tu misma dirección de Gmail, cierra
   sesión, y entra con Google → misma cuenta, mismos presupuestos, `auth_provider` pasa a
   `PASSWORD_AND_GOOGLE`.
4. **Agregar contraseña** — entra con Google (cuenta nueva), ve a **Ajustes → Cuenta → Establecer
   contraseña**, cierra sesión y entra con correo y esa contraseña.
5. **Cambiar de cuenta** — cierra sesión y vuelve a tocar el botón → el selector debe reaparecer.
   Si entra directo sin preguntar, la credencial cacheada no se limpió.

---

## Si algo falla

| Síntoma | Causa casi segura |
|---|---|
| El botón no aparece | `GOOGLE_SERVER_CLIENT_ID` vacío en `local.properties`, o no reconstruiste |
| `NoCredentialException` / "No hay cuentas de Google" | No hay cuenta de Google en el dispositivo, o la imagen del emulador no trae Play Services |
| El selector abre y falla al elegir cuenta | SHA-1 o package name mal registrados en el cliente Android |
| Tu correo recibe "acceso denegado" de Google | Falta agregarlo como *test user* en la pestaña Audience |
| El backend responde 401 con un token que parece válido | `GOOGLE_OAUTH_CLIENT_IDS` vacío, o no coincide con el web client ID |
| "Funcionaba y dejó de funcionar de repente" | Tras varios descartes seguidos del selector, Google impone un enfriamiento de 24 h. Limpia los datos de Play Services o cambia de cuenta en el emulador |
| El backend no arranca tras desplegar | Falta correr `001_add_google_sso.sql` |
| **iOS:** el botón no aparece | `GOOGLE_IOS_CLIENT_ID` vacío o sin la forma `….apps.googleusercontent.com` en `iosApp/Config.xcconfig` |
| **iOS:** la hoja de Google abre pero nunca vuelve a la app | `GOOGLE_REVERSED_CLIENT_ID` mal invertido |
| **iOS:** eliges cuenta y el backend responde 401 | El iOS client ID no está en `GOOGLE_OAUTH_CLIENT_IDS` del backend |

---

## Referencias

- [Sign in with Google en Android (Credential Manager)](https://developer.android.com/identity/sign-in/credential-manager-siwg)
- [Verificar ID tokens en el backend](https://developers.google.com/identity/sign-in/web/backend-auth)
- [Configurar la pantalla de consentimiento OAuth](https://support.google.com/googleapi/answer/6158849)
