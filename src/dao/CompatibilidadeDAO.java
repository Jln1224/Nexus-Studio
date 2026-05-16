package dao;

import database.DatabaseManager;
import model.Compatibilidade;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CompatibilidadeDAO {

    public void inserir(Compatibilidade c) {
        String sql = "INSERT INTO COMPATIBILIDADE (componente_id, socket_tipo, padrao) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, c.getComponenteId());
            stmt.setString(2, c.getSocketTipo());
            stmt.setString(3, c.getPadrao());
            stmt.executeUpdate();
            System.out.println("Compatibilidade adicionada: " + c.getSocketTipo());

        } catch (SQLException e) {
            System.err.println("Erro ao inserir compatibilidade: " + e.getMessage());
        }
    }

    public List<Compatibilidade> listarPorComponente(int componenteId) {
        List<Compatibilidade> lista = new ArrayList<>();
        String sql = "SELECT * FROM COMPATIBILIDADE WHERE componente_id = ?";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, componenteId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lista.add(new Compatibilidade(
                        rs.getInt("id"),
                        rs.getInt("componente_id"),
                        rs.getString("socket_tipo"),
                        rs.getString("padrao")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar compatibilidades: " + e.getMessage());
        }
        return lista;
    }

    public List<Compatibilidade> buscarPorSocket(String socket) {
        List<Compatibilidade> lista = new ArrayList<>();
        String sql = "SELECT * FROM COMPATIBILIDADE WHERE socket_tipo = ?";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, socket);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lista.add(new Compatibilidade(
                        rs.getInt("id"),
                        rs.getInt("componente_id"),
                        rs.getString("socket_tipo"),
                        rs.getString("padrao")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar por socket: " + e.getMessage());
        }
        return lista;
    }

    public void deletar(int id) {
        String sql = "DELETE FROM COMPATIBILIDADE WHERE id = ?";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
            System.out.println("Compatibilidade removida (id=" + id + ")");

        } catch (SQLException e) {
            System.err.println("Erro ao deletar: " + e.getMessage());
        }
    }
}