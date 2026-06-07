# gestion-despacho

Sistema de Gestión de Pedidos y Generación de Guías de Despacho para empresa transportista. Cada guía se genera como PDF con OpenPDF, se almacena temporalmente en EFS y se sube automáticamente a S3 en carpetas organizadas por fecha y transportista. Expone un API REST documentada con Swagger y se despliega mediante Docker con CI/CD en EC2.   

## Stack

| Componente | Versión |
|---|---|
| Spring Boot | 3.5.14 |
| Java | 17 (temurin) |
| Base de datos | H2 embebido |
| AWS SDK | v2 (`software.amazon.awssdk:s3`) |
| Generación PDF | OpenPDF (`com.github.librepdf:openpdf`) |
| Documentación API | springdoc-openapi-starter-webmvc-ui |
| Health / métricas | spring-boot-starter-actuator (`/actuator/health`) |
| Build | Maven en imagen `maven:3.9-eclipse-temurin-17` (Docker multistage) |
| Imagen runtime | `eclipse-temurin:17-jre-jammy`, usuario no-root, layered JAR |

---

## Endpoints `/api/guias`

| Método | Ruta | Acción | Códigos HTTP | Criterio |
|---|---|---|---|---|
| `POST` | `/api/guias` | Crea guía y sube PDF automáticamente a EFS → S3 | 201 + Location | C1, C2 |
| `POST` | `/api/guias/{id}/s3` | Sube o re-sube la guía a S3 (misma key) | 200 + key | C2 |
| `GET` | `/api/guias` | Historial filtrado (`?transportista=&fecha=`) | 200 lista JSON | C5 |
| `GET` | `/api/guias/{id}` | Metadata de la guía | 200 / 404 | — |
| `GET` | `/api/guias/{id}/s3` | Descarga PDF (requiere header `X-Transportista`) | 200 PDF / 403 / 404 | C4 |
| `PUT` | `/api/guias/{id}` | Modifica datos, regenera PDF y sobrescribe misma key en S3 | 200 | C3 |
| `DELETE` | `/api/guias/{id}` | Elimina de S3, EFS y base de datos | 204 / 404 | — |

Swagger UI disponible en `http://<host>:8080/swagger-ui.html`.

---

## Layout de key S3

```
{yyyyMMdd}/{transportista}/guia{n}.pdf
```

Ejemplo:

```
20250315/transportistaX/guia42.pdf
```

---

## Variables de entorno

