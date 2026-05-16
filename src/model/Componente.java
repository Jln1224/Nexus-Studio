package model;

public class Componente {
    private int id;
    private int categoriaId;
    private String nome;
    private String fabricante;
    private String modelo;
    private double preco;
    private double tdpWatts;

    public Componente(int categoriaId, String nome, String fabricante, String modelo, double preco, double tdpWatts) {
        this.categoriaId = categoriaId;
        this.nome = nome;
        this.fabricante = fabricante;
        this.modelo = modelo;
        this.preco = preco;
        this.tdpWatts = tdpWatts;
    }

    public Componente(int id, int categoriaId, String nome, String fabricante, String modelo, double preco, double tdpWatts) {
        this.id = id;
        this.categoriaId = categoriaId;
        this.nome = nome;
        this.fabricante = fabricante;
        this.modelo = modelo;
        this.preco = preco;
        this.tdpWatts = tdpWatts;
    }

    public int getId()          { return id; }
    public int getCategoriaId() { return categoriaId; }
    public String getNome()     { return nome; }
    public String getFabricante(){ return fabricante; }
    public String getModelo()   { return modelo; }
    public double getPreco()    { return preco; }
    public double getTdpWatts() { return tdpWatts; }

    @Override
    public String toString() {
        return "[" + id + "] " + fabricante + " " + modelo + " - R$" + preco + " | TDP: " + tdpWatts + "W";
    }
}