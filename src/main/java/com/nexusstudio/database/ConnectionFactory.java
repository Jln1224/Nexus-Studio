package com.nexusstudio.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    public Connection getConnection() {
        try {
            // Isso criará o arquivo nexus_db.sqlite na raiz do projeto
            return DriverManager.getConnection("jdbc:sqlite:nexus_db.sqlite");
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao conectar ao banco do Nexus Studio", e);
        }
    }
}