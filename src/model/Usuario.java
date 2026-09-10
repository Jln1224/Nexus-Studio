package model;

/** Usuário da aplicação e seu nível de acesso. */
public record Usuario(int id, String username, String nome, String senha, String nivel, boolean ativo) {
    public boolean isAdmin() { return "ADMIN".equalsIgnoreCase(nivel); }
    public boolean isOperador() { return isAdmin() || "OPERADOR".equalsIgnoreCase(nivel); }
}
