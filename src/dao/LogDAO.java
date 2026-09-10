package dao;

import database.DatabaseManager;
import model.LogEntry;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LogDAO {
    public void registrar(String usuario, String acao, String entidade, String detalhes) {
        try (Connection c = DatabaseManager.conectar();
             PreparedStatement p = c.prepareStatement("INSERT INTO LOG(usuario,acao,entidade,detalhes,data_hora) VALUES(?,?,?,?,?)")) {
            p.setString(1, usuario); p.setString(2, acao); p.setString(3, entidade);
            p.setString(4, detalhes); p.setString(5, LocalDateTime.now().toString()); p.executeUpdate();
        } catch (SQLException ignored) {}
    }
    public List<LogEntry> listar(String filtro) {
        List<LogEntry> out = new ArrayList<>();
        String sql = "SELECT * FROM LOG WHERE (?='' OR usuario LIKE ? OR acao LIKE ? OR entidade LIKE ? OR detalhes LIKE ?) ORDER BY data_hora DESC";
        try (Connection c = DatabaseManager.conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            String f = filtro == null ? "" : filtro.trim();
            p.setString(1, f); p.setString(2, "%"+f+"%"); p.setString(3, "%"+f+"%");
            p.setString(4, "%"+f+"%"); p.setString(5, "%"+f+"%");
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) out.add(new LogEntry(r.getInt("id"), r.getString("usuario"), r.getString("acao"),
                        r.getString("entidade"), r.getString("detalhes"), LocalDateTime.parse(r.getString("data_hora"))));
            }
        } catch (SQLException | RuntimeException ignored) {}
        return out;
    }
}
