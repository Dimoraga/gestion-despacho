# gestión-despacho

Sistema de Gestión de Pedidos y Generación de Guías de Despacho para una empresa transportista. Cada guía se genera como **PDF** (OpenPDF), se almacena temporalmente en **EFS** y se sube automáticamente a **Amazon S3** en carpetas organizadas por fecha y transportista. Expone una **API REST** documentada con Swagger, se empaqueta con **Docker** y se despliega en **EC2** mediante un pipeline de **CI/CD** (GitHub Actions).

## Stack

| Componente | Versión / Detalle |
|---|---|
| Spring Boot | 3.5.x |
| Java | 17 (Temurin) |
| Base de datos | H2 embebido persistido en EFS por el Consumidor |
| AWS SDK | v2 (`software.amazon.awssdk:s3`) |
| Generación PDF | OpenPDF (`com.github.librepdf:openpdf`) |
| Documentación API | springdoc-openapi (Swagger UI) |
| Health / métricas | spring-boot-starter-actuator (`/actuator/health`) |
| Mensajería | RabbitMQ: POST `/api/guias` publica un evento; el listener automático del consumidor lo procesa |
| Seguridad | Spring Security OAuth2 Resource Server JWT + Microsoft Entra ID |
| Empaquetado | Docker multistage (`maven:3.9-eclipse-temurin-17` → `eclipse-temurin:17-jre-jammy`, usuario no-root) |

---

## Estructura del proyecto

```
gestion-despacho/
├── Dockerfile                 # Imagen multistage (build + runtime no-root)
├── docker-compose.yml         # Despliegue en EC2 (bind EFS → /app/efs)
├── .env.example               # Plantilla de variables de entorno
├── pom.xml                    # Dependencias y build Maven
├── .github/workflows/
│   └── ci-cd.yml              # Pipeline: test (gate) + build & deploy
└── src/
    ├── main/java/cl/duoc/transportista/despacho/
    │   ├── GestionDespachoApplication.java     # Entry point
    │   ├── config/            # S3Config, OpenApiConfig, DataSeeder
    │   ├── controller/        # GuiaController (API REST /api/guias)
    │   ├── service/           # Lógica: guías, PDF, S3 y EFS
    │   ├── repository/        # GuiaDespachoRepository (Spring Data JPA)
    │   ├── model/             # GuiaDespacho (entidad)
    │   ├── dto/               # GuiaRequest, GuiaResponse, ErrorResponse
    │   └── exception/         # Manejo global de errores
    ├── main/resources/
    │   └── application.properties
    └── test/java/...          # Tests de controller y servicios (JUnit)
```

---

## Cómo ejecutar

El proyecto se compila **dentro de Docker**, así que no necesitas instalar Java ni Maven en tu máquina (y evita el problema de que un JDK reciente del host rompa Spring Boot 3).

### 1. Tests

```bash
docker run --rm \
  -v "$PWD":/app -v "$HOME/.m2":/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-17 \
  mvn -B clean test
```

Todos los tests deben pasar en verde.

### 2. Construir y correr la imagen localmente

La imagen se compila sola desde el `Dockerfile` (no hace falta empaquetar el JAR a mano). Requiere BuildKit:

```bash
DOCKER_BUILDKIT=1 docker build -t gestion-despacho:local .
docker run --rm -p 8080:8080 gestion-despacho:local
```

### 3. Verificar

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Health:** http://localhost:8080/actuator/health

Al arrancar, el seeder carga 3 guías de prueba (idempotente). Las operaciones contra S3 requieren AWS configurado; sin credenciales el PDF igual se escribe en el EFS local, pero subir a S3 devuelve error.

---

## Variables de entorno

Copiar `.env.example` a `.env` y completar (ver `docker-compose.yml`):

