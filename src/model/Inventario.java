package model;

public record Inventario(int id, int componenteId, int quantidade, String localizacao) {
    public Inventario(int componenteId, int quantidade, String localizacao) {
        this(0, componenteId, quantidade, localizacao);
    }

    public int getId() { return id; }
    public int getComponenteId() { return componenteId; }
    public int getQuantidade() { return quantidade; }
    public String getLocalizacao() { return localizacao; }
}
