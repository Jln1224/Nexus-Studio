package dao;

import database.DatabaseManager;
import model.Inventario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventarioDAO {
    public void inserir(Inventario inventario) {
        String sql = "INSERT INTO INVENTARIO (componente_id, quantidade, localizacao) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, inventario.componenteId());
            stmt.setInt(2, Math.max(0, inventario.quantidade()));
            stmt.setString(3, inventario.localizacao());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao inserir inventário: " + e.getMessage());
        }
    }

    public List<Inventario> listarTodos() {
        List<Inventario> lista = new ArrayList<>();
        String sql = "SELECT id, componente_id, quantidade, localizacao FROM INVENTARIO ORDER BY componente_id";
        try (Connection conn = DatabaseManager.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Inventario(rs.getInt("id"), rs.getInt("componente_id"),
                        rs.getInt("quantidade"), rs.getString("localizacao")));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar inventário: " + e.getMessage());
        }
        return lista;
    }

    public void atualizarQuantidade(int componenteId, int quantidade, String localizacao) {
        String sql = "INSERT INTO INVENTARIO (componente_id, quantidade, localizacao, data_entrada) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT(componente_id) DO UPDATE SET quantidade=excluded.quantidade, localizacao=excluded.localizacao";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, componenteId);
            stmt.setInt(2, Math.max(0, quantidade));
            stmt.setString(3, localizacao);
            stmt.setString(4, java.time.LocalDate.now().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar inventário: " + e.getMessage());
        }
    }

    public void atualizarQuantidade(int componenteId, int quantidade) {
        Inventario atual = buscarPorComponente(componenteId);
        atualizarQuantidade(componenteId, quantidade, atual == null ? null : atual.localizacao());
    }

    public Inventario buscarPorComponente(int componenteId) {
        String sql = "SELECT id, componente_id, quantidade, localizacao FROM INVENTARIO WHERE componente_id=?";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, componenteId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Inventario(rs.getInt("id"), rs.getInt("componente_id"),
                            rs.getInt("quantidade"), rs.getString("localizacao"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar inventário: " + e.getMessage());
        }
        return null;
    }
}
