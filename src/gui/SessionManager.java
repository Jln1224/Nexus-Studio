package gui;

import model.Usuario;

/** Sessão corrente, mantida em um único ponto para ações e auditoria. */
public final class SessionManager {
    private static Usuario usuario;
    private SessionManager() {}
    public static void iniciar(Usuario u) { usuario = u; }
    public static void encerrar() { usuario = null; }
    public static Usuario atual() { return usuario; }
    public static String nome() { return usuario == null ? "sistema" : usuario.username(); }
    public static boolean admin() { return usuario != null && usuario.isAdmin(); }
}