| Variable | Default | Descripción |
|---|---|---|
| `APP_EFS_DIR` | `/app/efs` | Ruta del directorio EFS dentro del contenedor |
| `AWS_REGION` | `us-east-1` | Región AWS |
| `AWS_S3_BUCKET` | *(requerido)* | Nombre del bucket S3 |
| `AWS_ACCESS_KEY_ID` | *(vacío si LabRole)* | Access key (solo fallback sin rol de instancia) |
| `AWS_SECRET_ACCESS_KEY` | *(vacío si LabRole)* | Secret key (solo fallback sin rol de instancia) |
| `AWS_SESSION_TOKEN` | *(vacío si LabRole)* | Session token Learner Lab (solo fallback) |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:mem:guias;DB_CLOSE_DELAY=-1` | URL H2 (usar `jdbc:h2:file:./data/guias` para persistencia) |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | DDL auto de Hibernate |

---

## Variables a setear en GitHub Actions (el colega)

Dónde: en el repo → **Settings → Secrets and variables → Actions → New repository secret**. Crear estos (mismos nombres que en `.env.example`):

| Secret | Ejemplo de valor | ¿Obligatorio? | De dónde sale |
|---|---|---|---|
| `DOCKERHUB_USERNAME` | `dalvaes` | sí | cuenta de Docker Hub |
| `DOCKERHUB_TOKEN` | `dckr_pat_xxx` | sí | Docker Hub → Account settings → Personal access tokens (read/write) |
| `EC2_HOST` | `34.205.57.70` | sí | IP elástica de la EC2 |
| `USER_SERVER` | `ec2-user` | sí | usuario SSH de la instancia |
| `EC2_SSH_KEY` | contenido del `.pem` (multilínea) | sí | par de claves creado con la EC2 |
| `AWS_S3_BUCKET` | `gestion-despacho-bucket` | sí | bucket creado en el Lab |
| `AWS_REGION` | `us-east-1` | sí | región del Lab |
| `AWS_ACCESS_KEY_ID` | `ASIA...` | solo si NO hay LabRole | "AWS Details" del Learner Lab |
| `AWS_SECRET_ACCESS_KEY` | `wJalr...` | solo si NO hay LabRole | "AWS Details" |
| `AWS_SESSION_TOKEN` | `FwoGZ...` | solo si NO hay LabRole | "AWS Details" (caduca ~4h) |

> Si la EC2 tiene el rol **LabRole / LabInstanceProfile**, las 3 credenciales `AWS_*` pueden omitirse (se resuelven solas por IMDS). `AWS_REGION` y `AWS_S3_BUCKET` siempre se necesitan.

Atajo con GitHub CLI (parado en el repo):

```bash
gh secret set DOCKERHUB_USERNAME -b 'dalvaes'
gh secret set DOCKERHUB_TOKEN    -b 'dckr_pat_xxx'
gh secret set EC2_HOST           -b '34.205.57.70'
gh secret set USER_SERVER        -b 'ec2-user'
gh secret set EC2_SSH_KEY        < mi-clave.pem
gh secret set AWS_S3_BUCKET      -b 'gestion-despacho-bucket'
gh secret set AWS_REGION         -b 'us-east-1'
# Solo si NO hay LabRole en la EC2:
gh secret set AWS_ACCESS_KEY_ID     -b 'ASIA...'
gh secret set AWS_SECRET_ACCESS_KEY -b 'wJalr...'
gh secret set AWS_SESSION_TOKEN     -b 'FwoGZ...'
```

> Las credenciales del Learner Lab caducan (~4h) y cambian al reiniciar el Lab → actualizar las `AWS_*` y `EC2_HOST` antes de cada corrida del pipeline. Ver `.env.example` para copiarlas a un `.env` local.

---

## Construcción en CI/CD (guía para el colega)

La imagen se construye con el `Dockerfile` del repo. Es **multistage** y requiere **BuildKit** (por los `--mount=type=cache`). `docker/build-push-action` ya usa BuildKit por defecto; en build local hay que exportar `DOCKER_BUILDKIT=1`.

Etapas del `Dockerfile`:
1. `deps` (`maven:3.9-eclipse-temurin-17`): copia solo `pom.xml` y corre `dependency:go-offline` con cache de `~/.m2` → las dependencias se cachean y no se re-descargan si `pom.xml` no cambia.
2. `build`: agrega `src/` y hace `mvn package -DskipTests` (reusa la cache de `~/.m2`).
3. `extract` (`temurin:17-jdk-jammy`): `java -Djarmode=tools ... extract --layers` → separa el JAR en capas (dependencies / loader / snapshot / application).
4. `runtime` (`temurin:17-jre-jammy`): copia las capas, instala `curl`, crea usuario **no-root** `app`, define `HEALTHCHECK` contra `/actuator/health` y arranca con flags de contenedor de la JVM.

Ventaja para el pipeline: al cambiar solo código de la app, únicamente se re-pushea la capa `application` (~1-3 MB) en vez de ~80-120 MB de dependencias.

### Workflow de GitHub Actions (a crear)

- **Job `test`** (gate): `actions/setup-java@v4` con Java 17 → `mvn -B test` (o `./mvnw -B test`). Si falla, no despliega.
- **Job `build-and-deploy`** (solo `push` a `main`):
  1. `docker/login-action` con `DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN`.
  2. `docker/build-push-action@v6` con `push: true`, tags `:latest` y `:${{ github.sha }}`, y cache `cache-from`/`cache-to: type=gha` para aprovechar la cache de dependencias entre runs.
  3. Copiar `docker-compose.yml` a la EC2 (`scp`) y por `ssh` exportar las variables (`APP_IMAGE`, `AWS_REGION`, `AWS_S3_BUCKET`, `APP_EFS_DIR=/app/efs`, y las `AWS_*` solo si no se usa LabRole) y ejecutar `docker compose pull app && docker compose up -d`.
- **Prerrequisito de infra:** EFS montado en `/mnt/efs` (NFS 2049, misma AZ), bucket S3 creado, EC2 con LabRole + puerto 8080, y los Secrets de la tabla anterior.

---

## Montaje EFS en EC2

Ejecutar una sola vez en la instancia antes de levantar el contenedor:

```bash
sudo mkdir -p /mnt/efs
sudo mount -t nfs4 -o nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2 \
  <efs-dns>:/ /mnt/efs