| Variable | Default | Descripción |
|---|---|---|
| `HOST_EFS_DIR` | `/mnt/efs/gestion-despacho-consumidor` | Subdirectorio del EFS montado en EC2 y compartido con ambos contenedores |
| `APP_EFS_DIR` | `/app/efs` | Directorio EFS dentro de los contenedores productor y consumidor |
| `AWS_REGION` | `us-east-1` | Región AWS |
| `AWS_S3_BUCKET` | *(requerido)* | Nombre del bucket S3 |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_SESSION_TOKEN` | *(vacío con LabRole)* | Credenciales Learner Lab; pueden omitirse si la EC2 usa LabRole (se resuelven vía IMDS) |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:mem:guias;DB_CLOSE_DELAY=-1` | URL H2 del Productor |
| `CONSUMIDOR_SPRING_DATASOURCE_URL` | `jdbc:h2:file:/app/efs/h2/guias-registro;DB_CLOSE_DELAY=-1` | H2 durable del Consumidor, almacenada en EFS |
| `SPRING_DATASOURCE_DRIVER` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | H2 / `sa` / vacío | Driver y credenciales de base de datos |
| `SPRING_PROFILES_ACTIVE` | *(vacío)* | Perfil activo de Spring |
| `RABBITMQ_USER` / `RABBITMQ_PASS` | *(requeridos)* | Credenciales del broker interno; generar una contraseña con `openssl rand -base64 32` |
| `AZURE_TENANT_ID` | *(requerido)* | ID del tenant de Microsoft Entra ID |
| `AZURE_CLIENT_ID` | *(requerido)* | Application/API Client ID usado como audiencia del access token |
| `AZURE_ISSUER_URI` | *(requerido)* | Emisor: `https://login.microsoftonline.com/{tenant-id}/v2.0` |
| `AZURE_JWK_SET_URI` | *(requerido)* | URL de llaves públicas del tenant para validar la firma JWT |
| `AZURE_ROLES_CLAIM` | `roles` | Claim JWT usado para roles |

> Nunca se hardcodean credenciales AWS en el código ni en `docker-compose.yml`. El `.env` real está en `.gitignore` y no se versiona.

### Seguridad JWT / Microsoft Entra ID

La aplicación **no emite tokens**: funciona como **OAuth2 Resource Server**. Postman debe pedir el access token directamente a Microsoft Entra ID y luego enviarlo como `Authorization: Bearer <token>`. Configura `AZURE_ISSUER_URI`, `AZURE_JWK_SET_URI`, `AZURE_CLIENT_ID` (audiencia) y `AZURE_ROLES_CLAIM` por entorno; no se incluyen valores de tenant ni credenciales en el repositorio.

Claims aceptados para autorización:

- `roles`: `GUIAS_DESCARGA` o `GUIAS_GESTION`.

Mapeo de permisos:

| Rol/claim | Permisos |
|---|---|
| `GUIAS_DESCARGA` | Solo `GET /api/guias/{id}/s3`, sujeto a la identidad y claim de transportista del JWT |
| `GUIAS_GESTION` | Endpoints de gestión de `/api/guias/**`; no autoriza `GET /api/guias/{id}/s3` |

Configuración Postman recomendada para obtener token desde Microsoft Entra ID:

1. Registrar/exponer una API en Microsoft Entra ID y definir un scope, por ejemplo `guia.readwrite`.
2. Crear un App Registration para Postman y habilitar redirect URI `https://oauth.pstmn.io/v1/callback`.
3. En Postman → Authorization → OAuth 2.0:
   - Grant Type: `Authorization Code`.
   - Auth URL: `https://login.microsoftonline.com/{tenant-id}/oauth2/v2.0/authorize`.
   - Access Token URL: `https://login.microsoftonline.com/{tenant-id}/oauth2/v2.0/token`.
   - Client ID/Secret: los del App Registration de Postman.
   - Scope: `openid offline_access api://{api-client-id}/guia.readwrite`.
4. Verificar el token en `https://jwt.ms`: debe traer el `aud` de la API y el claim `roles` con `GUIAS_GESTION` o `GUIAS_DESCARGA`.

Evidencias esperadas para la S6:

- Sin token: `401 Unauthorized`.
- Token válido sin rol suficiente: `403 Forbidden`.
- Token con `GUIAS_DESCARGA` cuya identidad/claim de transportista coincida con la guía: descarga PDF `200 OK`; sin coincidencia, `403 Forbidden`.
- Token con `GUIAS_GESTION`: creación devuelve `202 Accepted` y habilita operaciones de gestión, pero no la descarga S3.

