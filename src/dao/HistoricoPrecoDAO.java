package dao;

import database.DatabaseManager;
import model.HistoricoPreco;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HistoricoPrecoDAO {
    public void inserir(HistoricoPreco historico) {
        String sql = "INSERT INTO HISTORICO_PRECO (componente_id, preco, data_alteracao) VALUES (?, ?, COALESCE(?, CURRENT_TIMESTAMP))";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, historico.componenteId());
            stmt.setDouble(2, historico.preco());
            stmt.setString(3, historico.dataRegistro());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao registrar histórico de preço: " + e.getMessage());
        }
    }

    public List<HistoricoPreco> listarPorComponente(int componenteId) {
        List<HistoricoPreco> lista = new ArrayList<>();
        String sql = "SELECT id, componente_id, preco, data_alteracao FROM HISTORICO_PRECO " +
                "WHERE componente_id=? ORDER BY data_alteracao ASC, id ASC";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, componenteId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new HistoricoPreco(rs.getInt("id"), rs.getInt("componente_id"),
                            rs.getDouble("preco"), rs.getString("data_alteracao")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar histórico de preço: " + e.getMessage());
        }
        return lista;
    }
}
