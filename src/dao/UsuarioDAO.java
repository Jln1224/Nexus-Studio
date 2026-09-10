package dao;

import database.DatabaseManager;
import model.Usuario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {
    public Usuario autenticar(String username, String senha) {
        String sql = "SELECT * FROM USUARIO WHERE username=? AND (password=? OR senha=?) AND ativo=1";
        try (Connection c = DatabaseManager.conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, username);
            p.setString(2, senha);
            p.setString(3, senha);
            try (ResultSet r = p.executeQuery()) { return r.next() ? map(r) : null; }
        } catch (SQLException e) { return null; }
    }
    public List<Usuario> listarTodos() {
        List<Usuario> out = new ArrayList<>();
        try (Connection c = DatabaseManager.conectar();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT * FROM USUARIO ORDER BY username")) {
            while (r.next()) out.add(map(r));
        } catch (SQLException ignored) {}
        return out;
    }
    public void salvar(Usuario u) {
        String sql = u.id() == 0
                ? "INSERT INTO USUARIO(username,nome,password,senha,nivel,ativo) VALUES(?,?,?,?,?,?)"
                : "UPDATE USUARIO SET username=?,nome=?,password=?,senha=?,nivel=?,ativo=? WHERE id=?";
        try (Connection c = DatabaseManager.conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, u.username()); p.setString(2, u.nome()); p.setString(3, u.senha()); p.setString(4, u.senha());
            p.setString(5, u.nivel()); p.setBoolean(6, u.ativo());
            if (u.id() != 0) p.setInt(7, u.id());
            p.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException("Não foi possível salvar usuário", e); }
    }
    public void excluir(int id) {
        try (Connection c = DatabaseManager.conectar(); PreparedStatement p = c.prepareStatement("DELETE FROM USUARIO WHERE id=?")) {
            p.setInt(1, id); p.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException("Não foi possível excluir usuário", e); }
    }
    private Usuario map(ResultSet r) throws SQLException {
        return new Usuario(r.getInt("id"), r.getString("username"), r.getString("nome"),
                r.getString("senha"), r.getString("nivel"), r.getBoolean("ativo"));
    }
}
