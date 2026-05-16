package model;

public class Categoria {
    private int id;
    private String nome;
    private String descricao;

    public Categoria(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    public Categoria(int id, String nome, String descricao) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
    }

    public int getId()          { return id; }
    public String getNome()     { return nome; }
    public String getDescricao(){ return descricao; }

    @Override
    public String toString() {
        return "[" + id + "] " + nome + " - " + descricao;
    }
}