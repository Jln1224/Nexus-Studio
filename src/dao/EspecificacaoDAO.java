package dao;

import database.DatabaseManager;
import model.Especificacao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EspecificacaoDAO {

    public void inserir(Especificacao spec) {
        String sql = "INSERT INTO ESPECIFICACAO (componente_id, chave, valor, unidade) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, spec.getComponenteId());
            stmt.setString(2, spec.getChave());
            stmt.setString(3, spec.getValor());
            stmt.setString(4, spec.getUnidade());
            stmt.executeUpdate();
            System.out.println("Spec adicionada: " + spec.getChave() + " = " + spec.getValor());

        } catch (SQLException e) {
            System.err.println("Erro ao inserir spec: " + e.getMessage());
        }
    }

    public List<Especificacao> listarPorComponente(int componenteId) {
        List<Especificacao> lista = new ArrayList<>();
        String sql = "SELECT * FROM ESPECIFICACAO WHERE componente_id = ?";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, componenteId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lista.add(new Especificacao(
                        rs.getInt("id"),
                        rs.getInt("componente_id"),
                        rs.getString("chave"),
                        rs.getString("valor"),
                        rs.getString("unidade")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar specs: " + e.getMessage());
        }
        return lista;
    }

    public void deletar(int id) {
        String sql = "DELETE FROM ESPECIFICACAO WHERE id = ?";
        try (Connection conn = DatabaseManager.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
            System.out.println("Spec removida (id=" + id + ")");

        } catch (SQLException e) {
            System.err.println("Erro ao deletar spec: " + e.getMessage());
        }
    }
}