package gui;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import dao.CategoriaDAO;
import dao.ComponenteDAO;
import dao.CompatibilidadeDAO;
import dao.HistoricoPrecoDAO;
import dao.InventarioDAO;
import dao.GarantiaDAO;
import dao.EspecificacaoDAO;
import dao.LogDAO;
import dao.UsuarioDAO;
import database.DatabaseManager;
import model.LogEntry;
import model.Usuario;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.Categoria;
import model.Componente;
import model.Compatibilidade;
import model.HistoricoPreco;
import model.Inventario;
import model.Garantia;
import model.Especificacao;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class NexusStudioApp extends Application {
    private static final String TEAL = "#1D9E75";
    private final ComponenteDAO componenteDAO = new ComponenteDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final CompatibilidadeDAO compatibilidadeDAO = new CompatibilidadeDAO();
    private final HistoricoPrecoDAO historicoDAO = new HistoricoPrecoDAO();
    private final InventarioDAO inventarioDAO = new InventarioDAO();
    private final EspecificacaoDAO especificacaoDAO = new EspecificacaoDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final LogDAO logDAO = new LogDAO();
    private final GarantiaDAO garantiaDAO = new GarantiaDAO();
    private final UndoManager undoManager = new UndoManager();
    private final TableView<Componente> tabela = new TableView<>();
    private final ObservableList<Componente> componentes = FXCollections.observableArrayList();
    private final FilteredList<Componente> componentesVisiveis = new FilteredList<>(componentes);
    private final ObservableList<EstoqueLinha> estoque = FXCollections.observableArrayList();
    private Label statusLabel;
    private TextField buscaComponentes;
    private ComboBox<Categoria> categoriaCombo;
    private ImageView imagemDetalhe;
    private Label imagemPlaceholder;
    private Label detalheNome;
    private Label detalheDados;
    private TextField novoPreco;
    private ComboBox<Componente> historicoCombo;
    private LineChart<Number, Number> historicoChart;
    private Label estoqueResumo;
    private Label dashboardTotal;
    private Label dashboardCategorias;
    private Label dashboardMenorPreco;
    private Label dashboardMaiorTdp;
    private BarChart<String, Number> componentesPorCategoriaChart;
    private BarChart<String, Number> tdpMedioPorCategoriaChart;
    private PieChart precoPorFabricanteChart;
    private TabPane abas;
    private Pagination paginacaoComponentes;
    private ComboBox<Integer> tamanhoPagina;
    private ToastManager toasts;
    private Usuario usuarioAtual;
    private Scene cenaPrincipal;
    private boolean temaEscuro;

    @Override
    public void start(Stage stage) {
        DatabaseManager.inicializarBanco();
        undoManager.clear();
        mostrarSplash(stage);
        if (!mostrarLogin(stage)) {
            Platform.exit();
            return;
        }
        configurarTabela(tabela, "Nenhum componente cadastrado.");
        Tab dashboard = criarAbaDashboard();
        Tab componentesTab = criarAbaComponentes();
        Tab cadastroTab = criarAbaCadastro();
        Tab relatoriosTab = criarAbaRelatorios();
        Tab compatibilidadeTab = criarAbaCompatibilidade();
        Tab historicoTab = criarAbaHistorico();
        Tab estoqueTab = criarAbaEstoque();
        Tab monteSeuPcTab = criarAbaMonteSeuPc();
        Tab compararTab = criarAbaComparar();
        Tab garantiaTab = criarAbaGarantia();
        abas = new TabPane(
                dashboard, componentesTab, cadastroTab, relatoriosTab, compatibilidadeTab, historicoTab,
                estoqueTab, monteSeuPcTab, compararTab, garantiaTab
        );
        configurarAtualizacaoAba(componentesTab, () -> atualizarTabela(componenteDAO.listarTodos()));
        configurarAtualizacaoAba(compatibilidadeTab, () -> atualizarTabela(componenteDAO.listarTodos()));
        configurarAtualizacaoAba(historicoTab, () -> atualizarTabela(componenteDAO.listarTodos()));
        configurarAtualizacaoAba(estoqueTab, () -> { atualizarTabela(componenteDAO.listarTodos()); atualizarEstoque(); });
        configurarAtualizacaoAba(monteSeuPcTab, () -> atualizarTabela(componenteDAO.listarTodos()));
        if (SessionManager.admin()) {
            abas.getTabs().addAll(criarAbaUsuarios(), criarAbaLogs(), criarAbaConfiguracoes());
        }
        if (!SessionManager.atual().isOperador()) {
            abas.getTabs().stream().filter(t -> Set.of("Cadastrar", "Estoque").contains(t.getText()))
                    .forEach(t -> t.setDisable(true));
        }
        abas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        BorderPane raiz = new BorderPane();
        raiz.setTop(criarCabecalho());
        raiz.setCenter(abas);
        raiz.setBottom(criarBarraStatus());
        VBox toastArea = new VBox();
        toastArea.setPickOnBounds(false);
        StackPane camada = new StackPane(raiz, toastArea);
        StackPane.setAlignment(toastArea, Pos.TOP_RIGHT);
        StackPane.setMargin(toastArea, new Insets(70, 18, 0, 0));
        toasts = new ToastManager(toastArea);
        Scene cena = new Scene(camada, 1220, 780);
        cenaPrincipal = cena;
        temaEscuro = PreferencesManager.darkTheme();
        aplicarEstilo(cena);
        configurarAtalhos(cena);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setTitle("Nexus Studio");
        stage.setScene(cena);
        atualizarTabela(componenteDAO.listarTodos());
        atualizarEstoque();
        atualizarDashboard();
        stage.show();
    }

    private void mostrarSplash(Stage owner) {
        Stage splash = new Stage();
        splash.initOwner(owner);
        splash.initModality(Modality.APPLICATION_MODAL);
        ProgressBar progresso = new ProgressBar();
        progresso.setPrefWidth(260);
        Label texto = new Label("Inicializando catÃƒÂ¡logo...");
        VBox caixa = new VBox(14, new Label("Ã¢Å¡â„¢ NEXUS STUDIO"), texto, progresso);
        caixa.setAlignment(Pos.CENTER);
        caixa.setPadding(new Insets(32));
        Scene cena = new Scene(caixa, 360, 190);
        aplicarEstilo(cena);
        splash.setScene(cena);
        splash.setTitle("Nexus Studio");
        long inicio = System.currentTimeMillis();
        PauseTransition espera = new PauseTransition(Duration.seconds(2));
        espera.setOnFinished(e -> splash.close());
        espera.play();
        // showAndWait garante que a tela de login jamais apareÃƒÂ§a antes do perÃƒÂ­odo mÃƒÂ­nimo.
        splash.showAndWait();
        long restante = 2000 - (System.currentTimeMillis() - inicio);
        if (restante > 0) try { Thread.sleep(restante); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private boolean mostrarLogin(Stage owner) {
        Properties credenciais = new Properties();
        try (InputStream entrada = getClass().getResourceAsStream("/credentials.properties")) {
            if (entrada != null) credenciais.load(entrada);
        } catch (IOException ignored) {
            // Os valores padrÃƒÂ£o permitem iniciar mesmo sem o arquivo de configuraÃƒÂ§ÃƒÂ£o.
        }
        Stage login = new Stage();
        login.initOwner(owner);
        login.initModality(Modality.APPLICATION_MODAL);
        login.setTitle("Login Ã¢â‚¬â€ Nexus Studio");

        Label logo = new Label("\u2699");
        logo.getStyleClass().add("login-logo");
        Label titulo = new Label("NEXUS STUDIO");
        titulo.getStyleClass().add("login-title");
        Label subtitulo = new Label("Acesse o catÃƒÂ¡logo de hardware");
        subtitulo.getStyleClass().add("brand-subtitle");
        TextField usuario = campo("UsuÃƒÂ¡rio");
        usuario.setText("admin");
        PasswordField senha = new PasswordField();
        senha.setPromptText("Senha");
        senha.setText("nexus123");
        Label erro = new Label();
        erro.getStyleClass().add("field-error");
        erro.setVisible(false);
        Button entrar = botao("Entrar");
        entrar.setDefaultButton(true);
        final boolean[] autenticado = {false};
        Runnable autenticar = () -> {
            Usuario encontrado = usuarioDAO.autenticar(usuario.getText().trim(), senha.getText());
            // Compatibilidade com instalaÃƒÂ§ÃƒÂµes antigas que sÃƒÂ³ possuÃƒÂ­am credentials.properties.
            if (encontrado == null && usuario.getText().trim().equals(credenciais.getProperty("username", "admin"))
                    && senha.getText().equals(credenciais.getProperty("password", "nexus123"))) {
                encontrado = new Usuario(0, "admin", "Administrador", senha.getText(), "ADMIN", true);
            }
            if (encontrado != null) {
                autenticado[0] = true;
                usuarioAtual = encontrado;
                SessionManager.iniciar(encontrado);
                logDAO.registrar(encontrado.username(), "LOGIN", "SESSAO", "Login realizado");
                login.close();
            } else {
                erro.setText("UsuÃƒÂ¡rio ou senha incorretos");
                erro.setVisible(true);
            }
        };
        entrar.setOnAction(event -> autenticar.run());
        VBox formulario = new VBox(10, usuario, senha, erro, entrar);
        formulario.setFillWidth(true);
        VBox conteudo = new VBox(12, logo, titulo, subtitulo, formulario);
        conteudo.setAlignment(Pos.CENTER);
        conteudo.setPadding(new Insets(30, 44, 34, 44));
        conteudo.setPrefWidth(340);
        conteudo.getStyleClass().add("login-panel");
        Scene cena = new Scene(conteudo);
        aplicarEstilo(cena);
        login.setScene(cena);
        login.setOnCloseRequest(event -> {
            if (!autenticado[0]) Platform.exit();
        });
        login.showAndWait();
        return autenticado[0];
    }

    private HBox criarCabecalho() {
        Label icone = new Label("\u2699");
        icone.getStyleClass().add("brand-icon");
        Label titulo = new Label("NEXUS STUDIO");
        titulo.getStyleClass().add("brand-title");
        Label subtitulo = new Label("CatÃƒÂ¡logo e centro de compatibilidade de hardware");
        subtitulo.getStyleClass().add("brand-subtitle");
        Button tema = new Button();
        tema.setText(temaEscuro ? "Ã¢Ëœâ‚¬" : "Ã¢ËœÂ¾");
        tema.setTooltip(new Tooltip("Alternar tema (preferÃƒÂªncia salva)"));
        tema.setOnAction(e -> {
            temaEscuro = !temaEscuro;
            PreferencesManager.setDarkTheme(temaEscuro);
            tema.setText(temaEscuro ? "Ã¢Ëœâ‚¬" : "Ã¢ËœÂ¾");
            if (cenaPrincipal != null) aplicarEstilo(cenaPrincipal);
        });
        Label usuario = new Label(SessionManager.nome() + " (" + (SessionManager.admin() ? "admin" : "usuÃƒÂ¡rio") + ")");
        usuario.getStyleClass().add("brand-subtitle");
        VBox marca = new VBox(2, titulo, subtitulo, usuario);
        HBox cabecalho = new HBox(12, icone, marca, tema);
        HBox.setHgrow(marca, Priority.ALWAYS);
        tema.setMinWidth(44);
        cabecalho.setAlignment(Pos.CENTER_LEFT);
        cabecalho.getStyleClass().add("app-header");
        return cabecalho;
    }

    private HBox criarBarraStatus() {
        statusLabel = new Label();
        statusLabel.getStyleClass().add("status-label");
        HBox barra = new HBox(statusLabel);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("status-bar");
        return barra;
    }

    private Tab criarAbaDashboard() {
        dashboardTotal = new Label("--");
        dashboardCategorias = new Label("--");
        dashboardMenorPreco = new Label("--");
        dashboardMaiorTdp = new Label("--");
        HBox cartoes = new HBox(12,
                cartaoResumo("Total de componentes", dashboardTotal),
                cartaoResumo("Categorias", dashboardCategorias),
                cartaoResumo("Menor preÃƒÂ§o", dashboardMenorPreco),
                cartaoResumo("Maior TDP", dashboardMaiorTdp));
        cartoes.setFillHeight(true);

        CategoryAxis categorias = new CategoryAxis();
        categorias.setLabel("Categoria");
        NumberAxis quantidade = new NumberAxis();
        quantidade.setLabel("Componentes");
        componentesPorCategoriaChart = new BarChart<>(categorias, quantidade);
        componentesPorCategoriaChart.setTitle("Componentes por categoria");
        componentesPorCategoriaChart.setLegendVisible(false);
        componentesPorCategoriaChart.setAnimated(false);
        CategoryAxis tdpCategorias = new CategoryAxis();
        NumberAxis tdpValores = new NumberAxis();
        tdpValores.setLabel("TDP mÃƒÂ©dio (W)");
        tdpMedioPorCategoriaChart = new BarChart<>(tdpCategorias, tdpValores);
        tdpMedioPorCategoriaChart.setTitle("TDP mÃƒÂ©dio por categoria");
        tdpMedioPorCategoriaChart.setLegendVisible(false);
        tdpMedioPorCategoriaChart.setAnimated(false);

        precoPorFabricanteChart = new PieChart();
        precoPorFabricanteChart.setTitle("PreÃƒÂ§o mÃƒÂ©dio por fabricante");
        precoPorFabricanteChart.setAnimated(false);
        HBox graficos = new HBox(16, componentesPorCategoriaChart, tdpMedioPorCategoriaChart, precoPorFabricanteChart);
        HBox.setHgrow(componentesPorCategoriaChart, Priority.ALWAYS);
        HBox.setHgrow(tdpMedioPorCategoriaChart, Priority.ALWAYS);
        HBox.setHgrow(precoPorFabricanteChart, Priority.ALWAYS);
        componentesPorCategoriaChart.setPrefHeight(390);
        tdpMedioPorCategoriaChart.setPrefHeight(390);
        precoPorFabricanteChart.setPrefHeight(390);
        VBox conteudo = new VBox(16, cartoes, graficos);
        conteudo.setPadding(new Insets(20));
        VBox.setVgrow(graficos, Priority.ALWAYS);
        Tab tab = new Tab("Dashboard", conteudo);
        tab.setOnSelectionChanged(event -> {
            if (tab.isSelected()) atualizarDashboard();
        });
        return tab;
    }

    private VBox cartaoResumo(String titulo, Label valor) {
        Label nome = new Label(titulo);
        nome.getStyleClass().add("summary-title");
        valor.getStyleClass().add("summary-value");
        VBox cartao = new VBox(6, nome, valor);
        cartao.setPrefHeight(82);
        cartao.setMinWidth(170);
        HBox.setHgrow(cartao, Priority.ALWAYS);
        cartao.getStyleClass().add("summary-card");
        return cartao;
    }

    private void atualizarDashboard() {
        if (dashboardTotal == null) return;
        List<Componente> lista = componenteDAO.listarTodos();
        List<Categoria> categorias = categoriaDAO.listarTodas();
        dashboardTotal.setText(String.valueOf(lista.size()));
        dashboardCategorias.setText(String.valueOf(categorias.size()));
        dashboardMenorPreco.setText(lista.isEmpty() ? "--" :
                String.format("R$ %.2f", lista.stream().mapToDouble(Componente::getPreco).min().orElse(0)));
        dashboardMaiorTdp.setText(lista.isEmpty() ? "--" :
                String.format("%.0f W", lista.stream().mapToDouble(Componente::getTdpWatts).max().orElse(0)));

        Map<Integer, String> nomesCategorias = new HashMap<>();
        categorias.forEach(categoria -> nomesCategorias.put(categoria.getId(), categoria.getNome()));
        Map<String, Long> porCategoria = new LinkedHashMap<>();
        lista.forEach(componente -> porCategoria.merge(
                nomesCategorias.getOrDefault(componente.getCategoriaId(), "Sem categoria"), 1L, Long::sum));
        componentesPorCategoriaChart.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        porCategoria.forEach((nome, total) -> serie.getData().add(new XYChart.Data<>(nome, total)));
        componentesPorCategoriaChart.getData().add(serie);
        Map<String, double[]> tdpPorCategoria = new LinkedHashMap<>();
        lista.forEach(c -> {
            String cat = nomesCategorias.getOrDefault(c.getCategoriaId(), "Sem categoria");
            double[] v = tdpPorCategoria.computeIfAbsent(cat, ignored -> new double[2]);
            v[0] += c.getTdpWatts(); v[1]++;
        });
        tdpMedioPorCategoriaChart.getData().clear();
        XYChart.Series<String, Number> serieTdp = new XYChart.Series<>();
        tdpPorCategoria.forEach((nome, valores) -> {
            double media = valores[1] == 0 ? 0 : valores[0] / valores[1];
            XYChart.Data<String, Number> dado = new XYChart.Data<>(nome, media);
            serieTdp.getData().add(dado);
            dado.nodeProperty().addListener((obs, antigo, node) -> adicionarRotuloGrafico(node, String.format("%.0f W", media)));
        });
        tdpMedioPorCategoriaChart.getData().add(serieTdp);

        Map<String, double[]> porFabricante = new LinkedHashMap<>();
        lista.forEach(componente -> {
            String fabricante = texto(componente.getFabricante());
            double[] valores = porFabricante.computeIfAbsent(fabricante, chave -> new double[2]);
            valores[0] += componente.getPreco();
            valores[1]++;
        });
        precoPorFabricanteChart.getData().clear();
        porFabricante.forEach((fabricante, valores) ->
                precoPorFabricanteChart.getData().add(new PieChart.Data(fabricante, valores[0] / valores[1])));
    }

    private void adicionarRotuloGrafico(Node node, String texto) {
        if (node == null) return;
        if (node instanceof StackPane barra) {
            Label rotulo = new Label(texto);
            rotulo.getStyleClass().add("chart-label");
            barra.getChildren().add(rotulo);
            StackPane.setAlignment(rotulo, Pos.TOP_CENTER);
        }
    }

    private Tab criarAbaComponentes() {
        Button atualizar = botao("Atualizar");
        atualizar.setOnAction(event -> { atualizarTabela(componenteDAO.listarTodos()); registrar("ATUALIZAR", "COMPONENTE", "Lista atualizada"); });
        Button excluir = botao("Excluir selecionado");
        excluir.setDisable(!SessionManager.atual().isOperador());
        excluir.setOnAction(event -> excluirSelecionado());
        buscaComponentes = campo("Buscar por nome, fabricante ou modelo...");
        buscaComponentes.textProperty().addListener((obs, antigo, atual) -> aplicarBuscaComponentes());
        Button limparBusca = new Button("\u2715");
        limparBusca.setTooltip(new Tooltip("Limpar busca"));
        limparBusca.setOnAction(event -> buscaComponentes.clear());
        HBox busca = new HBox(6, buscaComponentes, limparBusca);
        busca.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(buscaComponentes, Priority.ALWAYS);
        ToolBar barra = new ToolBar(atualizar, excluir, new Separator(), busca);
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antigo, atual) -> mostrarDetalhes(atual));
        SplitPane divisao = new SplitPane(new VBox(10, barra, tabela), criarPainelDetalhes());
        divisao.setDividerPositions(.72);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        tamanhoPagina = new ComboBox<>(FXCollections.observableArrayList(15, 30, 50, 100));
        tamanhoPagina.setValue(PreferencesManager.pageSize());
        if (!tamanhoPagina.getItems().contains(tamanhoPagina.getValue())) tamanhoPagina.setValue(15);
        tamanhoPagina.valueProperty().addListener((obs, old, value) -> {
            PreferencesManager.setPageSize(value);
            atualizarPaginaComponentes();
        });
        paginacaoComponentes = new Pagination(1, 0);
        paginacaoComponentes.currentPageIndexProperty().addListener((obs, old, value) -> atualizarPaginaComponentes());
        VBox lista = new VBox(8, new HBox(8, new Label("Itens por pÃƒÂ¡gina:"), tamanhoPagina), divisao, paginacaoComponentes);
        VBox.setVgrow(divisao, Priority.ALWAYS);
        return new Tab("Componentes", lista);
    }

    private void aplicarBuscaComponentes() {
        String termo = buscaComponentes == null ? "" : normalizar(buscaComponentes.getText());
        componentesVisiveis.setPredicate(componente -> termo.isBlank()
                || normalizar(texto(componente.getNome())).contains(termo)
                || normalizar(texto(componente.getFabricante())).contains(termo)
                || normalizar(texto(componente.getModelo())).contains(termo));
        if (paginacaoComponentes != null) {
            paginacaoComponentes.setCurrentPageIndex(0);
            atualizarPaginaComponentes();
        }
    }

    private void atualizarPaginaComponentes() {
        if (paginacaoComponentes == null || tamanhoPagina == null) return;
        int tamanho = tamanhoPagina.getValue();
        int paginas = Math.max(1, (int) Math.ceil(componentesVisiveis.size() / (double) tamanho));
        paginacaoComponentes.setPageCount(paginas);
        int pagina = Math.min(paginacaoComponentes.getCurrentPageIndex(), paginas - 1);
        int de = pagina * tamanho;
        int ate = Math.min(de + tamanho, componentesVisiveis.size());
        tabela.getItems().setAll(componentesVisiveis.subList(de, ate));
    }

    private VBox criarPainelDetalhes() {
        imagemDetalhe = new ImageView();
        imagemDetalhe.setFitWidth(210);
        imagemDetalhe.setFitHeight(150);
        imagemDetalhe.setPreserveRatio(true);
        imagemPlaceholder = new Label("\u2699\nSem imagem");
        imagemPlaceholder.setStyle("-fx-font-size: 28px; -fx-text-alignment: center;");
        StackPane imagem = new StackPane(imagemDetalhe, imagemPlaceholder);
        imagem.getStyleClass().add("image-placeholder");
        imagem.setPrefHeight(170);
        detalheNome = new Label("Selecione um componente");
        detalheNome.getStyleClass().add("section-title");
        detalheDados = new Label("Os detalhes e o histÃƒÂ³rico aparecerÃƒÂ£o aqui.");
        detalheDados.setWrapText(true);
        Button escolher = botao("Adicionar imagem");
        escolher.setDisable(!SessionManager.atual().isOperador());
        escolher.setOnAction(event -> escolherImagem());
        novoPreco = campo("Novo preÃƒÂ§o (R$)");
        Button atualizarPreco = botao("Atualizar preÃƒÂ§o");
        atualizarPreco.setDisable(!SessionManager.atual().isOperador());
        atualizarPreco.setOnAction(event -> atualizarPrecoSelecionado());
        VBox painel = new VBox(12, imagem, detalheNome, detalheDados,
                new Separator(), rotulo("Alterar preÃƒÂ§o"), novoPreco, atualizarPreco, escolher);
        painel.setPadding(new Insets(16));
        painel.setMinWidth(270);
        painel.getStyleClass().add("content-panel");
        return painel;
    }

    private void mostrarDetalhes(Componente componente) {
        if (componente == null) {
            detalheNome.setText("Selecione um componente");
            detalheDados.setText("Os detalhes e o histÃƒÂ³rico aparecerÃƒÂ£o aqui.");
            imagemDetalhe.setImage(null);
            imagemPlaceholder.setVisible(true);
            return;
        }
        detalheNome.setText(componente.getNome());
        detalheDados.setText(String.format("Fabricante: %s%nModelo: %s%nPreÃƒÂ§o: R$ %.2f%nTDP: %.0f W",
                texto(componente.getFabricante()), componente.getModelo(), componente.getPreco(), componente.getTdpWatts()));
        imagemDetalhe.setImage(null);
        imagemPlaceholder.setVisible(true);
        if (componente.getImagePath() != null && !componente.getImagePath().isBlank()
                && Files.exists(Path.of(componente.getImagePath()))) {
            imagemDetalhe.setImage(new Image(Path.of(componente.getImagePath()).toUri().toString()));
            imagemPlaceholder.setVisible(false);
        }
        novoPreco.clear();
    }

    private void escolherImagem() {
        Componente selecionado = tabela.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            mostrarErro("Selecione um componente primeiro.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Escolher imagem do componente");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File arquivo = chooser.showOpenDialog(tabela.getScene().getWindow());
        if (arquivo == null) return;
        componenteDAO.atualizar(new Componente(selecionado.getId(), selecionado.getCategoriaId(),
                selecionado.getNome(), selecionado.getFabricante(), selecionado.getModelo(),
                selecionado.getPreco(), selecionado.getTdpWatts(), arquivo.getAbsolutePath()));
        atualizarTabela(componenteDAO.listarTodos());
        selecionarPorId(selecionado.getId());
    }

    private void atualizarPrecoSelecionado() {
        Componente selecionado = tabela.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            mostrarErro("Selecione um componente primeiro.");
            return;
        }
        try {
            double preco = numero(novoPreco);
            if (preco < 0) throw new NumberFormatException();
            componenteDAO.atualizarPreco(selecionado.getId(), preco);
            historicoDAO.inserir(new HistoricoPreco(0, selecionado.getId(), preco, LocalDateTime.now().toString()));
            registrar("ATUALIZAR", "COMPONENTE", "PreÃƒÂ§o id=" + selecionado.getId());
            atualizarTabela(componenteDAO.listarTodos());
            selecionarPorId(selecionado.getId());
            atualizarGrafico();
            mostrarInfo("PreÃƒÂ§o atualizado e registrado no histÃƒÂ³rico.");
        } catch (NumberFormatException e) {
            mostrarErro("Informe um preÃƒÂ§o vÃƒÂ¡lido.");
        }
    }

    private Tab criarAbaCadastro() {
        GridPane formulario = new GridPane();
        formulario.setHgap(12);
        formulario.setVgap(12);
        formulario.setPadding(new Insets(24));
        formulario.getStyleClass().add("form-card");
        TextField nome = campo("Nome do componente");
        TextField fabricante = campo("Fabricante");
        TextField modelo = campo("Modelo");
        TextField preco = campo("PreÃƒÂ§o em R$");
        TextField tdp = campo("TDP em watts");
        Label erroCategoria = mensagemCampo();
        Label erroNome = mensagemCampo();
        Label erroFabricante = mensagemCampo();
        Label erroModelo = mensagemCampo();
        Label erroPreco = mensagemCampo();
        Label erroTdp = mensagemCampo();
        categoriaCombo = new ComboBox<>();
        categoriaCombo.setPromptText("Selecione uma categoria");
        categoriaCombo.setMaxWidth(Double.MAX_VALUE);
        carregarCategorias();
        formulario.addRow(0, rotulo("Categoria"), campoComErro(categoriaCombo, erroCategoria));
        formulario.addRow(1, rotulo("Nome"), campoComErro(nome, erroNome));
        formulario.addRow(2, rotulo("Fabricante"), campoComErro(fabricante, erroFabricante));
        formulario.addRow(3, rotulo("Modelo"), campoComErro(modelo, erroModelo));
        formulario.addRow(4, rotulo("PreÃƒÂ§o (R$)"), campoComErro(preco, erroPreco));
        formulario.addRow(5, rotulo("TDP (W)"), campoComErro(tdp, erroTdp));
        formulario.getColumnConstraints().addAll(new ColumnConstraints(130), colunaExpansivel());
        Button salvar = botao("Cadastrar componente");
        Button importar = botao("Importar CSV");
        Button exportarExemplo = botao("Exportar exemplo CSV");
        boolean podeEditar = SessionManager.atual().isOperador();
        salvar.setDisable(!podeEditar);
        importar.setDisable(!podeEditar);
        Label sucesso = new Label();
        sucesso.getStyleClass().add("success-message");
        sucesso.setVisible(false);
        categoriaCombo.valueProperty().addListener((obs, antigo, atual) -> esconderMensagem(erroCategoria));
        for (TextField campo : List.of(nome, fabricante, modelo, preco, tdp)) {
            campo.textProperty().addListener((obs, antigo, atual) -> sucesso.setVisible(false));
        }
        salvar.setOnAction(event -> {
            sucesso.setVisible(false);
            boolean valido = true;
            valido &= validarObrigatorio(nome, erroNome, "Nome ÃƒÂ© obrigatÃƒÂ³rio.");
            valido &= validarObrigatorio(fabricante, erroFabricante, "Fabricante ÃƒÂ© obrigatÃƒÂ³rio.");
            valido &= validarObrigatorio(modelo, erroModelo, "Modelo ÃƒÂ© obrigatÃƒÂ³rio.");
            Categoria categoria = categoriaCombo.getValue();
            if (categoria == null) {
                exibirMensagem(erroCategoria, "Categoria ÃƒÂ© obrigatÃƒÂ³ria.");
                valido = false;
            } else esconderMensagem(erroCategoria);
            Double precoNumerico = validarPositivo(preco, erroPreco, "PreÃƒÂ§o deve ser maior que zero.");
            Double tdpNumerico = validarPositivo(tdp, erroTdp, "TDP deve ser maior que zero.");
            if (precoNumerico == null || tdpNumerico == null) valido = false;
            if (!valido) return;
            Componente novo = new Componente(categoria.getId(), nome.getText().trim(), fabricante.getText().trim(),
                    modelo.getText().trim(), precoNumerico, tdpNumerico);
            componenteDAO.inserir(novo);
            registrar("CRIAR", "COMPONENTE", novo.getNome());
            atualizarTabela(componenteDAO.listarTodos());
            atualizarEstoque();
            atualizarDashboard();
            nome.clear(); fabricante.clear(); modelo.clear(); preco.clear(); tdp.clear();
            sucesso.setText("Componente cadastrado com sucesso!");
            sucesso.setVisible(true);
        });
        importar.setOnAction(event -> importarCsv(formulario.getScene() == null ? null : formulario.getScene().getWindow()));
        exportarExemplo.setOnAction(event -> exportarExemploCsv());
        HBox acoes = new HBox(10, salvar, importar, exportarExemplo);
        VBox conteudo = new VBox(10, formulario, acoes, sucesso);
        conteudo.setPadding(new Insets(10));
        conteudo.getStyleClass().add("content-panel");
        return new Tab("Cadastrar", conteudo);
    }

    private VBox campoComErro(Control campo, Label erro) {
        VBox caixa = new VBox(3, campo, erro);
        VBox.setVgrow(campo, Priority.NEVER);
        return caixa;
    }

    private Label mensagemCampo() {
        Label erro = new Label();
        erro.getStyleClass().add("field-error");
        erro.setVisible(false);
        return erro;
    }

    private boolean validarObrigatorio(TextField campo, Label erro, String mensagem) {
        if (campo.getText() == null || campo.getText().isBlank()) {
            exibirMensagem(erro, mensagem);
            return false;
        }
        esconderMensagem(erro);
        return true;
    }

    private Double validarPositivo(TextField campo, Label erro, String mensagem) {
        try {
            double valor = numero(campo);
            if (!Double.isFinite(valor) || valor <= 0) throw new NumberFormatException();
            esconderMensagem(erro);
            return valor;
        } catch (NumberFormatException ex) {
            exibirMensagem(erro, mensagem);
            return null;
        }
    }

    private void exibirMensagem(Label label, String mensagem) {
        label.setText(mensagem);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void esconderMensagem(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    private void importarCsv(javafx.stage.Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importar componentes CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivos CSV", "*.csv"));
        File arquivo = chooser.showOpenDialog(owner);
        if (arquivo == null) return;
        Map<Integer, Categoria> categorias = new HashMap<>();
        categoriaDAO.listarTodas().forEach(categoria -> categorias.put(categoria.getId(), categoria));
        int importados = 0;
        int ignorados = 0;
        try {
            List<String> linhas = Files.readAllLines(arquivo.toPath(), StandardCharsets.UTF_8);
            for (int indice = 0; indice < linhas.size(); indice++) {
                String linha = linhas.get(indice);
                String linhaSemBOM = indice == 0 ? linha.replace("\uFEFF", "") : linha;
                if (indice == 0 && linhaSemBOM.trim().toLowerCase(Locale.ROOT).startsWith("categoria_id")) continue;
                if (linhaSemBOM.isBlank()) {
                    ignorados++;
                    continue;
                }
                List<String> campos = separarCsv(linhaSemBOM);
                if (campos.size() != 6) {
                    ignorados++;
                    continue;
                }
                try {
                    int categoriaId = Integer.parseInt(campos.get(0).trim());
                    String nome = campos.get(1).trim();
                    String fabricante = campos.get(2).trim();
                    String modelo = campos.get(3).trim();
                    double preco = Double.parseDouble(campos.get(4).trim().replace(',', '.'));
                    double tdp = Double.parseDouble(campos.get(5).trim().replace(',', '.'));
                    if (!categorias.containsKey(categoriaId) || nome.isBlank() || fabricante.isBlank()
                            || modelo.isBlank() || !Double.isFinite(preco) || preco <= 0
                            || !Double.isFinite(tdp) || tdp <= 0) {
                        ignorados++;
                        continue;
                    }
                    componenteDAO.inserir(new Componente(categoriaId, nome, fabricante, modelo, preco, tdp));
                    registrar("CRIAR", "COMPONENTE", "ImportaÃƒÂ§ÃƒÂ£o CSV: " + nome);
                    importados++;
                } catch (NumberFormatException ex) {
                    ignorados++;
                }
            }
            atualizarTabela(componenteDAO.listarTodos());
            atualizarEstoque();
            atualizarDashboard();
            mostrarInfo(importados + " componentes importados, " + ignorados + " linhas ignoradas");
        } catch (IOException ex) {
            mostrarErro("NÃƒÂ£o foi possÃƒÂ­vel ler o arquivo CSV: " + ex.getMessage());
        }
    }

    private List<String> separarCsv(String linha) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean aspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char caractere = linha.charAt(i);
            if (caractere == '"') {
                if (aspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    atual.append('"');
                    i++;
                } else aspas = !aspas;
            } else if (caractere == ',' && !aspas) {
                campos.add(atual.toString());
                atual.setLength(0);
            } else atual.append(caractere);
        }
        campos.add(atual.toString());
        return campos;
    }

    private void exportarExemploCsv() {
        try {
            Path desktop = Path.of(System.getProperty("user.home"), "Desktop");
            Files.createDirectories(desktop);
            int categoriaId = categoriaDAO.listarTodas().stream().findFirst().map(Categoria::getId).orElse(1);
            Path arquivo = desktop.resolve("nexus-studio-exemplo.csv");
            String csv = "categoria_id,nome,fabricante,modelo,preco,tdp_watts\n"
                    + categoriaId + ",Exemplo,Exemplo,Modelo-1,100.00,50\n";
            Files.writeString(arquivo, csv, StandardCharsets.UTF_8);
            mostrarInfo("Exemplo CSV salvo em: " + arquivo);
        } catch (IOException ex) {
            mostrarErro("NÃƒÂ£o foi possÃƒÂ­vel exportar o exemplo CSV: " + ex.getMessage());
        }
    }

    private Tab criarAbaRelatorios() {
        ComboBox<String> tipo = new ComboBox<>(FXCollections.observableArrayList(
                "Todos", "Faixa de preÃƒÂ§o", "TDP mÃƒÂ¡ximo", "Fabricante"));
        tipo.setValue("Todos");
        TextField valor1 = campo("Valor / fabricante");
        TextField valor2 = campo("PreÃƒÂ§o mÃƒÂ¡ximo");
        Button aplicar = botao("Aplicar filtro");
        Button exportar = botao("Exportar PDF");
        Button excel = botao("Exportar Excel");
        TableView<Componente> resultado = criarTabela("Nenhum componente encontrado.");
        aplicar.setOnAction(event -> {
            try {
                resultado.getItems().setAll(filtrar(tipo.getValue(), valor1.getText(), valor2.getText()));
            } catch (NumberFormatException e) {
                mostrarErro("Os valores do relatÃƒÂ³rio devem ser nÃƒÂºmeros vÃƒÂ¡lidos.");
            }
        });
        tipo.valueProperty().addListener((obs, antigo, atual) -> {
            boolean faixa = "Faixa de preÃƒÂ§o".equals(atual);
            valor2.setDisable(!faixa);
            valor2.setVisible(faixa);
        });
        valor2.setDisable(true);
        valor2.setVisible(false);
        exportar.setOnAction(event -> exportarComponentesPdf(new ArrayList<>(resultado.getItems()),
                "relatÃƒÂ³rio filtrado"));
        excel.setOnAction(event -> exportarExcel());
        return new Tab("RelatÃƒÂ³rios", painel(new ToolBar(tipo, valor1, valor2, aplicar, exportar, excel), resultado));
    }

    private Tab criarAbaCompatibilidade() {
        ComboBox<Componente> combo = comboComponentes("Selecione um componente");
        Label socketsTitulo = new Label("Sockets e padrÃƒÂµes cadastrados");
        socketsTitulo.getStyleClass().add("section-title");
        FlowPane sockets = new FlowPane(8, 8);
        sockets.getStyleClass().add("socket-list");
        Label compativeisTitulo = new Label("Componentes compatÃƒÂ­veis");
        compativeisTitulo.getStyleClass().add("section-title");
        VBox compativeis = new VBox(8, new Label("Selecione um componente para consultar."));
        compativeis.getStyleClass().add("compatible-list");
        combo.setOnAction(event -> {
            sockets.getChildren().clear();
            compativeis.getChildren().clear();
            Componente selecionado = combo.getValue();
            if (selecionado == null) return;
            List<Compatibilidade> registros = compatibilidadeDAO.listarPorComponente(selecionado.getId());
            if (registros.isEmpty()) sockets.getChildren().add(new Label("Nenhum socket ou padrÃƒÂ£o cadastrado."));
            registros.forEach(registro -> {
                Label nome = new Label(registro.getSocketTipo() + formatarPadrao(registro.getPadrao()));
                Button remover = new Button("Ãƒâ€”");
                remover.setOnAction(e -> {
                    if (!confirmarExclusao("Excluir compatibilidade", "Remover este registro?")) return;
                    undoManager.push("DELETE_COMPATIBILIDADE", registro.getComponenteId(), registro);
                    compatibilidadeDAO.deletar(registro.getId());
                    registrar("EXCLUIR", "COMPATIBILIDADE", "id=" + registro.getId());
                    combo.getOnAction().handle(new javafx.event.ActionEvent());
                    mostrarUndo("Compatibilidade removida.");
                });
                HBox linha = new HBox(4, nome, remover);
                linha.setAlignment(Pos.CENTER_LEFT);
                sockets.getChildren().add(linha);
            });
            Map<Integer, Componente> encontrados = new LinkedHashMap<>();
            registros.forEach(registro -> compatibilidadeDAO.buscarPorSocket(registro.getSocketTipo())
                    .forEach(compatibilidade -> {
                        if (compatibilidade.getComponenteId() != selecionado.getId()) {
                            Componente outro = componenteDAO.buscarPorId(compatibilidade.getComponenteId());
                            if (outro != null) encontrados.put(outro.getId(), outro);
                        }
                    }));
            if (encontrados.isEmpty()) compativeis.getChildren().add(new Label("Nenhum componente compatÃƒÂ­vel encontrado."));
            encontrados.values().forEach(componente -> compativeis.getChildren().add(criarCartaoCompativel(componente)));
        });
        VBox conteudo = new VBox(12, rotulo("Componente para consultar"), combo,
                socketsTitulo, sockets, compativeisTitulo, compativeis);
        conteudo.setPadding(new Insets(22));
        VBox.setVgrow(compativeis, Priority.ALWAYS);
        ScrollPane scroll = new ScrollPane(conteudo);
        scroll.setFitToWidth(true);
        return new Tab("Compatibilidade", scroll);
    }

    private Tab criarAbaComparar() {
        ComboBox<Componente> primeiro = comboComponentes("Primeiro componente");
        ComboBox<Componente> segundo = comboComponentes("Segundo componente");
        TableView<ComparacaoLinha> tabelaComparacao = new TableView<>();
        tabelaComparacao.setPlaceholder(new Label("Selecione dois componentes para comparar."));
        tabelaComparacao.getColumns().setAll(
                colunaComparacao("CaracterÃ¯Â¿Â½stica", ComparacaoLinha::caracteristica),
                colunaComparacao("Componente A", ComparacaoLinha::primeiro),
                colunaComparacao("Componente B", ComparacaoLinha::segundo));
        tabelaComparacao.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        Label diferenca = new Label("DiferenÃ¯Â¿Â½a de preÃ¯Â¿Â½o: -");
        Label socketsCompartilhados = new Label("Sockets compartilhados: -");
        Label veredito = new Label("Selecione dois componentes para comparar.");
        veredito.getStyleClass().add("verdict");
        Runnable atualizar = () -> {
            Componente a = primeiro.getValue();
            Componente b = segundo.getValue();
            tabelaComparacao.getItems().clear();
            if (a == null || b == null || a == b) {
                diferenca.setText("DiferenÃ¯Â¿Â½a de preÃ¯Â¿Â½o: -");
                socketsCompartilhados.setText("Sockets compartilhados: -");
                veredito.setText("Selecione dois componentes diferentes para comparar.");
                veredito.getStyleClass().removeAll("verdict-ok", "verdict-error");
                return;
            }
            Set<String> socketsA = sockets(a);
            Set<String> socketsB = sockets(b);
            Set<String> compartilhados = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            socketsA.forEach(socket -> socketsB.stream().filter(socket::equalsIgnoreCase).findFirst()
                    .ifPresent(compartilhados::add));
            tabelaComparacao.getItems().addAll(
                    new ComparacaoLinha("Nome", texto(a.getNome()), texto(b.getNome())),
                    new ComparacaoLinha("Fabricante", texto(a.getFabricante()), texto(b.getFabricante())),
                    new ComparacaoLinha("Modelo", texto(a.getModelo()), texto(b.getModelo())),
                    new ComparacaoLinha("PreÃ¯Â¿Â½o", String.format("R$ %.2f", a.getPreco()), String.format("R$ %.2f", b.getPreco())),
                    new ComparacaoLinha("TDP", String.format("%.0f W", a.getTdpWatts()), String.format("%.0f W", b.getTdpWatts())),
                    new ComparacaoLinha("EspecificaÃ¯Â¿Â½Ã¯Â¿Â½es", formatarEspecificacoes(a.getId()), formatarEspecificacoes(b.getId())),
                    new ComparacaoLinha("Sockets", formatarSockets(socketsA), formatarSockets(socketsB)));
            diferenca.setText(String.format("DiferenÃ¯Â¿Â½a de preÃ¯Â¿Â½o: R$ %.2f (%s)", Math.abs(a.getPreco() - b.getPreco()),
                    a.getPreco() <= b.getPreco() ? "A Ã¯Â¿Â½ mais barata" : "B Ã¯Â¿Â½ mais barata"));
            socketsCompartilhados.setText("Sockets compartilhados: " + (compartilhados.isEmpty() ? "nenhum" : String.join(", ", compartilhados)));
            boolean compativel = !compartilhados.isEmpty();
            veredito.setText(compativel ? "Veredito: componentes compatÃ¯Â¿Â½veis." : "Veredito: nenhum socket compatÃ¯Â¿Â½vel encontrado.");
            veredito.getStyleClass().removeAll("verdict-ok", "verdict-error");
            veredito.getStyleClass().add(compativel ? "verdict-ok" : "verdict-error");
        };
        primeiro.setOnAction(e -> atualizar.run());
        segundo.setOnAction(e -> atualizar.run());
        Button exportar = botao("Exportar PDF");
        exportar.setOnAction(e -> exportarComparacao(primeiro.getValue(), segundo.getValue()));
        HBox selecao = new HBox(10, rotulo("Componente A"), primeiro, rotulo("Componente B"), segundo);
        HBox.setHgrow(primeiro, Priority.ALWAYS);
        HBox.setHgrow(segundo, Priority.ALWAYS);
        VBox conteudo = new VBox(12, selecao, tabelaComparacao, diferenca, socketsCompartilhados, veredito, exportar);
        conteudo.setPadding(new Insets(18));
        VBox.setVgrow(tabelaComparacao, Priority.ALWAYS);
        Tab tab = new Tab("Comparar", conteudo);
        tab.setOnSelectionChanged(e -> { if (tab.isSelected()) { List<Componente> lista = componenteDAO.listarTodos(); primeiro.getItems().setAll(lista); segundo.getItems().setAll(lista); } });
        return tab;
    }

    private <T> TableColumn<ComparacaoLinha, T> colunaComparacao(String titulo,
                                                                    java.util.function.Function<ComparacaoLinha, T> valor) {
        TableColumn<ComparacaoLinha, T> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(dado -> new ReadOnlyObjectWrapper<>(valor.apply(dado.getValue())));
        return coluna;
    }

    private Set<String> sockets(Componente componente) {
        return compatibilidadeDAO.listarPorComponente(componente.getId()).stream()
                .map(Compatibilidade::getSocketTipo).filter(Objects::nonNull).filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
    }

    private String formatarSockets(Set<String> sockets) {
        return sockets.isEmpty() ? "Nenhum" : String.join(", ", sockets);
    }

    private String formatarEspecificacoes(int componenteId) {
        List<Especificacao> especificacoes = especificacaoDAO.listarPorComponente(componenteId);
        if (especificacoes.isEmpty()) return "Nenhuma";
        return especificacoes.stream().map(s -> s.getChave() + "=" + s.getValor() +
                (s.getUnidade() == null || s.getUnidade().isBlank() ? "" : " " + s.getUnidade()))
                .collect(Collectors.joining("; "));
    }

    private void exportarComparacao(Componente a, Componente b) {
        if (a == null || b == null || a == b) { mostrarErro("Selecione dois componentes diferentes antes de exportar."); return; }
        try {
            Path desktop = Path.of(System.getProperty("user.home"), "Desktop");
            Files.createDirectories(desktop);
            Path arquivo = desktop.resolve("nexus-comparacao-" + LocalDate.now() + ".pdf");
            try (PdfWriter writer = new PdfWriter(arquivo.toString()); PdfDocument pdf = new PdfDocument(writer);
                 Document documento = new Document(pdf)) {
                documento.add(new Paragraph("Nexus Studio - ComparaÃ¯Â¿Â½Ã¯Â¿Â½o de componentes").setBold());
                documento.add(new Paragraph("Gerado em " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
                Table tabelaPdf = new Table(UnitValue.createPercentArray(new float[]{2, 4, 4})).useAllAvailableWidth();
                tabelaPdf.addHeaderCell("CaracterÃ¯Â¿Â½stica"); tabelaPdf.addHeaderCell(a.getNome()); tabelaPdf.addHeaderCell(b.getNome());
                String[][] linhas = {{"Fabricante", texto(a.getFabricante()), texto(b.getFabricante())},
                        {"Modelo", texto(a.getModelo()), texto(b.getModelo())},
                        {"PreÃ¯Â¿Â½o", String.format("R$ %.2f", a.getPreco()), String.format("R$ %.2f", b.getPreco())},
                        {"TDP", String.format("%.0f W", a.getTdpWatts()), String.format("%.0f W", b.getTdpWatts())},
                        {"EspecificaÃ¯Â¿Â½Ã¯Â¿Â½es", formatarEspecificacoes(a.getId()), formatarEspecificacoes(b.getId())},
                        {"Sockets", formatarSockets(sockets(a)), formatarSockets(sockets(b))}};
                for (String[] linha : linhas) for (String valor : linha) tabelaPdf.addCell(valor);
                documento.add(tabelaPdf);
            }
            registrar("EXPORTAR", "COMPARACAO", arquivo.toString());
            mostrarInfo("ComparaÃ¯Â¿Â½Ã¯Â¿Â½o exportada em: " + arquivo);
        } catch (Exception e) { mostrarErro("NÃ¯Â¿Â½o foi possÃ¯Â¿Â½vel gerar a comparaÃ¯Â¿Â½Ã¯Â¿Â½o: " + e.getMessage()); }
    }
    private Tab criarAbaHistorico() {
        historicoCombo = comboComponentes("Selecione um componente");
        historicoCombo.setOnAction(event -> atualizarGrafico());
        NumberAxis eixoX = new NumberAxis();
        NumberAxis eixoY = new NumberAxis();
        eixoX.setLabel("Registro");
        eixoY.setLabel("PreÃƒÂ§o (R$)");
        historicoChart = new LineChart<>(eixoX, eixoY);
        historicoChart.setTitle("EvoluÃƒÂ§ÃƒÂ£o do preÃƒÂ§o");
        historicoChart.setCreateSymbols(true);
        VBox conteudo = new VBox(12, new HBox(10, rotulo("Componente"), historicoCombo), historicoChart);
        conteudo.setPadding(new Insets(18));
        VBox.setVgrow(historicoChart, Priority.ALWAYS);
        return new Tab("HistÃƒÂ³rico de preÃƒÂ§os", conteudo);
    }

    private void atualizarGrafico() {
        if (historicoChart == null || historicoCombo == null) return;
        historicoChart.getData().clear();
        Componente selecionado = historicoCombo.getValue();
        if (selecionado == null) return;
        List<HistoricoPreco> registros = historicoDAO.listarPorComponente(selecionado.getId());
        if (registros.isEmpty()) registros = List.of(new HistoricoPreco(0, selecionado.getId(),
                selecionado.getPreco(), LocalDateTime.now().toString()));
        XYChart.Series<Number, Number> serie = new XYChart.Series<>();
        serie.setName(selecionado.getModelo());
        for (int i = 0; i < registros.size(); i++) serie.getData().add(
                new XYChart.Data<>(i + 1, registros.get(i).preco()));
        historicoChart.getData().add(serie);
    }

    private Tab criarAbaEstoque() {
        TableView<EstoqueLinha> tabelaEstoque = new TableView<>(estoque);
        tabelaEstoque.setPlaceholder(new Label("Nenhum componente cadastrado."));
        tabelaEstoque.getColumns().setAll(
                colunaEstoque("Componente", linha -> linha.componente().getNome()),
                colunaEstoque("Modelo", linha -> linha.componente().getModelo()),
                colunaEstoque("Quantidade", EstoqueLinha::quantidade),
                colunaEstoque("LocalizaÃƒÂ§ÃƒÂ£o", linha -> texto(linha.localizacao())));
        tabelaEstoque.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabelaEstoque.setRowFactory(view -> new TableRow<>() {
            @Override protected void updateItem(EstoqueLinha item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("estoque-zero", "estoque-baixo");
                if (!empty && item != null) getStyleClass().add(item.quantidade() == 0 ? "estoque-zero" :
                        item.quantidade() <= 3 ? "estoque-baixo" : "estoque-normal");
            }
        });
        TextField quantidade = campo("Quantidade");
        TextField localizacao = campo("LocalizaÃƒÂ§ÃƒÂ£o");
        Button atualizar = botao("Atualizar selecionado");
        Button excluirEstoque = botao("Excluir estoque");
        boolean podeEditarEstoque = SessionManager.atual().isOperador();
        atualizar.setDisable(!podeEditarEstoque);
        excluirEstoque.setDisable(!podeEditarEstoque);
        atualizar.setOnAction(event -> {
            EstoqueLinha linha = tabelaEstoque.getSelectionModel().getSelectedItem();
            if (linha == null) { mostrarErro("Selecione um item do estoque."); return; }
            try {
                int qtd = Integer.parseInt(quantidade.getText().trim());
                if (qtd < 0) throw new NumberFormatException();
                inventarioDAO.atualizarQuantidade(linha.componente().getId(), qtd, localizacao.getText().trim());
                registrar("ATUALIZAR", "INVENTARIO", "componente=" + linha.componente().getId());
                atualizarEstoque();
                mostrarInfo("Estoque atualizado.");
            } catch (NumberFormatException e) { mostrarErro("A quantidade deve ser um nÃƒÂºmero inteiro nÃƒÂ£o negativo."); }
        });
        excluirEstoque.setOnAction(event -> {
            EstoqueLinha linha=tabelaEstoque.getSelectionModel().getSelectedItem();
            if(linha==null || inventarioDAO.buscarPorComponente(linha.componente().getId())==null){mostrarErro("Selecione um registro de estoque.");return;}
            if(!confirmarExclusao("Excluir estoque","Remover o registro de estoque de "+linha.componente().getNome()+"?"))return;
            Inventario inventario = inventarioDAO.buscarPorComponente(linha.componente().getId());
            if (inventario != null) undoManager.push("DELETE_INVENTARIO", linha.componente().getId(), inventario);
            inventarioDAO.deletar(linha.componente().getId()); atualizarEstoque(); registrar("EXCLUIR","INVENTARIO","componente="+linha.componente().getId()); mostrarUndo("Estoque removido.");
        });
        tabelaEstoque.getSelectionModel().selectedItemProperty().addListener((obs, antigo, atual) -> {
            if (atual != null) { quantidade.setText(String.valueOf(atual.quantidade())); localizacao.setText(texto(atual.localizacao())); }
        });
        estoqueResumo = new Label();
        HBox formulario = new HBox(10, rotulo("Quantidade"), quantidade, rotulo("LocalizaÃƒÂ§ÃƒÂ£o"), localizacao, atualizar, excluirEstoque);
        VBox conteudo = new VBox(10, estoqueResumo, formulario, tabelaEstoque);
        conteudo.setPadding(new Insets(16));
        VBox.setVgrow(tabelaEstoque, Priority.ALWAYS);
        return new Tab("Estoque", conteudo);
    }

    private void atualizarEstoque() {
        if (estoqueResumo == null) return;
        estoque.clear();
        int total = 0, zerados = 0, baixo = 0;
        for (Componente componente : componenteDAO.listarTodos()) {
            Inventario item = inventarioDAO.buscarPorComponente(componente.getId());
            int quantidade = item == null ? 0 : item.quantidade();
            String local = item == null ? "" : item.localizacao();
            estoque.add(new EstoqueLinha(componente, quantidade, local));
            total++;
            if (quantidade == 0) zerados++;
            if (quantidade <= 3) baixo++;
        }
        estoqueResumo.setText(String.format("Total de componentes: %d  |  Fora de estoque: %d  |  Estoque baixo: %d",
                total, zerados, baixo));
    }

    private Tab criarAbaMonteSeuPc() {
        ComboBox<Componente> processador = comboComponentes("Selecione o processador");
        ComboBox<Componente> placaMae = comboComponentes("Selecione a placa-mÃƒÂ£e");
        ComboBox<Componente> memoria = comboComponentes("Selecione a memÃƒÂ³ria RAM");
        ComboBox<Componente> gpu = comboComponentes("Selecione a placa de vÃƒÂ­deo");
        atualizarComboCategoria(processador, "processador");
        atualizarComboCategoria(placaMae, "placa mae");
        atualizarComboCategoria(memoria, "memoria");
        atualizarComboCategoria(gpu, "placa de video");
        Label total = new Label("Total: R$ 0,00 | TDP: 0 W");
        Label tdpAtual = new Label("TDP atual: 0 W");
        Label psuRecomendado = new Label("PSU recomendado: -");
        psuRecomendado.getStyleClass().add("verdict");
        VBox calculadoraPsu = new VBox(6, new Label("Calculadora PSU"), tdpAtual, psuRecomendado);
        calculadoraPsu.getStyleClass().add("form-card");
        Label veredito = new Label("Selecione os componentes para verificar a configuraÃƒÂ§ÃƒÂ£o.");
        veredito.getStyleClass().add("verdict");
        VBox selecionadosPainel = new VBox(6);
        selecionadosPainel.getStyleClass().add("compatible-list");
        selecionadosPainel.getChildren().add(new Label("Os componentes selecionados aparecerÃƒÂ£o aqui."));
        Button verificar = botao("Verificar compatibilidade");
        Button exportar = botao("Exportar OrÃƒÂ§amento");
        verificar.setOnAction(event -> {
            List<Componente> selecionados = Arrays.asList(processador.getValue(), placaMae.getValue(),
                    memoria.getValue(), gpu.getValue());
            if (selecionados.stream().anyMatch(Objects::isNull)) {
                veredito.setText("Selecione processador, placa-mÃƒÂ£e, memÃƒÂ³ria RAM e placa de vÃƒÂ­deo.");
                return;
            }
            double preco = selecionados.stream().mapToDouble(Componente::getPreco).sum();
            double tdp = selecionados.stream().mapToDouble(Componente::getTdpWatts).sum();
            selecionadosPainel.getChildren().setAll(selecionados.stream()
                    .map(this::criarLinhaConfiguracao).toList());
            boolean ok = compatibilidadeEntre(processador.getValue(), placaMae.getValue(), true)
                    && compatibilidadeEntre(placaMae.getValue(), memoria.getValue(), false)
                    && compatibilidadeEntre(placaMae.getValue(), gpu.getValue(), false);
            total.setText(String.format("Total: R$ %.2f | TDP: %.0f W", preco, tdp));
            atualizarPsu(tdp, tdpAtual, psuRecomendado);
            String conflitos = montarDescricaoConflitos(processador.getValue(), placaMae.getValue(),
                    memoria.getValue(), gpu.getValue());
            veredito.setText(ok ? "ConfiguraÃƒÂ§ÃƒÂ£o compatÃƒÂ­vel!" : "Incompatibilidade detectada! " + conflitos);
            veredito.getStyleClass().removeAll("verdict-ok", "verdict-error");
            veredito.getStyleClass().add(ok ? "verdict-ok" : "verdict-error");
        });
        exportar.setOnAction(event -> {
            List<Componente> itens = Arrays.asList(processador.getValue(), placaMae.getValue(), memoria.getValue(), gpu.getValue());
            if (itens.stream().anyMatch(Objects::isNull)) { mostrarErro("Selecione todos os componentes antes de exportar."); return; }
            exportarOrcamento(itens);
        });
        GridPane campos = new GridPane();
        campos.setHgap(12); campos.setVgap(12); campos.setPadding(new Insets(20));
        campos.getStyleClass().add("form-card");
        campos.addRow(0, rotulo("Processador"), processador);
        campos.addRow(1, rotulo("Placa-mÃƒÂ£e"), placaMae);
        campos.addRow(2, rotulo("MemÃƒÂ³ria RAM"), memoria);
        campos.addRow(3, rotulo("Placa de vÃƒÂ­deo"), gpu);
        campos.getColumnConstraints().addAll(new ColumnConstraints(130), colunaExpansivel());
        Runnable atualizarPsuSelecionados = () -> {
            double tdp = Arrays.asList(processador.getValue(), placaMae.getValue(), memoria.getValue(), gpu.getValue()).stream()
                    .filter(Objects::nonNull).mapToDouble(Componente::getTdpWatts).sum();
            atualizarPsu(tdp, tdpAtual, psuRecomendado);
        };
        for (ComboBox<Componente> combo : List.of(processador, placaMae, memoria, gpu)) combo.setOnAction(e -> atualizarPsuSelecionados.run());
        VBox conteudo = new VBox(14, campos, new HBox(10, verificar, exportar), calculadoraPsu,
                new Label("Componentes selecionados"), selecionadosPainel, total, veredito);
        conteudo.setPadding(new Insets(20));
        return new Tab("Monte seu PC", conteudo);
    }

    private void atualizarPsu(double tdp, Label tdpAtual, Label recomendacao) {
        double alvo = tdp * 1.2;
        int recomendado = calcularPsuPadrao(alvo);
        tdpAtual.setText(String.format("TDP atual: %.0f W | com margem de 20%%: %.0f W", tdp, alvo));
        recomendacao.setText(String.format("PSU recomendado: %d W", recomendado));
        recomendacao.getStyleClass().removeAll("verdict-ok", "verdict-error");
        recomendacao.getStyleClass().add(tdp <= 0 ? "verdict-error" : recomendado >= alvo * 1.15 ? "verdict-ok" : "verdict-tight");
    }

    private int calcularPsuPadrao(double alvo) {
        int[] padroes = {450, 550, 650, 750, 850, 1000, 1200, 1500};
        for (int padrao : padroes) if (padrao >= alvo) return padrao;
        return (int) (Math.ceil(alvo / 100.0) * 100);
    }

    private boolean compatibilidadeEntre(Componente primeiro, Componente segundo, boolean usarSocket) {
        List<Compatibilidade> a = compatibilidadeDAO.listarPorComponente(primeiro.getId());
        List<Compatibilidade> b = compatibilidadeDAO.listarPorComponente(segundo.getId());
        if (a.isEmpty() || b.isEmpty()) return false;
        return a.stream().anyMatch(x -> b.stream().anyMatch(y ->
                usarSocket ? iguais(x.getSocketTipo(), y.getSocketTipo()) :
                        iguais(x.getPadrao(), y.getPadrao()) || iguais(x.getSocketTipo(), y.getSocketTipo())));
    }

    private HBox criarLinhaConfiguracao(Componente componente) {
        Label nome = new Label(componente.getNome() + " - " + componente.getModelo());
        Label preco = new Label(String.format("R$ %.2f", componente.getPreco()));
        HBox.setHgrow(nome, Priority.ALWAYS);
        HBox linha = new HBox(10, nome, preco);
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.getStyleClass().add("compatible-card");
        return linha;
    }

    private String montarDescricaoConflitos(Componente processador, Componente placaMae,
                                             Componente memoria, Componente gpu) {
        List<String> conflitos = new ArrayList<>();
        if (!compatibilidadeEntre(processador, placaMae, true)) {
            conflitos.add("processador e placa-mÃƒÂ£e");
        }
        if (!compatibilidadeEntre(placaMae, memoria, false)) {
            conflitos.add("placa-mÃƒÂ£e e memÃƒÂ³ria RAM");
        }
        if (!compatibilidadeEntre(placaMae, gpu, false)) {
            conflitos.add("placa-mÃƒÂ£e e placa de vÃƒÂ­deo");
        }
        return conflitos.isEmpty() ? "" : "Conflitos: " + String.join(", ", conflitos) + ".";
    }

    private void atualizarComboCategoria(ComboBox<Componente> combo, String termo) {
        combo.getItems().setAll(componentesPorCategoria(termo));
    }

    private List<Componente> componentesPorCategoria(String termo) {
        String chave = normalizar(termo);
        for (Categoria categoria : categoriaDAO.listarTodas()) {
            if (normalizar(categoria.getNome()).contains(chave)) return componenteDAO.listarPorCategoria(categoria.getId());
        }
        return List.of();
    }

    private ComboBox<Componente> comboComponentes(String prompt) {
        ComboBox<Componente> combo = new ComboBox<>(FXCollections.observableArrayList(componenteDAO.listarTodos()));
        combo.setPromptText(prompt);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setCellFactory(lista -> componenteCell());
        combo.setButtonCell(componenteCell());
        return combo;
    }

    private ListCell<Componente> componenteCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Componente item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNome() + " - " + texto(item.getFabricante()) + " " + item.getModelo());
            }
        };
    }

    private TableColumn<EstoqueLinha, ?> colunaEstoque(String titulo, java.util.function.Function<EstoqueLinha, ?> valor) {
        TableColumn<EstoqueLinha, Object> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(dado -> new ReadOnlyObjectWrapper<>(valor.apply(dado.getValue())));
        return coluna;
    }

    private HBox criarCartaoCompativel(Componente componente) {
        Label check = new Label("\u2713");
        check.getStyleClass().add("compatibility-check");
        Label nome = new Label(componente.getNome());
        nome.getStyleClass().add("compatible-name");
        Label detalhes = new Label(texto(componente.getFabricante()) + "  |  " + componente.getModelo());
        detalhes.getStyleClass().add("compatible-details");
        Label preco = new Label(String.format("R$ %.2f", componente.getPreco()));
        preco.getStyleClass().add("compatible-price");
        VBox textos = new VBox(3, nome, detalhes);
        HBox.setHgrow(textos, Priority.ALWAYS);
        HBox cartao = new HBox(12, check, textos, preco);
        cartao.setAlignment(Pos.CENTER_LEFT);
        cartao.getStyleClass().add("compatible-card");
        return cartao;
    }

    private Tab criarAbaUsuarios() {
        TableView<Usuario> tabelaUsuarios = new TableView<>();
        tabelaUsuarios.setItems(FXCollections.observableArrayList(usuarioDAO.listarTodos()));
        tabelaUsuarios.getColumns().setAll(
                colunaUsuario("UsuÃƒÂ¡rio", Usuario::username), colunaUsuario("Nome", Usuario::nome),
                colunaUsuario("NÃƒÂ­vel", Usuario::nivel), colunaUsuario("Ativo", u -> u.ativo() ? "Sim" : "NÃƒÂ£o"));
        TextField username=campo("UsuÃƒÂ¡rio"), nome=campo("Nome"), senha=campo("Senha");
        ComboBox<String> nivel=new ComboBox<>(FXCollections.observableArrayList("ADMIN","OPERADOR","USUARIO")); nivel.setValue("USUARIO");
        Button salvar=botao("Salvar usuÃƒÂ¡rio"), excluir=botao("Excluir selecionado");
        salvar.setOnAction(e->{if(username.getText().isBlank()||nome.getText().isBlank()||senha.getText().isBlank()){mostrarErro("Preencha usuÃƒÂ¡rio, nome e senha.");return;}try{usuarioDAO.salvar(new Usuario(0,username.getText().trim(),nome.getText().trim(),senha.getText(),nivel.getValue(),true));tabelaUsuarios.getItems().setAll(usuarioDAO.listarTodos());registrar("CRIAR","USUARIO",username.getText());mostrarInfo("UsuÃƒÂ¡rio salvo.");}catch(RuntimeException ex){mostrarErro(ex.getMessage());}});
        excluir.setOnAction(e->{Usuario u=tabelaUsuarios.getSelectionModel().getSelectedItem();if(u==null)return;if(!confirmarExclusao("Excluir usuÃƒÂ¡rio","Deseja remover "+u.username()+"?"))return;usuarioDAO.excluir(u.id());tabelaUsuarios.getItems().setAll(usuarioDAO.listarTodos());registrar("EXCLUIR","USUARIO",u.username());});
        tabelaUsuarios.getSelectionModel().selectedItemProperty().addListener((o,a,u)->{if(u!=null){username.setText(u.username());nome.setText(u.nome());senha.setText(u.senha());nivel.setValue(u.nivel());}});
        HBox form=new HBox(8,username,nome,senha,nivel,salvar,excluir);
        VBox box=new VBox(12,form,tabelaUsuarios);box.setPadding(new Insets(18));VBox.setVgrow(tabelaUsuarios,Priority.ALWAYS);
        return new Tab("UsuÃƒÂ¡rios",box);
    }
    private <T> TableColumn<Usuario,T> colunaUsuario(String title, java.util.function.Function<Usuario,T> fn) {
        TableColumn<Usuario,T> c=new TableColumn<>(title); c.setCellValueFactory(v->new ReadOnlyObjectWrapper<>(fn.apply(v.getValue()))); return c;
    }

    private Tab criarAbaLogs() {
        TextField filtro=campo("Filtrar por usuÃƒÂ¡rio, aÃƒÂ§ÃƒÂ£o ou entidade...");
        TableView<LogEntry> tabelaLogs=new TableView<>();
        tabelaLogs.getColumns().setAll(colunaLog("Data",l->l.dataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))),
                colunaLog("UsuÃƒÂ¡rio",LogEntry::usuario),colunaLog("AÃƒÂ§ÃƒÂ£o",LogEntry::acao),colunaLog("Entidade",LogEntry::entidade),colunaLog("Detalhes",LogEntry::detalhes));
        Runnable atualizar=()->tabelaLogs.getItems().setAll(logDAO.listar(filtro.getText()));
        filtro.textProperty().addListener((o,a,b)->atualizar.run()); atualizar.run();
        Button csv=botao("Exportar CSV");
        csv.setOnAction(e->{try{Path desktop=Path.of(System.getProperty("user.home"),"Desktop");Files.createDirectories(desktop);Path arq=desktop.resolve("nexus-logs-"+LocalDate.now()+".csv");StringBuilder out=new StringBuilder("data,usuario,acao,entidade,detalhes\n");for(LogEntry l:logDAO.listar(filtro.getText()))out.append('"').append(l.dataHora()).append("\",\"").append(l.usuario()).append("\",\"").append(l.acao()).append("\",\"").append(l.entidade()).append("\",\"").append(l.detalhes()).append("\"\n");Files.writeString(arq,out);mostrarInfo("Logs exportados em: "+arq);}catch(IOException ex){mostrarErro("Falha ao exportar logs: "+ex.getMessage());}});
        VBox box=new VBox(10,new HBox(8,filtro,csv),tabelaLogs);box.setPadding(new Insets(18));VBox.setVgrow(tabelaLogs,Priority.ALWAYS);
        return new Tab("Logs de atividade",box);
    }
    private <T> TableColumn<LogEntry,T> colunaLog(String title, java.util.function.Function<LogEntry,T> fn) {
        TableColumn<LogEntry,T> c=new TableColumn<>(title);c.setCellValueFactory(v->new ReadOnlyObjectWrapper<>(fn.apply(v.getValue())));return c;
    }

    private Tab criarAbaGarantia() {
        TableView<Garantia> tabelaGarantias = new TableView<>();
        tabelaGarantias.setPlaceholder(new Label("Nenhuma garantia cadastrada."));
        ComboBox<Componente> componente = comboComponentes("Componente");
        TextField tipo = campo("Tipo de garantia");
        DatePicker inicio = new DatePicker(LocalDate.now());
        DatePicker fim = new DatePicker(LocalDate.now().plusYears(1));
        ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList("Ativa", "Vencida", "Cancelada"));
        status.setValue("Ativa");
        TextArea notas = new TextArea(); notas.setPromptText("Notas"); notas.setPrefRowCount(2);
        Button salvar = botao("Salvar garantia");
        Button excluir = botao("Excluir selecionada");
        Button exportar = botao("Exportar PDF");
        boolean podeEditar = SessionManager.admin() || SessionManager.atual().isOperador();
        salvar.setDisable(!podeEditar); excluir.setDisable(!podeEditar);
        tabelaGarantias.getColumns().setAll(
                colunaGarantia("ID", Garantia::getId),
                colunaGarantia("Componente", g -> nomeComponente(g.getComponenteId())),
                colunaGarantia("Tipo", Garantia::getTipo),
                colunaGarantia("Data Inicio", g -> g.getDataInicio().toString()),
                colunaGarantia("Data Fim", g -> g.getDataFim().toString()),
                colunaGarantia("Status", Garantia::getStatus),
                colunaGarantia("Notas", g -> texto(g.getNotas())));
        tabelaGarantias.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabelaGarantias.setRowFactory(view -> new TableRow<>() {
            @Override protected void updateItem(Garantia item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("garantia-expirando");
                if (!empty && item != null && expiraEmBreve(item)) getStyleClass().add("garantia-expirando");
            }
        });
        int[] idEdicao = {-1};
        Runnable carregar = () -> {
            tabelaGarantias.getItems().setAll(garantiaDAO.listarTodas());
            componente.getItems().setAll(componenteDAO.listarTodos());
        };
        salvar.setOnAction(e -> {
            if (componente.getValue() == null || tipo.getText().isBlank() || inicio.getValue() == null || fim.getValue() == null) {
                mostrarErro("Preencha componente, tipo e datas da garantia."); return;
            }
            if (fim.getValue().isBefore(inicio.getValue())) { mostrarErro("A data final deve ser posterior Ã¯Â¿Â½ inicial."); return; }
            Garantia garantia = new Garantia(idEdicao[0],
                    componente.getValue().getId(), tipo.getText().trim(), inicio.getValue(), fim.getValue(), status.getValue(), notas.getText().trim());
            if (idEdicao[0] < 0) { garantiaDAO.inserir(garantia); registrar("CRIAR", "GARANTIA", tipo.getText().trim()); }
            else { garantiaDAO.atualizar(garantia); registrar("ATUALIZAR", "GARANTIA", "id=" + idEdicao[0]); }
            idEdicao[0] = -1; carregar.run(); limparGarantiaForm(componente, tipo, inicio, fim, status, notas); mostrarInfo("Garantia salva.");
        });
        tabelaGarantias.getSelectionModel().selectedItemProperty().addListener((obs, antigo, atual) -> {
            if (atual == null) return;
            idEdicao[0] = atual.getId(); componente.setValue(componenteDAO.buscarPorId(atual.getComponenteId()));
            tipo.setText(atual.getTipo()); inicio.setValue(atual.getDataInicio()); fim.setValue(atual.getDataFim());
            status.setValue(atual.getStatus()); notas.setText(atual.getNotas());
        });
        excluir.setOnAction(e -> {
            Garantia atual = tabelaGarantias.getSelectionModel().getSelectedItem();
            if (atual == null || !confirmarExclusao("Excluir garantia", "Remover a garantia selecionada?")) return;
            undoManager.push("DELETE_GARANTIA", atual.getComponenteId(), atual);
            garantiaDAO.deletar(atual.getId()); registrar("EXCLUIR", "GARANTIA", "id=" + atual.getId()); carregar.run();
            mostrarUndo("Garantia removida.");
        });
        exportar.setOnAction(e -> exportarGarantiasPdf(tabelaGarantias.getItems()));
        carregar.run();
        GridPane formulario = new GridPane(); formulario.setHgap(8); formulario.setVgap(8); formulario.getStyleClass().add("form-card");
        formulario.addRow(0, rotulo("Componente"), componente, rotulo("Tipo"), tipo);
        formulario.addRow(1, rotulo("Data Inicio"), inicio, rotulo("Data Fim"), fim);
        formulario.addRow(2, rotulo("Status"), status, rotulo("Notas"), notas);
        VBox conteudo = new VBox(10, formulario, new HBox(8, salvar, excluir, exportar),
                new Label("Garantias que vencem em menos de 30 dias sao destacadas."), tabelaGarantias);
        conteudo.setPadding(new Insets(16)); VBox.setVgrow(tabelaGarantias, Priority.ALWAYS);
        Tab tab = new Tab("Garantia", conteudo);
        tab.setOnSelectionChanged(e -> { if (tab.isSelected()) carregar.run(); });
        return tab;
    }

    private <T> TableColumn<Garantia, T> colunaGarantia(String titulo, java.util.function.Function<Garantia, T> valor) {
        TableColumn<Garantia, T> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(dado -> new ReadOnlyObjectWrapper<>(valor.apply(dado.getValue())));
        return coluna;
    }

    private String nomeComponente(int id) {
        Componente componente = componenteDAO.buscarPorId(id);
        return componente == null ? "#" + id : componente.getNome() + " - " + componente.getModelo();
    }

    private boolean expiraEmBreve(Garantia garantia) {
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), garantia.getDataFim());
        return "Ativa".equalsIgnoreCase(garantia.getStatus()) && dias >= 0 && dias < 30;
    }

    private void limparGarantiaForm(ComboBox<Componente> componente, TextField tipo, DatePicker inicio,
                                    DatePicker fim, ComboBox<String> status, TextArea notas) {
        componente.getSelectionModel().clearSelection(); tipo.clear(); inicio.setValue(LocalDate.now());
        fim.setValue(LocalDate.now().plusYears(1)); status.setValue("Ativa"); notas.clear();
    }

    private void exportarGarantiasPdf(List<Garantia> garantias) {
        try {
            Path desktop = Path.of(System.getProperty("user.home"), "Desktop"); Files.createDirectories(desktop);
            Path arquivo = desktop.resolve("nexus-garantias-" + LocalDate.now() + ".pdf");
            try (PdfWriter writer = new PdfWriter(arquivo.toString()); PdfDocument pdf = new PdfDocument(writer); Document documento = new Document(pdf)) {
                documento.add(new Paragraph("Nexus Studio - Garantias").setBold());
                Table tabelaPdf = new Table(UnitValue.createPercentArray(new float[]{1, 3, 2, 2, 2, 2, 3})).useAllAvailableWidth();
                for (String cabecalho : new String[]{"ID", "Componente", "Tipo", "Data Inicio", "Data Fim", "Status", "Notas"}) tabelaPdf.addHeaderCell(cabecalho);
                for (Garantia garantia : garantias) for (String valor : new String[]{String.valueOf(garantia.getId()), nomeComponente(garantia.getComponenteId()), garantia.getTipo(), garantia.getDataInicio().toString(), garantia.getDataFim().toString(), garantia.getStatus(), texto(garantia.getNotas())}) tabelaPdf.addCell(valor);
                documento.add(tabelaPdf);
            }
            registrar("EXPORTAR", "GARANTIA", arquivo.toString()); mostrarInfo("Garantias exportadas em: " + arquivo);
        } catch (Exception e) { mostrarErro("Nao foi possivel gerar o PDF: " + e.getMessage()); }
    }
    private Tab criarAbaConfiguracoes() {
        Button backup = botao("Fazer backup agora");
        Label status = new Label("Backups automaticos: retencao dos 10 mais recentes.");
        backup.setOnAction(e -> { DatabaseManager.criarBackup(); registrar("BACKUP", "BANCO", "Backup manual"); status.setText("Backup criado em " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))); mostrarInfo("Backup realizado com sucesso."); });
        CheckBox tema = new CheckBox("Usar tema escuro"); tema.setSelected(temaEscuro);
        tema.setOnAction(e -> { temaEscuro = tema.isSelected(); PreferencesManager.setDarkTheme(temaEscuro); aplicarEstilo(cenaPrincipal); });
        Slider fonte = new Slider(10, 16, PreferencesManager.fontSize());
        fonte.setMajorTickUnit(2); fonte.setMinorTickCount(0); fonte.setShowTickLabels(true); fonte.setShowTickMarks(true); fonte.setBlockIncrement(1);
        Label fonteValor = new Label(String.format("Tamanho da fonte: %.0f px", fonte.getValue()));
        fonte.valueProperty().addListener((obs, antigo, atual) -> { double valor = Math.round(atual.doubleValue()); fonte.setValue(valor); PreferencesManager.setFontSize(valor); fonteValor.setText(String.format("Tamanho da fonte: %.0f px", valor)); if (cenaPrincipal != null) aplicarTamanhoFonte(cenaPrincipal, valor); });
        HBox presets = new HBox(8);
        for (Map.Entry<String, Double> preset : Map.of("Pequena", 10d, "Normal", 12d, "Grande", 14d, "XLarge", 16d).entrySet()) {
            Button botao = new Button(preset.getKey()); botao.setOnAction(e -> fonte.setValue(preset.getValue())); presets.getChildren().add(botao);
        }
        VBox box = new VBox(16, new Label("Configuracoes administrativas"), tema, fonteValor, fonte, presets, backup, status);
        box.setPadding(new Insets(24));
        return new Tab("Configuracoes", box);
    }
    private void configurarAtualizacaoAba(Tab tab, Runnable atualizar) {
        tab.setOnSelectionChanged(event -> { if (tab.isSelected()) atualizar.run(); });
    }

    private void aplicarTamanhoFonte(Scene cena, double tamanho) {
        if (cena != null) cena.getRoot().setStyle("-fx-font-size: " + tamanho + "px;");
    }

    private void configurarAtalhos(Scene cena) {
        cena.getAccelerators().put(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.N, javafx.scene.input.KeyCombination.CONTROL_DOWN), () -> selecionarAba("Cadastrar"));
        cena.getAccelerators().put(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.F, javafx.scene.input.KeyCombination.CONTROL_DOWN), () -> {if(buscaComponentes!=null){selecionarAba("Componentes");buscaComponentes.requestFocus();}});
        cena.getAccelerators().put(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.R, javafx.scene.input.KeyCombination.CONTROL_DOWN), () -> atualizarTabela(componenteDAO.listarTodos()));
        cena.getAccelerators().put(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.E, javafx.scene.input.KeyCombination.CONTROL_DOWN), this::exportarExcel);
        cena.getAccelerators().put(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.S, javafx.scene.input.KeyCombination.CONTROL_DOWN), () -> mostrarInfo("Use os botÃƒÂµes Salvar para confirmar alteraÃƒÂ§ÃƒÂµes."));
        cena.getAccelerators().put(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.Z, javafx.scene.input.KeyCombination.CONTROL_DOWN), this::desfazerUltimaAcao);
        cena.getAccelerators().put(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.F1), () -> mostrarInfo("Atalhos: Ctrl+N novo, Ctrl+F buscar, Ctrl+R/F5 atualizar, Ctrl+E Excel, Delete excluir, Esc limpar, Ctrl+S salvar."));
        cena.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED,e->{if(e.getCode()==javafx.scene.input.KeyCode.F5){atualizarTabela(componenteDAO.listarTodos());e.consume();}else if(e.getCode()==javafx.scene.input.KeyCode.DELETE){excluirSelecionado();e.consume();}else if(e.getCode()==javafx.scene.input.KeyCode.ESCAPE){if(buscaComponentes!=null)buscaComponentes.clear();tabela.getSelectionModel().clearSelection();}});
    }
    private void selecionarAba(String titulo) {if(abas==null)return;for(int i=0;i<abas.getTabs().size();i++)if(abas.getTabs().get(i).getText().equals(titulo)){abas.getSelectionModel().select(i);return;}}

    private String formatarPadrao(String padrao) { return padrao == null || padrao.isBlank() ? "" : "  |  " + padrao; }

    private VBox painel(Control... controles) {
        VBox painel = new VBox(12, controles);
        painel.setPadding(new Insets(16));
        painel.getStyleClass().add("content-panel");
        VBox.setVgrow(tabela, Priority.ALWAYS);
        return painel;
    }

    private VBox formularioComBotao(GridPane formulario, Button botao) {
        VBox conteudo = new VBox(10, formulario, botao);
        conteudo.setPadding(new Insets(10));
        conteudo.getStyleClass().add("content-panel");
        return conteudo;
    }

    private void configurarTabela(TableView<Componente> tabela, String vazio) {
        tabela.setItems(FXCollections.observableArrayList());
        tabela.getColumns().setAll(criarColunas());
        tabela.setPlaceholder(mensagemVazia(vazio));
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setRowFactory(view -> new TableRow<>() {
            @Override protected void updateItem(Componente item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("even-row", "odd-row");
                if (!empty && item != null) getStyleClass().add(getIndex() % 2 == 0 ? "even-row" : "odd-row");
            }
        });
        tabela.getItems().addListener((javafx.collections.ListChangeListener<Componente>) change ->
                ajustarAlturaTabela(tabela));
        ajustarAlturaTabela(tabela);
    }

    private TableView<Componente> criarTabela(String vazio) {
        TableView<Componente> resultado = new TableView<>();
        resultado.setItems(FXCollections.observableArrayList());
        resultado.getColumns().setAll(criarColunas());
        resultado.setPlaceholder(mensagemVazia(vazio));
        resultado.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        resultado.getItems().addListener((javafx.collections.ListChangeListener<Componente>) change ->
                ajustarAlturaTabela(resultado));
        ajustarAlturaTabela(resultado);
        return resultado;
    }

    private Label mensagemVazia(String texto) {
        Label mensagem = new Label(texto);
        mensagem.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        mensagem.setAlignment(Pos.CENTER);
        mensagem.getStyleClass().add("empty-state");
        return mensagem;
    }

    private void ajustarAlturaTabela(TableView<?> tabela) {
        double altura = 38 + Math.max(1, tabela.getItems().size()) * 40;
        tabela.setPrefHeight(altura);
        tabela.setMaxHeight(altura);
    }

    private List<TableColumn<Componente, ?>> criarColunas() {
        TableColumn<Componente, Number> id = coluna("ID", Componente::getId);
        TableColumn<Componente, String> nome = coluna("Nome", Componente::getNome);
        TableColumn<Componente, String> fabricante = coluna("Fabricante", c -> texto(c.getFabricante()));
        TableColumn<Componente, String> modelo = coluna("Modelo", Componente::getModelo);
        TableColumn<Componente, Number> preco = coluna("PreÃƒÂ§o", Componente::getPreco);
        TableColumn<Componente, Number> tdp = coluna("TDP (W)", Componente::getTdpWatts);
        return List.of(id, nome, fabricante, modelo, preco, tdp);
    }

    private <T> TableColumn<Componente, T> coluna(String titulo, java.util.function.Function<Componente, T> valor) {
        TableColumn<Componente, T> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(dado -> new ReadOnlyObjectWrapper<>(valor.apply(dado.getValue())));
        return coluna;
    }

    private List<Componente> filtrar(String tipo, String valor1, String valor2) {
        return switch (tipo) {
            case "Faixa de preÃƒÂ§o" -> componenteDAO.filtrarPorPreco(numero(valor1), numero(valor2));
            case "TDP mÃƒÂ¡ximo" -> componenteDAO.filtrarPorTdpMaximo(numero(valor1));
            case "Fabricante" -> componenteDAO.buscarPorFabricante(valor1.trim());
            default -> componenteDAO.listarTodos();
        };
    }

    private double numero(String valor) { return valor == null || valor.isBlank() ? 0 : Double.parseDouble(valor.replace(',', '.').trim()); }
    private double numero(TextField campo) { return numero(campo.getText()); }
    private TextField campo(String prompt) { TextField campo = new TextField(); campo.setPromptText(prompt); campo.setPrefWidth(220); return campo; }
    private Label rotulo(String texto) { Label rotulo = new Label(texto); rotulo.getStyleClass().add("field-label"); return rotulo; }
    private Button botao(String texto) { Button botao = new Button(texto); botao.setMinWidth(140); botao.setPrefHeight(34); botao.getStyleClass().add("primary-button"); return botao; }
    private ColumnConstraints colunaExpansivel() { ColumnConstraints c = new ColumnConstraints(); c.setHgrow(Priority.ALWAYS); return c; }

    private void carregarCategorias() {
        categoriaCombo.getItems().setAll(categoriaDAO.listarTodas());
        categoriaCombo.setCellFactory(lista -> categoriaCell());
        categoriaCombo.setButtonCell(categoriaCell());
    }

    private ListCell<Categoria> categoriaCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Categoria item, boolean empty) { super.updateItem(item, empty); setText(empty || item == null ? null : item.getNome()); }
        };
    }

    private void atualizarTabela(List<Componente> lista) {
        componentes.setAll(lista);
        System.out.println("Loaded " + lista.size() + " components into the table");
        aplicarBuscaComponentes();
        if (statusLabel != null) statusLabel.setText(lista.size() + (lista.size() == 1 ? " componente no banco" : " componentes no banco"));
        if (historicoCombo != null) {
            Componente selecionado = historicoCombo.getValue();
            historicoCombo.getItems().setAll(lista);
            if (selecionado != null) {
                for (int i = 0; i < lista.size(); i++) {
                    if (lista.get(i).getId() == selecionado.getId()) {
                        historicoCombo.getSelectionModel().select(i);
                        break;
                    }
                }
            }
        }
    }

    private void selecionarPorId(int id) {
        for (int i = 0; i < tabela.getItems().size(); i++) if (tabela.getItems().get(i).getId() == id) {
            tabela.getSelectionModel().select(i); tabela.scrollTo(i); break;
        }
    }

    private void excluirSelecionado() {
        Componente selecionado = tabela.getSelectionModel().getSelectedItem();
        if (selecionado == null) { mostrarErro("Selecione um componente para excluir."); return; }
        if (!confirmarExclusao("Excluir componente", "Deseja excluir \"" + selecionado.getNome() + "\"?")) return;
        undoManager.push("DELETE_COMPONENTE", selecionado.getId(), selecionado);
        componenteDAO.deletar(selecionado.getId());
        registrar("EXCLUIR", "COMPONENTE", "id=" + selecionado.getId());
        atualizarTabela(componenteDAO.listarTodos());
        atualizarEstoque();
        atualizarDashboard();
        mostrarUndo("Componente removido.");
    }

    private void desfazerUltimaAcao() {
        Optional<UndoManager.UndoAction> acao = undoManager.pop();
        if (acao.isEmpty()) { mostrarInfo("Nenhuma acao pode ser desfeita."); return; }
        Object dados = acao.get().getData();
        switch (acao.get().getType()) {
            case "DELETE_COMPONENTE" -> componenteDAO.inserir((Componente) dados);
            case "DELETE_COMPATIBILIDADE" -> compatibilidadeDAO.inserir((Compatibilidade) dados);
            case "DELETE_ESPECIFICACAO" -> especificacaoDAO.inserir((Especificacao) dados);
            case "DELETE_INVENTARIO" -> inventarioDAO.inserir((Inventario) dados);
            case "DELETE_GARANTIA" -> garantiaDAO.inserir((Garantia) dados);
            default -> { mostrarErro("Tipo de undo desconhecido: " + acao.get().getType()); return; }
        }
        registrar("DESFAZER", acao.get().getType(), "id=" + acao.get().getComponenteId());
        atualizarTabela(componenteDAO.listarTodos()); atualizarEstoque();
        mostrarInfo("Acao desfeita.");
    }

    private void mostrarUndo(String mensagem) {
        if (toasts != null) toasts.showUndo(mensagem, this::desfazerUltimaAcao); else System.out.println(mensagem);
    }

    private void exportarComponentesPdf(List<Componente> lista, String titulo) {
        if (lista.isEmpty()) { mostrarErro("NÃƒÂ£o hÃƒÂ¡ componentes para exportar."); return; }
        try {
            Path desktop = Path.of(System.getProperty("user.home"), "Desktop");
            Files.createDirectories(desktop);
            String prefixo = titulo.startsWith("configuraÃƒÂ§ÃƒÂ£o") ? "nexus-configuracao-" : "nexus-relatorio-";
            Path arquivo = desktop.resolve(prefixo + LocalDate.now() + ".pdf");
            try (PdfWriter writer = new PdfWriter(arquivo.toString());
                 PdfDocument pdf = new PdfDocument(writer);
                 Document documento = new Document(pdf)) {
                documento.add(new Paragraph("Nexus Studio Ã¢â‚¬â€ RelatÃƒÂ³rio de Componentes"));
                documento.add(new Paragraph(titulo));
                documento.add(new Paragraph("Gerado em " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
                if (titulo.startsWith("configuraÃƒÂ§ÃƒÂ£o")) {
                    double totalPreco = lista.stream().mapToDouble(Componente::getPreco).sum();
                    double totalTdp = lista.stream().mapToDouble(Componente::getTdpWatts).sum();
                    documento.add(new Paragraph(String.format("Total: R$ %.2f | TDP total: %.0f W",
                            totalPreco, totalTdp)));
                }

                Table tabelaPdf = new Table(UnitValue.createPercentArray(new float[]{1, 3, 2, 2, 2, 1})).useAllAvailableWidth();
                for (String cabecalho : new String[]{"ID", "Nome", "Fabricante", "Modelo", "PreÃƒÂ§o (R$)", "TDP (W)"}) tabelaPdf.addHeaderCell(new Cell().add(new Paragraph(cabecalho)));
                for (Componente c : lista) {
                    tabelaPdf.addCell(String.valueOf(c.getId())); tabelaPdf.addCell(c.getNome());
                    tabelaPdf.addCell(texto(c.getFabricante())); tabelaPdf.addCell(c.getModelo());
                    tabelaPdf.addCell(String.format("%.2f", c.getPreco())); tabelaPdf.addCell(String.format("%.0f", c.getTdpWatts()));
                }
                documento.add(tabelaPdf);
            }
            mostrarInfo("PDF salvo em: " + arquivo);
        } catch (Exception e) { mostrarErro("NÃƒÂ£o foi possÃƒÂ­vel gerar o PDF: " + e.getMessage()); }
    }

    private void exportarExcel() {
        try {
            Path desktop = Path.of(System.getProperty("user.home"), "Desktop");
            Files.createDirectories(desktop);
            Path arquivo = desktop.resolve("nexus-studio-" + LocalDate.now() + ".xlsx");
            try (Workbook workbook = new XSSFWorkbook()) {
                CellStyle cabecalho = workbook.createCellStyle();
                Font fonte = workbook.createFont(); fonte.setBold(true); fonte.setColor(IndexedColors.WHITE.getIndex());
                cabecalho.setFont(fonte); cabecalho.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
                cabecalho.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                criarPlanilhaComponentes(workbook, cabecalho);
                criarPlanilhaEstoque(workbook, cabecalho);
                criarPlanilhaEspecificacoes(workbook, cabecalho);
                try (var out = Files.newOutputStream(arquivo)) { workbook.write(out); }
            }
            registrar("EXPORTAR", "EXCEL", arquivo.toString());
            mostrarInfo("Excel exportado em: " + arquivo);
        } catch (IOException e) { mostrarErro("NÃƒÂ£o foi possÃƒÂ­vel gerar o Excel: " + e.getMessage()); }
    }

    private void criarPlanilhaComponentes(Workbook w, CellStyle style) {
        Sheet s = w.createSheet("Componentes");
        String[] h = {"ID","Categoria","Nome","Fabricante","Modelo","PreÃƒÂ§o (R$)","TDP (W)"};
        Row r = s.createRow(0); for (int i=0;i<h.length;i++) { r.createCell(i).setCellValue(h[i]); r.getCell(i).setCellStyle(style); }
        Map<Integer,String> cats = categoriaDAO.listarTodas().stream().collect(Collectors.toMap(Categoria::getId, Categoria::getNome));
        int row = 1;
        for (Componente c : componenteDAO.listarTodos()) {
            Row x=s.createRow(row++); Object[] v={c.getId(),cats.getOrDefault(c.getCategoriaId(),""),c.getNome(),c.getFabricante(),c.getModelo(),c.getPreco(),c.getTdpWatts()};
            for(int i=0;i<v.length;i++) if(v[i] instanceof Number n)x.createCell(i).setCellValue(n.doubleValue()); else x.createCell(i).setCellValue(String.valueOf(v[i]));
        }
        ajustarColunas(s,h.length);
    }
    private void criarPlanilhaEstoque(Workbook w, CellStyle style) {
        Sheet s=w.createSheet("Estoque"); String[] h={"Componente","Modelo","Quantidade","LocalizaÃƒÂ§ÃƒÂ£o"};
        Row r=s.createRow(0); for(int i=0;i<h.length;i++){r.createCell(i).setCellValue(h[i]);r.getCell(i).setCellStyle(style);}
        int row=1; for(Componente c:componenteDAO.listarTodos()){Inventario i=inventarioDAO.buscarPorComponente(c.getId());Row x=s.createRow(row++);x.createCell(0).setCellValue(c.getNome());x.createCell(1).setCellValue(c.getModelo());x.createCell(2).setCellValue(i==null?0:i.quantidade());x.createCell(3).setCellValue(i==null?"":texto(i.localizacao()));}
        ajustarColunas(s,h.length);
    }
    private void criarPlanilhaEspecificacoes(Workbook w, CellStyle style) {
        Sheet s=w.createSheet("Especificacoes"); String[] h={"Componente","Chave","Valor","Unidade"};
        Row r=s.createRow(0); for(int i=0;i<h.length;i++){r.createCell(i).setCellValue(h[i]);r.getCell(i).setCellStyle(style);}
        int row=1;
        for (Componente c : componenteDAO.listarTodos()) {
            for (Especificacao x : especificacaoDAO.listarPorComponente(c.getId())) {
                Row z=s.createRow(row++);
                z.createCell(0).setCellValue(c.getNome());
                z.createCell(1).setCellValue(x.getChave());
                z.createCell(2).setCellValue(x.getValor());
                z.createCell(3).setCellValue(texto(x.getUnidade()));
            }
        }
        ajustarColunas(s,h.length);
    }
    private void ajustarColunas(Sheet sheet, int count) { for(int i=0;i<count;i++) sheet.autoSizeColumn(i); }

    private void exportarOrcamento(List<Componente> itens) {
        if (itens.stream().anyMatch(Objects::isNull)) { mostrarErro("Selecione todos os componentes antes de exportar."); return; }
        try {
            Path desktop=Path.of(System.getProperty("user.home"),"Desktop"); Files.createDirectories(desktop);
            Path arquivo=desktop.resolve("nexus-orcamento-"+LocalDate.now()+".pdf");
            try(PdfWriter writer=new PdfWriter(arquivo.toString()); PdfDocument pdf=new PdfDocument(writer); Document doc=new Document(pdf)){
                doc.add(new Paragraph("NEXUS STUDIO").setBold().setFontSize(22));
                doc.add(new Paragraph("OrÃƒÂ§amento personalizado de computador").setFontSize(15));
                doc.add(new Paragraph("Emitido em "+LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
                Table tabela=new Table(UnitValue.createPercentArray(new float[]{4,2,2})).useAllAvailableWidth();
                tabela.addHeaderCell("Componente"); tabela.addHeaderCell("Modelo"); tabela.addHeaderCell("PreÃƒÂ§o");
                for(Componente c:itens){tabela.addCell(c.getNome());tabela.addCell(c.getModelo());tabela.addCell(String.format("R$ %.2f",c.getPreco()));}
                doc.add(tabela);
                doc.add(new Paragraph(String.format("TOTAL: R$ %.2f",itens.stream().mapToDouble(Componente::getPreco).sum())).setBold().setFontSize(16));
                doc.add(new Paragraph("Valores sujeitos a alteraÃƒÂ§ÃƒÂ£o. Este documento ÃƒÂ© um orÃƒÂ§amento sem compromisso."));
            }
            registrar("EXPORTAR","ORCAMENTO",arquivo.toString()); mostrarInfo("OrÃƒÂ§amento salvo em: "+arquivo);
        } catch(Exception e){ mostrarErro("NÃƒÂ£o foi possÃƒÂ­vel gerar o orÃƒÂ§amento: "+e.getMessage()); }
    }


    private void aplicarEstilo(Scene cena) {
        String cssName = temaEscuro ? "dark.css" : "light.css";
        java.net.URL css = getClass().getResource("/styles/" + cssName);
        if (css == null) throw new IllegalStateException("Folha de estilo nao encontrada: " + cssName);
        cena.getStylesheets().clear();
        cena.getStylesheets().add(css.toExternalForm());
        cena.getRoot().pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("dark"), temaEscuro);
        aplicarTamanhoFonte(cena, PreferencesManager.fontSize());
    }

    private String texto(String valor) { return valor == null || valor.isBlank() ? "NÃƒÂ£o informado" : valor; }
    private boolean iguais(String a, String b) { return a != null && b != null && !a.isBlank() && a.equalsIgnoreCase(b); }
    private String normalizar(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
    }
    private void mostrarErro(String mensagem) {
        if (toasts != null) toasts.show(mensagem, true); else System.err.println(mensagem);
    }
    private void mostrarInfo(String mensagem) {
        if (toasts != null) toasts.show(mensagem, false); else System.out.println(mensagem);
    }
    private boolean confirmarExclusao(String titulo, String mensagem) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(titulo);
        dialog.setHeaderText("ConfirmaÃƒÂ§ÃƒÂ£o necessÃƒÂ¡ria");
        Label texto = new Label(mensagem);
        texto.setWrapText(true);
        texto.getStyleClass().add("delete-message");
        dialog.getDialogPane().setContent(texto);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.getDialogPane().getStylesheets().add("data:text/css,.delete-message{-fx-padding:12px;-fx-font-size:14px;}");
        return dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
    private void registrar(String acao, String entidade, String detalhes) {
        logDAO.registrar(SessionManager.nome(), acao, entidade, detalhes);
    }
    private record EstoqueLinha(Componente componente, int quantidade, String localizacao) {}
    private record ComparacaoLinha(String caracteristica, String primeiro, String segundo) {}

    public static void main(String[] args) { launch(args); }
}
