package model;

public record HistoricoPreco(int id, int componenteId, double preco, String dataRegistro) {
    public HistoricoPreco(int componenteId, double preco) {
        this(0, componenteId, preco, null);
    }

    public int getId() { return id; }
    public int getComponenteId() { return componenteId; }
    public double getPreco() { return preco; }
    public String getDataRegistro() { return dataRegistro; }
}
