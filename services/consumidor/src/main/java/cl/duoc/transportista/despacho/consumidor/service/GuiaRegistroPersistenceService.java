package cl.duoc.transportista.despacho.consumidor.service;

import cl.duoc.transportista.despacho.consumidor.dto.*;
import cl.duoc.transportista.despacho.consumidor.model.*;
import cl.duoc.transportista.despacho.consumidor.repository.GuiaDespachoRegistroRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service public class GuiaRegistroPersistenceService {
  public static final int VERSION_EVENTO = 1;
  private static final DateTimeFormatter KEY_DATE=DateTimeFormatter.ofPattern("yyyyMMdd");
  private final GuiaDespachoRegistroRepository repo;
  GuiaRegistroPersistenceService(GuiaDespachoRegistroRepository repo) { this.repo=repo; }
  @Transactional public GuiaDespachoRegistro preparar(GuiaColaMensaje m) {
    validarEvento(m); String hash=hash(m); Optional<GuiaDespachoRegistro> existing=repo.findByNumeroGuia(m.numeroGuia());
    if (existing.isPresent()) { GuiaDespachoRegistro r=existing.get(); if (!hash.equals(r.getPayloadHash())) throw new PayloadConflictException(); if (!r.isEliminada() && r.getEstado()!=EstadoProcesamiento.COMPLETED) { r.setEstado(EstadoProcesamiento.PROCESSING); r.setFechaInicioProcesamiento(Instant.now()); } return r; }
    GuiaDespachoRegistro r=new GuiaDespachoRegistro(); r.setNumeroGuia(m.numeroGuia()); r.setTransportista(normalizarTransportista(m.transportista())); r.setFecha(m.fecha()); r.setDestino(m.destino()); r.setPedido(m.pedido()); r.setPayloadHash(hash); r.setVersionEvento(m.version()); r.setArchivoKey(key(m)); r.setEstado(EstadoProcesamiento.PENDING); repo.saveAndFlush(r); r.setEstado(EstadoProcesamiento.PROCESSING); r.setFechaInicioProcesamiento(Instant.now()); return r;
  }
  @Transactional public void completar(Long id, String efsPath) { GuiaDespachoRegistro r=repo.findById(id).orElseThrow(); r.setEfsPath(efsPath); r.setEstado(EstadoProcesamiento.COMPLETED); r.setFechaProcesado(Instant.now()); }
  @Transactional public void fallar(Long id) { repo.findById(id).filter(r -> !r.isEliminada()).ifPresent(r -> r.setEstado(EstadoProcesamiento.FAILED)); }
  @Transactional public void recuperarProcesandoAbandonado() { repo.findByEstadoAndFechaInicioProcesamientoBefore(EstadoProcesamiento.PROCESSING, Instant.now().minus(Duration.ofMinutes(10))).forEach(r -> { r.setEstado(EstadoProcesamiento.PENDING); r.setFechaInicioProcesamiento(null); }); }
  @Transactional(readOnly=true) public GuiaDespachoRegistro obtener(Long numero) { GuiaDespachoRegistro r=repo.findByNumeroGuia(numero).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Guia no encontrada todavía")); if(r.isEliminada()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Guia eliminada"); return r; }
  @Transactional(readOnly=true) public List<GuiaDespachoRegistro> historial(String t, LocalDate f) { List<GuiaDespachoRegistro> xs=t!=null&&f!=null?repo.findByTransportistaAndFecha(normalizarTransportista(t),f):t!=null?repo.findByTransportista(normalizarTransportista(t)):f!=null?repo.findByFecha(f):repo.findAll(); return xs.stream().filter(r -> !r.isEliminada()).toList(); }
  @Transactional public GuiaDespachoRegistro actualizar(Long n, GuiaRequest q) { GuiaDespachoRegistro r=operable(n); r.setTransportista(normalizarTransportista(q.transportista())); r.setFecha(q.fecha()); r.setDestino(q.destino()); r.setPedido(q.pedido()); r.setArchivoKey(key(r)); return r; }
  @Transactional public GuiaDespachoRegistro eliminar(Long n) { GuiaDespachoRegistro r=operable(n); r.setEliminada(true); r.setEstado(EstadoProcesamiento.COMPLETED); return r; }
  @Transactional(readOnly=true) public GuiaDespachoRegistro operable(Long n) { GuiaDespachoRegistro r=obtener(n); if(r.getEstado()!=EstadoProcesamiento.COMPLETED) throw new ResponseStatusException(HttpStatus.CONFLICT,"Guia en procesamiento"); return r; }
  public String key(GuiaColaMensaje m) { return m.fecha().format(KEY_DATE)+"/"+normalizarTransportista(m.transportista())+"/guia"+m.numeroGuia()+".pdf"; }
  public String key(GuiaDespachoRegistro r) { return r.getFecha().format(KEY_DATE)+"/"+normalizarTransportista(r.getTransportista())+"/guia"+r.getNumeroGuia()+".pdf"; }
  public String normalizarTransportista(String value) { if(value==null) throw new IllegalArgumentException("Transportista requerido"); String n=Normalizer.normalize(value,Normalizer.Form.NFKD).replaceAll("\\p{M}","").trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("(^-+|-+$)",""); if(!n.matches("[a-z0-9][a-z0-9_-]{0,62}")) throw new IllegalArgumentException("Transportista inválido"); return n; }
  private void validarEvento(GuiaColaMensaje m) { if(m==null||m.version()==null||m.version()!=VERSION_EVENTO||m.numeroGuia()==null||m.numeroGuia()<=0||m.fecha()==null||m.destino()==null||m.pedido()==null) throw new UnsupportedEventException(); }
  private String hash(GuiaColaMensaje m) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((m.version()+"|"+m.numeroGuia()+"|"+normalizarTransportista(m.transportista())+"|"+m.fecha()+"|"+m.destino()+"|"+m.pedido()).getBytes(StandardCharsets.UTF_8))); } catch(Exception e) { throw new IllegalStateException(e); } }
  public static class PayloadConflictException extends RuntimeException { }
  public static class UnsupportedEventException extends RuntimeException { }
}