```

Verificar que el mount está activo:

```bash
mountpoint -q /mnt/efs && echo "EFS montado" || echo "EFS NO montado"
df -h /mnt/efs
```

El `docker-compose.yml` hace el bind al contenedor:

```yaml
volumes:
  - /mnt/efs:/app/efs
```

Esto hace que `/app/efs` dentro del contenedor y `/mnt/efs` en el host sean el mismo directorio (criterio C1). Para verificar la identidad:

```bash
sudo touch /mnt/efs/_probe
docker exec gestion-despacho ls -l /app/efs/_probe
sudo rm /mnt/efs/_probe
```

Para que el mount persista tras reinicio, agregar a `/etc/fstab`:

```
<efs-dns>:/ /mnt/efs nfs4 nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2,_netdev 0 0
```

El Security Group del EFS debe permitir inbound TCP 2049 desde el SG de la EC2, en la misma AZ donde está el mount target.

---

## Nota de seguridad de credenciales

- **Nunca** hardcodear `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` ni `AWS_SESSION_TOKEN` en el codigo, en `application.properties` ni en `docker-compose.yml`.
- El archivo `.env` (valores reales) esta en `.gitignore` y **no se versiona**.
- En produccion (EC2 con LabRole) las credenciales AWS se resuelven automaticamente via IMDS; las variables `AWS_ACCESS_KEY_ID`/`SECRET`/`SESSION_TOKEN` pueden dejarse vacias.
- Verificar ausencia de secretos antes de cada commit:
  ```bash
  git grep -nE "AKIA|aws_secret|SESSION_TOKEN|password=" -- ':!*.example' ':!README.md'
  ```
  El resultado debe estar vacio.

---

## Build y tests en Docker JDK17

El host puede tener JDK 26, que rompe Spring Boot 3. Compilar y testear siempre con:

```bash
docker run --rm \
  -v "$PWD":/app \
  -v "$HOME/.m2":/root/.m2 \
  -w /app \
  maven:3.9-eclipse-temurin-17 \
  mvn -B clean test
```

Todos los tests deben pasar en verde antes de entregar al colega (el workflow del colega usa el mismo gate).

## Construir y correr la imagen localmente

La imagen se compila sola desde el `Dockerfile` (no hace falta empaquetar el JAR a mano). Requiere BuildKit:

```bash
DOCKER_BUILDKIT=1 docker build -t gestion-despacho:local .
docker run --rm -p 8080:8080 gestion-despacho:local
```

Luego: Swagger en `http://localhost:8080/swagger-ui.html` y health en `http://localhost:8080/actuator/health`. El seeder carga 3 guías de prueba (idempotente). Las operaciones contra S3 requieren AWS configurado; sin credenciales, crear/subir devuelve 5xx pero el PDF igual se escribe en el EFS configurado.
