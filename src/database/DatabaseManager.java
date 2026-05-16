package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String URL = "jdbc:sqlite:nexus_studio.db";

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void inicializarBanco() {
        String sqlCategoria = """
            CREATE TABLE IF NOT EXISTS CATEGORIA (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                nome        TEXT NOT NULL UNIQUE,
                descricao   TEXT
            );
        """;

        String sqlComponente = """
            CREATE TABLE IF NOT EXISTS COMPONENTE (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                categoria_id INTEGER NOT NULL,
                nome         TEXT NOT NULL,
                fabricante   TEXT,
                modelo       TEXT,
                preco        REAL,
                tdp_watts    REAL,
                FOREIGN KEY (categoria_id) REFERENCES CATEGORIA(id)
            );
        """;

        String sqlEspecificacao = """
            CREATE TABLE IF NOT EXISTS ESPECIFICACAO (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                componente_id  INTEGER NOT NULL,
                chave          TEXT NOT NULL,
                valor          TEXT NOT NULL,
                unidade        TEXT,
                FOREIGN KEY (componente_id) REFERENCES COMPONENTE(id)
            );
        """;

        String sqlCompatibilidade = """
            CREATE TABLE IF NOT EXISTS COMPATIBILIDADE (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                componente_id  INTEGER NOT NULL,
                socket_tipo    TEXT NOT NULL,
                padrao         TEXT,
                FOREIGN KEY (componente_id) REFERENCES COMPONENTE(id)
            );
        """;

        try (Connection conn = conectar(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlCategoria);
            stmt.execute(sqlComponente);
            stmt.execute(sqlEspecificacao);
            stmt.execute(sqlCompatibilidade);
            System.out.println("Banco inicializado com sucesso!");
        } catch (SQLException e) {
            System.err.println("Erro ao inicializar banco: " + e.getMessage());
        }
    }
}