---

## API REST `/api/guias`

| Método | Ruta | Acción |
|---|---|---|
| `POST` | `/api/guias` | Valida y publica el evento en RabbitMQ (202 + `Location`); requiere `Idempotency-Key` entero positivo |
| `POST` | `/api/guias/{id}/s3` | Sube o re-sube la guía a S3 (misma key) |
| `GET` | `/api/guias` | Historial filtrable (`?transportista=&fecha=`) |
| `GET` | `/api/guias/{id}` | Metadata de la guía |
| `GET` | `/api/guias/{id}/s3` | Descarga el PDF: requiere `GUIAS_DESCARGA` y que la identidad/claim de transportista del JWT coincida con la guía |
| `PUT` | `/api/guias/{id}` | Modifica datos, regenera el PDF y sobrescribe la key en S3 |
| `DELETE` | `/api/guias/{id}` | Elimina de S3, EFS y base de datos |

**Layout de la key en S3:** `{yyyyMMdd}/{transportista}/guia{n}.pdf` — por ejemplo `20250315/transportistaX/guia42.pdf`.

### Flujo asíncrono

El flujo de creación es **Productor → RabbitMQ → Listener del Consumidor → BD/EFS/S3**. El Productor no persiste ni genera archivos: tras confirmar la publicación persistente responde `202 Accepted`. El cliente debe enviar `Idempotency-Key` como entero positivo; ese valor identifica la guía y permite reintentos seguros.

El Consumidor recibe el evento mediante listener automático, procesa de forma idempotente, persiste la guía y su H2 durable en el EFS compartido, genera el PDF en ese EFS y lo sube a S3. Las rutas existentes de consulta, modificación, eliminación y S3 son atendidas por el Consumidor una vez terminado el procesamiento. Durante la consistencia eventual, una guía todavía no creada puede responder `404`; en estados `PENDING`/`PROCESSING`, las consultas y operaciones S3 devuelven `202` y las modificaciones o eliminaciones `409`.

Los errores terminales se envían a la DLQ de RabbitMQ mediante NACK sin requeue. Los errores transitorios de red o S3 se reintentan antes de enviarlos a la DLQ. RabbitMQ usa `restart: unless-stopped` para volver a iniciar tras reinicios de EC2 y evitar indisponibilidad del Gateway.

---

## Despliegue (CI/CD en EC2)

El pipeline está en `.github/workflows/ci-cd.yml` y se dispara en cada `push`/`pull_request` a `main`:

1. **`test`** — corre `./mvnw -B test` con Java 17. Si falla, no despliega.
2. **`build-and-deploy`** (solo `push` a `main`) — construye la imagen con `docker/build-push-action`, la publica en Docker Hub (`:latest` y `:${sha}`), se conecta por SSH y descarga el `docker-compose.yml` del commit con `curl` autenticado por la URL pública de GitHub; no usa SCP. Luego ejecuta `docker compose pull && up -d`.

En la EC2, el EFS se monta una vez en `/mnt/efs`. El despliegue crea y valida el subdirectorio `HOST_EFS_DIR` (por defecto `/mnt/efs/gestion-despacho-consumidor`) con modo `2770` y propietario/grupo del usuario no-root del Consumidor. `docker-compose.yml` lo enlaza en `APP_EFS_DIR` (por defecto `/app/efs`) para Productor y Consumidor. Allí persisten los archivos H2 y los PDF del Consumidor antes de subirse a S3.

Secrets requeridos en GitHub (*Settings → Secrets and variables → Actions*): `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `USER_SERVER`, `EC2_SSH_KEY`, `RABBITMQ_USER`, `RABBITMQ_PASS`, `AWS_S3_BUCKET`, `AWS_REGION`, `AZURE_TENANT_ID`, `AZURE_CLIENT_ID`, `AZURE_ISSUER_URI`, `AZURE_JWK_SET_URI` y `AZURE_ROLES_CLAIM`. Solo si la EC2 no usa LabRole: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` y `AWS_SESSION_TOKEN`.
