package database;

import java.sql.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

public class DatabaseManager {

    private static final String URL = "jdbc:sqlite:nexus_studio.db";
    private static final Path DATABASE_FILE = Path.of("nexus_studio.db");

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
        String sqlUsuario = """
            CREATE TABLE IF NOT EXISTS USUARIO (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                nome TEXT NOT NULL,
                password TEXT,
                senha TEXT NOT NULL,
                nivel TEXT NOT NULL DEFAULT 'USUARIO',
                ativo INTEGER NOT NULL DEFAULT 1
            );
        """;
        String sqlGarantia = """
            CREATE TABLE IF NOT EXISTS GARANTIA (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                componente_id INTEGER NOT NULL,
                tipo TEXT NOT NULL,
                data_inicio TEXT NOT NULL,
                data_fim TEXT NOT NULL,
                status TEXT NOT NULL,
                notas TEXT,
                FOREIGN KEY (componente_id) REFERENCES COMPONENTE(id)
            );
        """;
        String sqlLog = """
            CREATE TABLE IF NOT EXISTS LOG (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario TEXT NOT NULL,
                acao TEXT NOT NULL,
                entidade TEXT,
                detalhes TEXT,
                data_hora TEXT NOT NULL
            );
        """;

        try (Connection conn = conectar(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlCategoria);
            stmt.execute(sqlComponente);
            try {
                stmt.execute("ALTER TABLE COMPONENTE ADD COLUMN image_path TEXT");
            } catch (SQLException ignored) {
                // Banco existente: a coluna jÃ¡ foi criada.
            }
            stmt.execute(sqlEspecificacao);
            stmt.execute(sqlCompatibilidade);
            stmt.execute(sqlHistorico);
            stmt.execute(sqlInventario);
            stmt.execute(sqlUsuario);
            stmt.execute(sqlLog);
            stmt.execute(sqlGarantia);
            adicionarColunaSeNecessario(stmt, "USUARIO", "password", "TEXT");
            stmt.execute("UPDATE USUARIO SET password=senha WHERE password IS NULL");
            adicionarColunaSeNecessario(stmt, "HISTORICO_PRECO", "data_alteracao", "TEXT");
            adicionarColunaSeNecessario(stmt, "INVENTARIO", "data_entrada", "TEXT");
            inserirCategoriasPadrao(conn);
            inserirUsuariosPadrao(conn);
            System.out.println("Banco de dados inicializado: " + DATABASE_FILE.toAbsolutePath());
            int componentCount = 0;
            try (Connection checkConn = conectar(); Statement checkStmt = checkConn.createStatement();
                 ResultSet checkRs = checkStmt.executeQuery("SELECT COUNT(*) as count FROM COMPONENTE")) {
                if (checkRs.next()) componentCount = checkRs.getInt("count");
            }
            System.out.println("Loaded " + componentCount + " components from database");
        } catch (SQLException e) {
            System.err.println("Erro ao inicializar banco: " + e.getMessage());
        }
        criarBackup();

    }

    private static void inserirUsuariosPadrao(Connection conn) throws SQLException {
        String sql = "INSERT OR IGNORE INTO USUARIO(username,nome,senha,nivel,ativo) VALUES(?,?,?,?,1)";
        try (PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, "admin"); p.setString(2, "Administrador"); p.setString(3, "nexus123"); p.setString(4, "ADMIN"); p.addBatch();
            p.setString(1, "operador"); p.setString(2, "Operador"); p.setString(3, "op123"); p.setString(4, "OPERADOR"); p.addBatch();
            p.setString(1, "usuario"); p.setString(2, "UsuÃ¡rio"); p.setString(3, "nexus123"); p.setString(4, "USUARIO"); p.addBatch();
            p.executeBatch();
        }
    }

    /** Creates a timestamped backup and keeps only the ten most recent copies. */
    public static void criarBackup() {
        try {
            if (!Files.exists(DATABASE_FILE)) return;
            Path dir = Path.of("backups");
            Files.createDirectories(dir);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Files.copy(DATABASE_FILE, dir.resolve("nexus-studio-" + stamp + ".db"),
                    StandardCopyOption.REPLACE_EXISTING);
            Files.list(dir).filter(p -> p.getFileName().toString().endsWith(".db"))
                    .sorted(Comparator.comparingLong(DatabaseManager::lastModified).reversed())
                    .skip(10).forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {
            // A falha de backup nÃ£o deve impedir a utilizaÃ§Ã£o do catÃ¡logo.
        }
    }

    private static long lastModified(Path p) {
        try { return Files.getLastModifiedTime(p).toMillis(); } catch (IOException e) { return 0; }
    }

    private static void adicionarColunaSeNecessario(Statement stmt, String tabela,
                                                     String coluna, String tipo) {
        try {
            stmt.execute("ALTER TABLE " + tabela + " ADD COLUMN " + coluna + " " + tipo);
        } catch (SQLException ignorada) {
            // Banco existente: a coluna jÃ¡ estÃ¡ disponÃ­vel.
        }
    }

    private static void inserirCategoriasPadrao(Connection conn) throws SQLException {
        String sql = "INSERT OR IGNORE INTO CATEGORIA (nome, descricao) VALUES (?, ?)";
        String[][] categorias = {
                {"Processador", "CPUs para computadores"},
                {"Placa-mÃ£e", "Placas-mÃ£e e chipsets"},
                {"MemÃ³ria RAM", "MÃ³dulos de memÃ³ria"},
                {"Placa de vÃ­deo", "GPUs dedicadas"},
                {"Armazenamento", "SSD e HD"},
                {"Fonte", "Fontes de alimentaÃ§Ã£o"}
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
