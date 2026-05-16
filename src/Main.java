import dao.CategoriaDAO;
import dao.ComponenteDAO;
import dao.CompatibilidadeDAO;
import dao.EspecificacaoDAO;
import database.DatabaseManager;
import model.Compatibilidade;
import model.Componente;
import model.Especificacao;

import java.util.List;
import java.util.Scanner;

public class Main {
    static Scanner scanner = new Scanner(System.in);
    static ComponenteDAO componenteDAO = new ComponenteDAO();
    static CategoriaDAO categoriaDAO = new CategoriaDAO();
    static EspecificacaoDAO especDAO = new EspecificacaoDAO();
    static CompatibilidadeDAO compatDAO = new CompatibilidadeDAO();

    public static void main(String[] args) {
        DatabaseManager.inicializarBanco();

        int opcao = -1;
        while (opcao != 0) {
            System.out.println("""
                
                ╔══════════════════════════════════╗
                ║      NEXUS STUDIO — MENU         ║
                ╠══════════════════════════════════╣
                ║ 1.  Listar componentes           ║
                ║ 2.  Cadastrar componente         ║
                ║ 3.  Buscar componente por ID     ║
                ║ 4.  Atualizar componente         ║
                ║ 5.  Deletar componente           ║
                ║ 6.  Listar categorias            ║
                ║ 7.  Adicionar especificação      ║
                ║ 8.  Ver specs de um componente   ║
                ║ 9.  Adicionar compatibilidade    ║
                ║ 10. Buscar por socket            ║
                ║ 11. Filtrar por faixa de preço   ║
                ║ 12. Filtrar por TDP máximo       ║
                ║ 13. Buscar por fabricante        ║
                ║ 14. Listar ordenado por preço    ║
                ║ 0.  Sair                         ║
                ╚══════════════════════════════════╝
                Escolha:\s""");

            opcao = Integer.parseInt(scanner.nextLine());

            switch (opcao) {
                case 1  -> listarComponentes();
                case 2  -> cadastrarComponente();
                case 3  -> buscarPorId();
                case 4  -> atualizarComponente();
                case 5  -> deletarComponente();
                case 6  -> listarCategorias();
                case 7  -> adicionarSpec();
                case 8  -> verSpecs();
                case 9  -> adicionarCompatibilidade();
                case 10 -> buscarPorSocket();
                case 11 -> filtrarPorPreco();
                case 12 -> filtrarPorTdp();
                case 13 -> buscarPorFabricante();
                case 14 -> listarPorPreco();
                case 0  -> System.out.println("Até logo!");
                default -> System.out.println("Opção inválida.");
            }
        }
    }

    static void listarComponentes() {
        List<Componente> lista = componenteDAO.listarTodos();
        if (lista.isEmpty()) { System.out.println("Nenhum componente cadastrado."); return; }
        System.out.println("\n=== Componentes ===");
        lista.forEach(System.out::println);
    }

    static void cadastrarComponente() {
        System.out.println("\n=== Novo Componente ===");
        categoriaDAO.listarTodas().forEach(System.out::println);
        System.out.print("ID da categoria: ");
        int catId = Integer.parseInt(scanner.nextLine());
        System.out.print("Nome: ");       String nome = scanner.nextLine();
        System.out.print("Fabricante: "); String fab  = scanner.nextLine();
        System.out.print("Modelo: ");     String mod  = scanner.nextLine();
        System.out.print("Preço: ");
        String precoStr = scanner.nextLine();
        double preco = precoStr.isEmpty() ? 0.0 : Double.parseDouble(precoStr);
        System.out.print("TDP (W): ");
        String tdpStr = scanner.nextLine();
        double tdp = tdpStr.isEmpty() ? 0.0 : Double.parseDouble(tdpStr);
        componenteDAO.inserir(new Componente(catId, nome, fab, mod, preco, tdp));
    }

    static void buscarPorId() {
        System.out.print("ID do componente: ");
        int id = Integer.parseInt(scanner.nextLine());
        Componente c = componenteDAO.buscarPorId(id);
        if (c == null) { System.out.println("Não encontrado."); return; }
        System.out.println(c);
        List<Especificacao> specs = especDAO.listarPorComponente(id);
        if (!specs.isEmpty()) {
            System.out.println("  Especificações:");
            specs.forEach(System.out::println);
        }
    }

    static void atualizarComponente() {
        System.out.print("ID do componente a atualizar: ");
        int id = Integer.parseInt(scanner.nextLine());
        Componente c = componenteDAO.buscarPorId(id);
        if (c == null) { System.out.println("Não encontrado."); return; }
        System.out.println("Atual: " + c);
        System.out.print("Novo modelo (Enter para manter): ");  String mod = scanner.nextLine();
        System.out.print("Novo preço (Enter para manter): ");   String precoStr = scanner.nextLine();
        System.out.print("Novo TDP (Enter para manter): ");     String tdpStr = scanner.nextLine();
        componenteDAO.atualizar(new Componente(
                id, c.getCategoriaId(), c.getNome(), c.getFabricante(),
                mod.isEmpty()      ? c.getModelo()   : mod,
                precoStr.isEmpty() ? c.getPreco()    : Double.parseDouble(precoStr),
                tdpStr.isEmpty()   ? c.getTdpWatts() : Double.parseDouble(tdpStr)
        ));
    }

