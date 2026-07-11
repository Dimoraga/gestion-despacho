# gestión-despacho

Sistema de Gestión de Pedidos y Generación de Guías de Despacho para una empresa transportista. Cada guía se genera como **PDF** (OpenPDF), se almacena temporalmente en **EFS** y se sube automáticamente a **Amazon S3** en carpetas organizadas por fecha y transportista. Expone una **API REST** documentada con Swagger, se empaqueta con **Docker** y se despliega en **EC2** mediante un pipeline de **CI/CD** (GitHub Actions).

## Stack

| Componente | Versión / Detalle |
|---|---|
| Spring Boot | 3.5.x |
| Java | 17 (Temurin) |
| Base de datos | H2 embebido |
| AWS SDK | v2 (`software.amazon.awssdk:s3`) |
| Generación PDF | OpenPDF (`com.github.librepdf:openpdf`) |
| Documentación API | springdoc-openapi (Swagger UI) |
| Health / métricas | spring-boot-starter-actuator (`/actuator/health`) |
| Mensajería | RabbitMQ: POST `/api/guias` publica tras commit; consumidor procesa `/api/guias/cola/procesar` |
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
| `APP_EFS_DIR` | `/app/efs` | Directorio EFS dentro del contenedor |
| `AWS_REGION` | `us-east-1` | Región AWS |
| `AWS_S3_BUCKET` | *(requerido)* | Nombre del bucket S3 |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_SESSION_TOKEN` | *(vacío con LabRole)* | Credenciales Learner Lab; pueden omitirse si la EC2 usa LabRole (se resuelven vía IMDS) |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:mem:guias;DB_CLOSE_DELAY=-1` | URL H2 (usar `jdbc:h2:file:./data/guias` para persistencia) |
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

- `roles`: `DESCARGADOR` o `GESTOR`.
- `extension_consultaRole`: también acepta `consulta`, `descarga`, `descargador`, `gestion`, `gestor` o `admin`.
- `scp`: útil si Azure entrega permisos como scopes separados por espacio.

Mapeo de permisos:

| Rol/claim | Permisos |
|---|---|
| `DESCARGADOR` / `consulta` / `descarga` | Solo `GET /api/guias/{id}/s3` |
| `GESTOR` / `gestion` / `admin` | Todos los endpoints `/api/guias/**`, incluida descarga |

Configuración Postman recomendada para obtener token desde Microsoft Entra ID:

1. Registrar/exponer una API en Microsoft Entra ID y definir un scope, por ejemplo `guia.readwrite`.
2. Crear un App Registration para Postman y habilitar redirect URI `https://oauth.pstmn.io/v1/callback`.
3. En Postman → Authorization → OAuth 2.0:
   - Grant Type: `Authorization Code`.
   - Auth URL: `https://login.microsoftonline.com/{tenant-id}/oauth2/v2.0/authorize`.
   - Access Token URL: `https://login.microsoftonline.com/{tenant-id}/oauth2/v2.0/token`.
   - Client ID/Secret: los del App Registration de Postman.
   - Scope: `openid offline_access api://{api-client-id}/guia.readwrite`.
4. Verificar el token en `https://jwt.ms`: debe traer `aud` de la API y algún claim de rol (`roles`, `extension_consultaRole` o `scp`).

Evidencias esperadas para la S6:

- Sin token: `401 Unauthorized`.
- Token válido sin rol suficiente: `403 Forbidden`.
- Token con `DESCARGADOR`/`consulta`: descarga PDF `200 OK`, pero creación/edición `403`.
- Token con `GESTOR`/`gestion`: creación/edición/descarga `200` o `201`.

---

## API REST `/api/guias`

| Método | Ruta | Acción |
|---|---|---|
| `POST` | `/api/guias` | Crea la guía, sube PDF a EFS → S3 y publica RabbitMQ tras commit (201 + Location) |
| `POST` | `/api/guias/{id}/s3` | Sube o re-sube la guía a S3 (misma key) |
| `GET` | `/api/guias` | Historial filtrable (`?transportista=&fecha=`) |
| `GET` | `/api/guias/{id}` | Metadata de la guía |
| `GET` | `/api/guias/{id}/s3` | Descarga el PDF (requiere header `X-Transportista`) |
| `PUT` | `/api/guias/{id}` | Modifica datos, regenera el PDF y sobrescribe la key en S3 |
| `DELETE` | `/api/guias/{id}` | Elimina de S3, EFS y base de datos |

**Layout de la key en S3:** `{yyyyMMdd}/{transportista}/guia{n}.pdf` — por ejemplo `20250315/transportistaX/guia42.pdf`.

---

## Despliegue (CI/CD en EC2)

El pipeline está en `.github/workflows/ci-cd.yml` y se dispara en cada `push`/`pull_request` a `main`:

1. **`test`** — corre `./mvnw -B test` con Java 17. Si falla, no despliega.
2. **`build-and-deploy`** (solo `push` a `main`) — construye la imagen con `docker/build-push-action`, la publica en Docker Hub (`:latest` y `:${sha}`), copia el `docker-compose.yml` a la EC2 por SSH y ejecuta `docker compose pull && up -d`.

En la EC2, el EFS se monta una vez en `/mnt/efs` y el `docker-compose.yml` lo enlaza al contenedor (`/mnt/efs:/app/efs`), de modo que los PDF escritos por la app quedan en el almacenamiento compartido EFS antes de subirse a S3.

Secrets requeridos en GitHub (*Settings → Secrets and variables → Actions*): `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `USER_SERVER`, `EC2_SSH_KEY`, `RABBITMQ_USER`, `RABBITMQ_PASS`, `AWS_S3_BUCKET`, `AWS_REGION`, `AZURE_TENANT_ID`, `AZURE_CLIENT_ID`, `AZURE_ISSUER_URI`, `AZURE_JWK_SET_URI` y `AZURE_ROLES_CLAIM`. Solo si la EC2 no usa LabRole: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` y `AWS_SESSION_TOKEN`.
