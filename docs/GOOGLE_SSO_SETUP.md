# Configurar Google Sign-In (Fase 0)

Guía para crear las credenciales OAuth que necesita el inicio de sesión con Google. Sin esto el
código funciona pero el botón "Continuar con Google" no aparece: la app se construye con
`GOOGLE_SERVER_CLIENT_ID` vacío y se reporta a sí misma como no disponible.

Al terminar vas a tener **un valor** que copiar en dos lugares:

| Valor | Dónde va |
|---|---|
| **Web client ID** | `local.properties` de la app (`GOOGLE_SERVER_CLIENT_ID`) y `.env` del servidor (`GOOGLE_OAUTH_CLIENT_IDS`) |

Los otros dos clientes (Android e iOS) no se copian a ningún archivo: solo tienen que existir para
que Google reconozca la app que hace la llamada.

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

## 5. Crear el iOS client ID (solo para la Fase 4)

Si por ahora solo vas a probar en Android, sáltate este paso.

1. **Google Auth Platform → Clients** → **Create client**.
2. **Application type**: **iOS**.
3. **Name**: `BudgetHunter iOS`.
4. **Bundle ID**: `com.meneses.budgethunter`
5. **Create**.
6. Guarda el **Client ID** y el **iOS URL scheme** (el *reversed client ID*,
   `com.googleusercontent.apps.123456789012-...`). Los dos hacen falta al agregar el SDK en Xcode.

> El token que emite el SDK de iOS lleva el **iOS client ID** en su campo `aud`, no el web. Por eso
> el backend acepta una **lista** de audiencias: cuando llegue la Fase 4 hay que agregar este
> client ID a `GOOGLE_OAUTH_CLIENT_IDS`, separado por coma.

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

### Desarrollo local

```bash
cd ~/Documents/BudgetHunter/BudgetHunterBackend
GOOGLE_OAUTH_CLIENT_IDS=123456789012-abc...apps.googleusercontent.com \
  ./gradlew bootRun --args='--spring.profiles.active=debug'
```

### Servidor

En `/opt/budgethunter/.env` del droplet, agrega:

```
GOOGLE_OAUTH_CLIENT_IDS=123456789012-abc...apps.googleusercontent.com
```

Y **antes** de desplegar el jar nuevo, corre la migración:

```bash
docker compose exec -T postgres psql -U budgethunter_user -d budgethunter \
  < database/migrations/001_add_google_sso.sql
```

> El orden importa. Producción arranca con `spring.jpa.hibernate.ddl-auto=validate`: si el jar
> nuevo se levanta contra el esquema viejo, la validación falla y el contenedor entra en bucle de
> reinicio.

Si dejas `GOOGLE_OAUTH_CLIENT_IDS` vacío, el resto del backend funciona normal y solo el endpoint
de Google responde 401 — el arranque no se rompe.

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

---

## Referencias

- [Sign in with Google en Android (Credential Manager)](https://developer.android.com/identity/sign-in/credential-manager-siwg)
- [Verificar ID tokens en el backend](https://developers.google.com/identity/sign-in/web/backend-auth)
- [Configurar la pantalla de consentimiento OAuth](https://support.google.com/googleapi/answer/6158849)
