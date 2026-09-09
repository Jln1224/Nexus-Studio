package database;

import java.sql.*;

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
                image_path   TEXT,
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
        String sqlHistorico = """
            CREATE TABLE IF NOT EXISTS HISTORICO_PRECO (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                componente_id  INTEGER NOT NULL,
                preco          REAL NOT NULL,
                data_alteracao TEXT NOT NULL,
                FOREIGN KEY (componente_id) REFERENCES COMPONENTE(id)
            );
        """;
        String sqlInventario = """
            CREATE TABLE IF NOT EXISTS INVENTARIO (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                componente_id INTEGER NOT NULL UNIQUE,
                quantidade   INTEGER NOT NULL DEFAULT 0,
                localizacao  TEXT,
                data_entrada TEXT NOT NULL,
                FOREIGN KEY (componente_id) REFERENCES COMPONENTE(id)
            );
        """;

        try (Connection conn = conectar(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlCategoria);
            stmt.execute(sqlComponente);
            try {
                stmt.execute("ALTER TABLE COMPONENTE ADD COLUMN image_path TEXT");
            } catch (SQLException ignored) {
                // Banco existente: a coluna já foi criada.
            }
            stmt.execute(sqlEspecificacao);
            stmt.execute(sqlCompatibilidade);
            stmt.execute(sqlHistorico);
            stmt.execute(sqlInventario);
            adicionarColunaSeNecessario(stmt, "HISTORICO_PRECO", "data_alteracao", "TEXT");
            adicionarColunaSeNecessario(stmt, "INVENTARIO", "data_entrada", "TEXT");
            inserirCategoriasPadrao(conn);
            System.out.println("Banco inicializado com sucesso!");
        } catch (SQLException e) {
            System.err.println("Erro ao inicializar banco: " + e.getMessage());
        }

    }

    private static void adicionarColunaSeNecessario(Statement stmt, String tabela,
                                                     String coluna, String tipo) {
        try {
            stmt.execute("ALTER TABLE " + tabela + " ADD COLUMN " + coluna + " " + tipo);
        } catch (SQLException ignorada) {
            // Banco existente: a coluna já está disponível.
        }
    }

    private static void inserirCategoriasPadrao(Connection conn) throws SQLException {
        String sql = "INSERT OR IGNORE INTO CATEGORIA (nome, descricao) VALUES (?, ?)";
        String[][] categorias = {
                {"Processador", "CPUs para computadores"},
                {"Placa-mãe", "Placas-mãe e chipsets"},
                {"Memória RAM", "Módulos de memória"},
                {"Placa de vídeo", "GPUs dedicadas"},
                {"Armazenamento", "SSD e HD"},
                {"Fonte", "Fontes de alimentação"}
        };
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (String[] categoria : categorias) {
                stmt.setString(1, categoria[0]);
                stmt.setString(2, categoria[1]);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
}