package dao;

import database.DatabaseManager;
import model.Componente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ComponenteDAO {

    public void inserir(Componente componente) {
        String sql = """
            INSERT INTO COMPONENTE (categoria_id, nome, fabricante, modelo, preco, tdp_watts)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, componente.getCategoriaId());
            stmt.setString(2, componente.getNome());
            stmt.setString(3, componente.getFabricante());
            stmt.setString(4, componente.getModelo());
            stmt.setDouble(5, componente.getPreco());
            stmt.setDouble(6, componente.getTdpWatts());
            stmt.executeUpdate();
            System.out.println("Componente inserido: " + componente.getModelo());

        } catch (SQLException e) {
            System.err.println("Erro ao inserir componente: " + e.getMessage());
        }
    }

    public List<Componente> listarTodos() {
        List<Componente> lista = new ArrayList<>();
        String sql = "SELECT * FROM COMPONENTE";
        try (Connection conn = DatabaseManager.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(new Componente(
                        rs.getInt("id"),
                        rs.getInt("categoria_id"),
                        rs.getString("nome"),
                        rs.getString("fabricante"),
                        rs.getString("modelo"),
                        rs.getDouble("preco"),
                        rs.getDouble("tdp_watts")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar componentes: " + e.getMessage());
        }
        return lista;
    }

    public List<Componente> listarPorCategoria(int categoriaId) {
        List<Componente> lista = new ArrayList<>();
        String sql = "SELECT * FROM COMPONENTE WHERE categoria_id = ?";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, categoriaId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lista.add(new Componente(
                        rs.getInt("id"),
                        rs.getInt("categoria_id"),
                        rs.getString("nome"),
                        rs.getString("fabricante"),
                        rs.getString("modelo"),
                        rs.getDouble("preco"),
                        rs.getDouble("tdp_watts")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar por categoria: " + e.getMessage());
        }
        return lista;
    }

    public Componente buscarPorId(int id) {
        String sql = "SELECT * FROM COMPONENTE WHERE id = ?";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Componente(
                        rs.getInt("id"),
                        rs.getInt("categoria_id"),
                        rs.getString("nome"),
                        rs.getString("fabricante"),
                        rs.getString("modelo"),
                        rs.getDouble("preco"),
                        rs.getDouble("tdp_watts")
                );
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar componente: " + e.getMessage());
        }
        return null;
    }

    public void atualizar(Componente componente) {
        String sql = """
            UPDATE COMPONENTE
            SET nome=?, fabricante=?, modelo=?, preco=?, tdp_watts=?
            WHERE id=?
        """;
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, componente.getNome());
            stmt.setString(2, componente.getFabricante());
            stmt.setString(3, componente.getModelo());
            stmt.setDouble(4, componente.getPreco());
            stmt.setDouble(5, componente.getTdpWatts());
            stmt.setInt(6, componente.getId());
            stmt.executeUpdate();
            System.out.println("Componente atualizado: " + componente.getModelo());

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar: " + e.getMessage());
        }
    }

    public void deletar(int id) {
        String sql = "DELETE FROM COMPONENTE WHERE id = ?";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
            System.out.println("Componente deletado (id=" + id + ")");

        } catch (SQLException e) {
            System.err.println("Erro ao deletar: " + e.getMessage());
        }
    }
    public List<Componente> filtrarPorPreco(double precoMin, double precoMax) {
        List<Componente> lista = new ArrayList<>();
        String sql = "SELECT * FROM COMPONENTE WHERE preco BETWEEN ? AND ?";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, precoMin);
            stmt.setDouble(2, precoMax);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lista.add(new Componente(
                        rs.getInt("id"),
                        rs.getInt("categoria_id"),
                        rs.getString("nome"),
                        rs.getString("fabricante"),
                        rs.getString("modelo"),
                        rs.getDouble("preco"),
                        rs.getDouble("tdp_watts")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao filtrar por preco: " + e.getMessage());
        }
        return lista;
    }

    public List<Componente> filtrarPorTdpMaximo(double tdpMax) {
        List<Componente> lista = new ArrayList<>();
        String sql = "SELECT * FROM COMPONENTE WHERE tdp_watts <= ? ORDER BY tdp_watts ASC";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, tdpMax);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lista.add(new Componente(
                        rs.getInt("id"),
                        rs.getInt("categoria_id"),
                        rs.getString("nome"),
                        rs.getString("fabricante"),
                        rs.getString("modelo"),
                        rs.getDouble("preco"),
                        rs.getDouble("tdp_watts")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao filtrar por TDP: " + e.getMessage());
        }
        return lista;
    }

    public List<Componente> buscarPorFabricante(String fabricante) {
        List<Componente> lista = new ArrayList<>();
        String sql = "SELECT * FROM COMPONENTE WHERE fabricante = ?";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fabricante);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lista.add(new Componente(
                        rs.getInt("id"),
                        rs.getInt("categoria_id"),
                        rs.getString("nome"),
                        rs.getString("fabricante"),
                        rs.getString("modelo"),
                        rs.getDouble("preco"),
                        rs.getDouble("tdp_watts")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar por fabricante: " + e.getMessage());
        }
        return lista;
    }

    public List<Componente> listarOrdenadosPorPreco() {
        List<Componente> lista = new ArrayList<>();
        String sql = "SELECT * FROM COMPONENTE ORDER BY preco ASC";
        try (Connection conn = DatabaseManager.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(new Componente(
                        rs.getInt("id"),
                        rs.getInt("categoria_id"),
                        rs.getString("nome"),
                        rs.getString("fabricante"),
                        rs.getString("modelo"),
                        rs.getDouble("preco"),
                        rs.getDouble("tdp_watts")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao ordenar por preco: " + e.getMessage());
        }
        return lista;
    }
}