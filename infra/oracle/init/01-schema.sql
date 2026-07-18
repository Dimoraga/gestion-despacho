-- gvenzl executes init scripts as SYS; create application objects in FREEPDB1.despacho.
ALTER SESSION SET CONTAINER = FREEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = despacho;

CREATE SEQUENCE guia_despacho_registro_seq START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE guia_numero_seq START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE solicitud_despacho_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE guia_despacho_registro (
  id NUMBER(19) PRIMARY KEY,
  numero_guia NUMBER(19) NOT NULL,
  transportista VARCHAR2(63 CHAR) NOT NULL,
  fecha DATE NOT NULL,
  destino VARCHAR2(255 CHAR) NOT NULL,
  pedido VARCHAR2(255 CHAR) NOT NULL,
  archivo_key VARCHAR2(512 CHAR) NOT NULL,
  efs_path VARCHAR2(1024 CHAR),
  payload_hash VARCHAR2(64 CHAR) NOT NULL,
  version_evento NUMBER(10) NOT NULL,
  eliminada BOOLEAN DEFAULT FALSE NOT NULL,
  estado VARCHAR2(16 CHAR) NOT NULL,
  fecha_inicio_procesamiento TIMESTAMP WITH TIME ZONE,
  fecha_procesado TIMESTAMP WITH TIME ZONE,
  fase VARCHAR2(16 CHAR) DEFAULT 'PENDING' NOT NULL,
  lease_token VARCHAR2(64 CHAR),
  lease_expira_en TIMESTAMP WITH TIME ZONE,
  fence NUMBER(19) DEFAULT 0 NOT NULL,
  checksum VARCHAR2(64 CHAR),
  ultimo_error VARCHAR2(2000 CHAR),
  retry_count NUMBER(10) DEFAULT 0 NOT NULL,
  retry_at TIMESTAMP WITH TIME ZONE,
  CONSTRAINT uk_registro_numero_guia UNIQUE (numero_guia),
  CONSTRAINT uk_registro_payload_hash UNIQUE (payload_hash),
  CONSTRAINT ck_guia_estado CHECK (estado IN ('PENDING','PROCESSING','RETRY','COMPLETED','FAILED')),
  CONSTRAINT ck_guia_fase CHECK (fase IN ('PENDING','EFS_WRITTEN','S3_WRITTEN','COMPLETED')),
  CONSTRAINT ck_guia_eliminada CHECK (eliminada IN (FALSE, TRUE)),
  CONSTRAINT ck_guia_fence CHECK (fence >= 0)
);
CREATE INDEX ix_guia_lease ON guia_despacho_registro (estado, retry_at, lease_expira_en);
CREATE INDEX ix_guia_transportista_fecha ON guia_despacho_registro (transportista, fecha);

CREATE TABLE solicitud_despacho (
  id NUMBER(19) PRIMARY KEY,
  request_id VARCHAR2(128 CHAR) NOT NULL,
  fingerprint VARCHAR2(64 CHAR) NOT NULL,
  numero_guia NUMBER(19) NOT NULL,
  recibida_en TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT uk_solicitud_request_id UNIQUE (request_id),
  CONSTRAINT fk_solicitud_guia FOREIGN KEY (numero_guia) REFERENCES guia_despacho_registro(numero_guia)
);
CREATE INDEX ix_solicitud_guia ON solicitud_despacho (numero_guia);
