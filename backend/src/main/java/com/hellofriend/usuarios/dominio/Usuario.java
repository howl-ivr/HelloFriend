package com.hellofriend.usuarios.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

/**
 * Cuenta de acceso (tabla {@code usuarios_usuario}, V1 y V2). La contraseña se guarda solo como hash. El campo
 * {@code versionSesion} viaja en el token: al sumarle 1 los tokens emitidos antes dejan de valer.
 */
@Entity
@Table(name = "usuarios_usuario")
public class Usuario {

	@Id
	@UuidGenerator
	@Column(name = "id", updatable = false, length = 36)
	private String id;

	@Column(name = "correo", nullable = false, length = 255)
	private String correo;

	@Column(name = "usuario", nullable = false, length = 15)
	private String usuario;

	@Column(name = "contrasena_hash", nullable = false, length = 100)
	private String contrasenaHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "rol", nullable = false, length = 20)
	private Rol rol;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoCuenta estado = EstadoCuenta.ACTIVA;

	/** Desactivación de una cuenta ya aprobada; el flujo de aprobación está en {@code estado}. */
	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	/** Se suma 1 al cerrar sesión o cambiar la contraseña; el token lleva el valor con el que se emitió. */
	@Column(name = "version_sesion", nullable = false)
	private int versionSesion;

	/** SHA-256 del enlace de recuperación vigente (nunca el token en claro); null si no hay ninguno. */
	@Column(name = "token_recuperacion_hash", length = 100)
	private String tokenRecuperacionHash;

	/** Vencimiento del enlace de recuperación, en UTC. */
	@Column(name = "token_recuperacion_expira_en")
	private LocalDateTime tokenRecuperacionExpiraEn;

	/** Fecha de creación en UTC. */
	@Column(name = "creado_en", nullable = false, updatable = false)
	private LocalDateTime creadoEn;

	protected Usuario() {
	}

	public Usuario(String correo, String usuario, String contrasenaHash, Rol rol, LocalDateTime creadoEn) {
		this.correo = correo;
		this.usuario = usuario;
		this.contrasenaHash = contrasenaHash;
		this.rol = rol;
		this.creadoEn = creadoEn;
	}

	/** Cuenta de un estudiante que se registró solo: queda PENDIENTE hasta que el administrador la apruebe. */
	public static Usuario solicitud(String correo, String usuario, String contrasenaHash, LocalDateTime creadoEn) {
		Usuario nueva = new Usuario(correo, usuario, contrasenaHash, Rol.ESTUDIANTE, creadoEn);
		nueva.estado = EstadoCuenta.PENDIENTE;
		return nueva;
	}

	/** Solo una cuenta aprobada y no desactivada puede iniciar sesión o usar un token. */
	public boolean puedeIniciarSesion() {
		return activo && estado == EstadoCuenta.ACTIVA;
	}

	public void aprobar() {
		estado = EstadoCuenta.ACTIVA;
	}

	public void rechazar() {
		estado = EstadoCuenta.RECHAZADA;
	}

	/** Guarda el enlace de recuperación. Pedir otro sobrescribe el anterior, que deja de valer. */
	public void iniciarRecuperacion(String hashToken, LocalDateTime expiraEn) {
		tokenRecuperacionHash = hashToken;
		tokenRecuperacionExpiraEn = expiraEn;
	}

	public boolean recuperacionVigenteEn(LocalDateTime ahora) {
		return tokenRecuperacionHash != null && tokenRecuperacionExpiraEn != null
				&& tokenRecuperacionExpiraEn.isAfter(ahora);
	}

	/** Cambia la contraseña, consume el enlace (un solo uso) y cierra todas las sesiones abiertas. */
	public void restablecerContrasena(String nuevoHash) {
		contrasenaHash = nuevoHash;
		tokenRecuperacionHash = null;
		tokenRecuperacionExpiraEn = null;
		cerrarSesion();
	}

	/** Invalida todos los tokens emitidos hasta ahora. */
	public void cerrarSesion() {
		versionSesion++;
	}

	public void desactivar() {
		activo = false;
		cerrarSesion();
	}

	public void reactivar() {
		activo = true;
	}

	public void asignarRol(Rol rol) {
		this.rol = rol;
		cerrarSesion();
	}

	public String getId() {
		return id;
	}

	public String getCorreo() {
		return correo;
	}

	public String getUsuario() {
		return usuario;
	}

	public String getContrasenaHash() {
		return contrasenaHash;
	}

	public Rol getRol() {
		return rol;
	}

	public EstadoCuenta getEstado() {
		return estado;
	}

	public boolean isActivo() {
		return activo;
	}

	public String getTokenRecuperacionHash() {
		return tokenRecuperacionHash;
	}

	public LocalDateTime getTokenRecuperacionExpiraEn() {
		return tokenRecuperacionExpiraEn;
	}

	public int getVersionSesion() {
		return versionSesion;
	}

	public LocalDateTime getCreadoEn() {
		return creadoEn;
	}

}
