package model;

import java.time.LocalDateTime;

public record LogEntry(int id, String usuario, String acao, String entidade,
                       String detalhes, LocalDateTime dataHora) {}
