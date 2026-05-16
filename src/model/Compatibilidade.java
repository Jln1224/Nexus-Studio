package model;

public class Compatibilidade {
    private int id;
    private int componenteId;
    private String socketTipo;
    private String padrao;

    public Compatibilidade(int componenteId, String socketTipo, String padrao) {
        this.componenteId = componenteId;
        this.socketTipo = socketTipo;
        this.padrao = padrao;
    }

    public Compatibilidade(int id, int componenteId, String socketTipo, String padrao) {
        this.id = id;
        this.componenteId = componenteId;
        this.socketTipo = socketTipo;
        this.padrao = padrao;
    }

    public int getId()            { return id; }
    public int getComponenteId()  { return componenteId; }
    public String getSocketTipo() { return socketTipo; }
    public String getPadrao()     { return padrao; }

    @Override
    public String toString() {
        String p = (padrao != null && !padrao.isEmpty()) ? " | padrão: " + padrao : "";
        return "  socket: " + socketTipo + p;
    }
}