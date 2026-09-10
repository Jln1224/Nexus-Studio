package dao;

import database.DatabaseManager;
import model.Garantia;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GarantiaDAO {

    public void inserir(Garantia garantia) {
        String sql = "INSERT INTO GARANTIA (componente_id, tipo, data_inicio, data_fim, status, notas) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, garantia.getComponenteId());
            stmt.setString(2, garantia.getTipo());
            stmt.setString(3, garantia.getDataInicio().toString());
            stmt.setString(4, garantia.getDataFim().toString());
            stmt.setString(5, garantia.getStatus());
            stmt.setString(6, garantia.getNotas());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao inserir garantia: " + e.getMessage());
        }
    }

    public List<Garantia> listarTodas() {
        List<Garantia> lista = new ArrayList<>();
        String sql = "SELECT * FROM GARANTIA";
        try (Connection conn = DatabaseManager.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Garantia(
                        rs.getInt("id"),
                        rs.getInt("componente_id"),
                        rs.getString("tipo"),
                        LocalDate.parse(rs.getString("data_inicio")),
                        LocalDate.parse(rs.getString("data_fim")),
                        rs.getString("status"),
                        rs.getString("notas")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar garantias: " + e.getMessage());
        }
        return lista;
    }

    public List<Garantia> listarPorComponente(int componenteId) {
        List<Garantia> lista = new ArrayList<>();
        String sql = "SELECT * FROM GARANTIA WHERE componente_id = ?";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, componenteId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lista.add(new Garantia(
                        rs.getInt("id"),
                        rs.getInt("componente_id"),
                        rs.getString("tipo"),
                        LocalDate.parse(rs.getString("data_inicio")),
                        LocalDate.parse(rs.getString("data_fim")),
                        rs.getString("status"),
                        rs.getString("notas")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar garantias por componente: " + e.getMessage());
        }
        return lista;
    }

    public Garantia buscarPorId(int id) {
        String sql = "SELECT * FROM GARANTIA WHERE id = ?";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Garantia(
                        rs.getInt("id"),
                        rs.getInt("componente_id"),
                        rs.getString("tipo"),
                        LocalDate.parse(rs.getString("data_inicio")),
                        LocalDate.parse(rs.getString("data_fim")),
                        rs.getString("status"),
                        rs.getString("notas")
                );
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar garantia: " + e.getMessage());
        }
        return null;
    }

    public void atualizar(Garantia garantia) {
        String sql = "UPDATE GARANTIA SET tipo = ?, data_inicio = ?, data_fim = ?, status = ?, notas = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, garantia.getTipo());
            stmt.setString(2, garantia.getDataInicio().toString());
            stmt.setString(3, garantia.getDataFim().toString());
            stmt.setString(4, garantia.getStatus());
            stmt.setString(5, garantia.getNotas());
            stmt.setInt(6, garantia.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar garantia: " + e.getMessage());
        }
    }

    public void deletar(int id) {
        String sql = "DELETE FROM GARANTIA WHERE id = ?";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao deletar garantia: " + e.getMessage());
        }
    }

    public void deletarPorComponente(int componenteId) {
        String sql = "DELETE FROM GARANTIA WHERE componente_id = ?";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, componenteId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao deletar garantias do componente: " + e.getMessage());
        }
    }
}
