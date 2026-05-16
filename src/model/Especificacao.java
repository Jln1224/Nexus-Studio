package model;

public class Especificacao {
    private int id;
    private int componenteId;
    private String chave;
    private String valor;
    private String unidade;

    public Especificacao(int componenteId, String chave, String valor, String unidade) {
        this.componenteId = componenteId;
        this.chave = chave;
        this.valor = valor;
        this.unidade = unidade;
    }

    public Especificacao(int id, int componenteId, String chave, String valor, String unidade) {
        this.id = id;
        this.componenteId = componenteId;
        this.chave = chave;
        this.valor = valor;
        this.unidade = unidade;
    }

    public int getId()           { return id; }
    public int getComponenteId() { return componenteId; }
    public String getChave()     { return chave; }
    public String getValor()     { return valor; }
    public String getUnidade()   { return unidade; }

    @Override
    public String toString() {
        String u = (unidade != null && !unidade.isEmpty()) ? " " + unidade : "";
        return "  " + chave + ": " + valor + u;
    }
}