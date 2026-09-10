package model;

import java.time.LocalDate;

public class Garantia {
    private int id;
    private int componenteId;
    private String tipo;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String status;
    private String notas;

    public Garantia(int componenteId, String tipo, LocalDate dataInicio, LocalDate dataFim, String status, String notas) {
        this.componenteId = componenteId;
        this.tipo = tipo;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.status = status;
        this.notas = notas;
    }

    public Garantia(int id, int componenteId, String tipo, LocalDate dataInicio, LocalDate dataFim, String status, String notas) {
        this.id = id;
        this.componenteId = componenteId;
        this.tipo = tipo;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.status = status;
        this.notas = notas;
    }

    public int getId() { return id; }
    public int getComponenteId() { return componenteId; }
    public String getTipo() { return tipo; }
    public LocalDate getDataInicio() { return dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public String getStatus() { return status; }
    public String getNotas() { return notas; }
    public LocalDate getDataCompra() { return dataInicio; }
    public int getPrazoMeses() { return (int) java.time.temporal.ChronoUnit.MONTHS.between(dataInicio, dataFim); }
    public LocalDate getDataVencimento() { return dataFim; }
    public String getFornecedor() { return tipo; }
    public String getNumeroNota() { return notas; }

    @Override
    public String toString() {
        return String.format("[%d] %s - %s (até %s) - %s", id, tipo, dataInicio, dataFim, status);
    }
}