    static void deletarComponente() {
        System.out.print("ID do componente a deletar: ");
        int id = Integer.parseInt(scanner.nextLine());
        componenteDAO.deletar(id);
    }

    static void listarCategorias() {
        System.out.println("\n=== Categorias ===");
        categoriaDAO.listarTodas().forEach(System.out::println);
    }

    static void adicionarSpec() {
        System.out.println("\n=== Adicionar Especificação ===");
        listarComponentes();
        System.out.print("ID do componente: ");
        int compId = Integer.parseInt(scanner.nextLine());
        Componente c = componenteDAO.buscarPorId(compId);
        if (c == null) { System.out.println("Componente não encontrado."); return; }

        boolean continuar = true;
        while (continuar) {
            System.out.print("Chave (ex: clock_speed): ");  String chave = scanner.nextLine();
            System.out.print("Valor (ex: 3.6): ");          String valor = scanner.nextLine();
            System.out.print("Unidade (ex: GHz, deixe vazio se não tiver): "); String unidade = scanner.nextLine();
            especDAO.inserir(new Especificacao(compId, chave, valor, unidade));
            System.out.print("Adicionar mais uma spec? (s/n): ");
            continuar = scanner.nextLine().equalsIgnoreCase("s");
        }
    }

    static void verSpecs() {
        System.out.print("ID do componente: ");
        int id = Integer.parseInt(scanner.nextLine());
        Componente c = componenteDAO.buscarPorId(id);
        if (c == null) { System.out.println("Não encontrado."); return; }
        System.out.println("\n" + c);
        List<Especificacao> specs = especDAO.listarPorComponente(id);
        if (specs.isEmpty()) {
            System.out.println("  Nenhuma especificação cadastrada.");
        } else {
            System.out.println("  Especificações:");
            specs.forEach(System.out::println);
        }
    }

    static void adicionarCompatibilidade() {
        System.out.println("\n=== Adicionar Compatibilidade ===");
        listarComponentes();
        System.out.print("ID do componente: ");
        int compId = Integer.parseInt(scanner.nextLine());
        Componente c = componenteDAO.buscarPorId(compId);
        if (c == null) { System.out.println("Componente não encontrado."); return; }

        boolean continuar = true;
        while (continuar) {
            System.out.print("Socket (ex: AM4, LGA1700, PCIe 4.0): "); String socket = scanner.nextLine();
            System.out.print("Padrão (ex: DDR5, ATX, deixe vazio): ");  String padrao = scanner.nextLine();
            compatDAO.inserir(new Compatibilidade(compId, socket, padrao));
            System.out.print("Adicionar mais? (s/n): ");
            continuar = scanner.nextLine().equalsIgnoreCase("s");
        }
    }

    static void buscarPorSocket() {
        System.out.print("Digite o socket (ex: AM4): ");
        String socket = scanner.nextLine();
        List<Compatibilidade> lista = compatDAO.buscarPorSocket(socket);
        if (lista.isEmpty()) {
            System.out.println("Nenhum componente compatível com " + socket + ".");
            return;
        }
        System.out.println("\n=== Componentes com socket " + socket + " ===");
        for (Compatibilidade comp : lista) {
            Componente c = componenteDAO.buscarPorId(comp.getComponenteId());
            if (c != null) System.out.println(c + "\n" + comp);
        }
    }

    static void filtrarPorPreco() {
        System.out.print("Preço mínimo: ");
        double min = Double.parseDouble(scanner.nextLine());
        System.out.print("Preço máximo: ");
        double max = Double.parseDouble(scanner.nextLine());
        List<Componente> lista = componenteDAO.filtrarPorPreco(min, max);
        if (lista.isEmpty()) {
            System.out.println("Nenhum componente nessa faixa de preço.");
            return;
        }
        System.out.println("\n=== Componentes entre R$" + min + " e R$" + max + " ===");
        lista.forEach(System.out::println);
    }

    static void filtrarPorTdp() {
        System.out.print("TDP máximo (W): ");
        double tdpMax = Double.parseDouble(scanner.nextLine());
        List<Componente> lista = componenteDAO.filtrarPorTdpMaximo(tdpMax);
        if (lista.isEmpty()) {
            System.out.println("Nenhum componente com TDP até " + tdpMax + "W.");
            return;
        }
        System.out.println("\n=== Componentes com TDP até " + tdpMax + "W ===");
        lista.forEach(System.out::println);
    }

    static void buscarPorFabricante() {
        System.out.print("Fabricante (ex: AMD, Intel, NVIDIA): ");
        String fabricante = scanner.nextLine();
        List<Componente> lista = componenteDAO.buscarPorFabricante(fabricante);
        if (lista.isEmpty()) {
            System.out.println("Nenhum componente do fabricante " + fabricante + ".");
            return;
        }
        System.out.println("\n=== Componentes da " + fabricante + " ===");
        lista.forEach(System.out::println);
    }

    static void listarPorPreco() {
        List<Componente> lista = componenteDAO.listarOrdenadosPorPreco();
        if (lista.isEmpty()) { System.out.println("Nenhum componente cadastrado."); return; }
        System.out.println("\n=== Componentes ordenados por preço ===");
        lista.forEach(System.out::println);
    }
}