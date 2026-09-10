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
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
import java.io.InputStreamReader;
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
import org.kordamp.ikonli.javafx.FontIcon;

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
    private final ObservableList<Componente> componentesPagina = FXCollections.observableArrayList();
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
    private FlowPane cartoesComponentes;
    private StackPane visualizacaoComponentes;
    private boolean mostrarCartoes;
    private Label breadcrumb;
    private StackPane buscaOverlay;
    private TextField buscaGlobal;
    private ListView<Componente> resultadosBusca;

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
        configurarIconeAbas(dashboard, componentesTab, cadastroTab, relatoriosTab, compatibilidadeTab,
                historicoTab, estoqueTab, monteSeuPcTab, compararTab, garantiaTab);
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
            Tab usuarios = criarAbaUsuarios();
            Tab logs = criarAbaLogs();
            Tab configuracoes = criarAbaConfiguracoes();
            usuarios.setGraphic(icone("fas-users"));
            logs.setGraphic(icone("fas-clipboard-list"));
            configuracoes.setGraphic(icone("fas-cog"));
            abas.getTabs().addAll(usuarios, logs, configuracoes);
        }
        if (!SessionManager.atual().isOperador()) {
            abas.getTabs().stream().filter(t -> Set.of("Cadastrar", "Estoque").contains(t.getText()))
                    .forEach(t -> t.setDisable(true));
        }
        abas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        temaEscuro = PreferencesManager.darkTheme();
        BorderPane raiz = new BorderPane();
        breadcrumb = new Label();
        breadcrumb.getStyleClass().add("breadcrumb");
        HBox trilha = new HBox(breadcrumb);
        trilha.getStyleClass().add("breadcrumb-bar");
        raiz.setTop(new VBox(criarCabecalho(), trilha));
        raiz.setCenter(abas);
        raiz.setBottom(criarBarraStatus());
        VBox toastArea = new VBox();
        toastArea.setPickOnBounds(false);
        StackPane camada = new StackPane(raiz, toastArea);
        buscaOverlay = criarBuscaOverlay();
        camada.getChildren().add(buscaOverlay);
        StackPane.setAlignment(toastArea, Pos.TOP_RIGHT);
        StackPane.setMargin(toastArea, new Insets(70, 18, 0, 0));
        toasts = new ToastManager(toastArea);
        Scene cena = new Scene(camada, 1220, 780);
        cenaPrincipal = cena;
        aplicarEstilo(cena);
        configurarAtalhos(cena);
        abas.getSelectionModel().selectedItemProperty().addListener((obs, antigo, atual) -> {
            if (atual == null) return;
            atualizarBreadcrumb(atual);
            Node conteudo = atual.getContent();
            if (conteudo != null) {
                conteudo.setOpacity(0);
                FadeTransition transicao = new FadeTransition(Duration.millis(220), conteudo);
                transicao.setFromValue(0);
                transicao.setToValue(1);
                transicao.setInterpolator(Interpolator.EASE_OUT);
                transicao.play();
            }
        });
        atualizarBreadcrumb(abas.getSelectionModel().getSelectedItem());
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
        Label texto = new Label("Inicializando catálogo...");
        VBox caixa = new VBox(14, new Label("⚙ NEXUS STUDIO"), texto, progresso);
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
        // showAndWait garante que a tela de login jamais apareça antes do período mínimo.
        splash.showAndWait();
        long restante = 2000 - (System.currentTimeMillis() - inicio);
        if (restante > 0) try { Thread.sleep(restante); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private boolean mostrarLogin(Stage owner) {
        Properties credenciais = new Properties();
        try (InputStream entrada = getClass().getResourceAsStream("/credentials.properties")) {
            if (entrada != null) credenciais.load(new InputStreamReader(entrada, StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // Os valores padrão permitem iniciar mesmo sem o arquivo de configuração.
        }
        Stage login = new Stage();
        login.initOwner(owner);
        login.initModality(Modality.APPLICATION_MODAL);
        login.setTitle("Login — Nexus Studio");

        Label logo = new Label("\u2699");
        logo.getStyleClass().add("login-logo");
        Label titulo = new Label("NEXUS STUDIO");
        titulo.getStyleClass().add("login-title");
        Label subtitulo = new Label("Acesse o catálogo de hardware");
        subtitulo.getStyleClass().add("brand-subtitle");
        TextField usuario = campo("Usuário");
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
            // Compatibilidade com instalações antigas que só possuíam credentials.properties.
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
                erro.setText("Usuário ou senha incorretos");
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
        Label subtitulo = new Label("Catálogo e centro de compatibilidade de hardware");
        subtitulo.getStyleClass().add("brand-subtitle");
        Button tema = new Button();
        tema.setGraphic(icone(temaEscuro ? "fas-sun" : "fas-moon"));
        tema.setTooltip(new Tooltip("Alternar tema (preferência salva)"));
        adicionarRipple(tema);
        tema.setOnAction(e -> {
            temaEscuro = !temaEscuro;
            PreferencesManager.setDarkTheme(temaEscuro);
            tema.setGraphic(icone(temaEscuro ? "fas-sun" : "fas-moon"));
            if (cenaPrincipal != null) aplicarEstilo(cenaPrincipal);
        });
        Label usuario = new Label(SessionManager.nome() + " (" + (SessionManager.admin() ? "admin" : "usuário") + ")");
        usuario.getStyleClass().add("brand-subtitle");
        VBox marca = new VBox(2, titulo, subtitulo, usuario);
        HBox cabecalho = new HBox(12, icone, marca, tema);
        HBox.setHgrow(marca, Priority.ALWAYS);
        tema.setMinWidth(44);
        cabecalho.setAlignment(Pos.CENTER_LEFT);
        cabecalho.getStyleClass().add("app-header");
        return cabecalho;
    }

    private Node icone(String literal) {
        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(14);
        icon.getStyleClass().add("nexus-icon");
        return icon;
    }

    private void configurarIconeAbas(Tab... tabs) {
        String[] icons = {"fas-chart-bar", "fas-microchip", "fas-plus-circle", "fas-file-alt",
                "fas-check-circle", "fas-history", "fas-boxes", "fas-desktop",
                "fas-exchange-alt", "fas-shield-alt"};
        for (int i = 0; i < tabs.length && i < icons.length; i++) tabs[i].setGraphic(icone(icons[i]));
    }

    private void atualizarBreadcrumb(Tab tab) {
        if (breadcrumb != null) breadcrumb.setText(tab == null ? "Nexus Studio" : "Nexus Studio  /  " + tab.getText());
    }

    private void animarContador(Label label, double destino, java.util.function.Function<Double, String> formatador) {
        if (label == null) return;
        double inicial;
        try {
            inicial = Double.parseDouble(label.getText().replaceAll("[^0-9.,-]", "").replace(',', '.'));
        } catch (RuntimeException e) {
            inicial = 0;
        }
        DoubleProperty valor = new SimpleDoubleProperty(inicial);
        valor.addListener((obs, antigo, atual) -> label.setText(formatador.apply(atual.doubleValue())));
        Timeline animacao = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(valor, inicial)),
                new KeyFrame(Duration.millis(480), new KeyValue(valor, destino, Interpolator.EASE_OUT)));
        animacao.play();
    }

    private StackPane criarBuscaOverlay() {
        buscaGlobal = new TextField();
        buscaGlobal.setPromptText("Buscar componentes, fabricantes e modelos...");
        buscaGlobal.setPrefHeight(42);
        resultadosBusca = new ListView<>();
        resultadosBusca.setMaxHeight(260);
        resultadosBusca.setPlaceholder(mensagemVazia("Digite para buscar no catálogo."));
        resultadosBusca.setCellFactory(view -> componenteCell());
        resultadosBusca.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) selecionarResultadoGlobal();
        });
        buscaGlobal.textProperty().addListener((obs, antigo, atual) -> atualizarResultadosBusca(atual));
        buscaGlobal.setOnAction(event -> selecionarResultadoGlobal());
        VBox painel = new VBox(10, new Label("Busca rápida  •  Ctrl+K"), buscaGlobal, resultadosBusca);
        painel.getStyleClass().add("search-overlay");
        painel.setMaxWidth(620);
        painel.setMaxHeight(360);
        StackPane camada = new StackPane(painel);
        camada.setAlignment(Pos.TOP_CENTER);
        camada.setPadding(new Insets(24, 20, 20, 20));
        camada.setVisible(false);
        camada.setManaged(false);
        camada.setOnMouseClicked(event -> {
            if (event.getTarget() == camada) ocultarBuscaGlobal();
        });
        return camada;
    }

    private void atualizarResultadosBusca(String termo) {
        if (resultadosBusca == null) return;
        String chave = normalizar(termo);
        resultadosBusca.getItems().setAll(componenteDAO.listarTodos().stream()
                .filter(c -> chave.isBlank() || normalizar(texto(c.getNome())).contains(chave)
                        || normalizar(texto(c.getFabricante())).contains(chave)
                        || normalizar(texto(c.getModelo())).contains(chave))
                .limit(30).toList());
    }

    private void mostrarBuscaGlobal() {
        if (buscaOverlay == null) return;
        buscaOverlay.setVisible(true);
        buscaOverlay.setManaged(true);
        buscaOverlay.setOpacity(0);
        FadeTransition entrada = new FadeTransition(Duration.millis(160), buscaOverlay);
        entrada.setToValue(1);
        entrada.play();
        buscaGlobal.clear();
        atualizarResultadosBusca("");
        Platform.runLater(buscaGlobal::requestFocus);
    }

    private void ocultarBuscaGlobal() {
        if (buscaOverlay == null) return;
        FadeTransition saida = new FadeTransition(Duration.millis(120), buscaOverlay);
        saida.setToValue(0);
        saida.setOnFinished(event -> {
            buscaOverlay.setVisible(false);
            buscaOverlay.setManaged(false);
        });
        saida.play();
    }

    private void selecionarResultadoGlobal() {
        if (resultadosBusca == null) return;
        Componente selecionado = resultadosBusca.getSelectionModel().getSelectedItem();
        if (selecionado == null && !resultadosBusca.getItems().isEmpty()) selecionado = resultadosBusca.getItems().get(0);
        if (selecionado == null) return;
        Componente escolhido = selecionado;
        ocultarBuscaGlobal();
        selecionarAba("Componentes");
        Platform.runLater(() -> {
            buscaComponentes.setText(escolhido.getNome());
            selecionarPorId(escolhido.getId());
            mostrarDetalheJanela(escolhido);
        });
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
        HBox cartoes = new HBox(16,
                cartaoResumo("Total de componentes", dashboardTotal),
                cartaoResumo("Categorias", dashboardCategorias),
                cartaoResumo("Menor preço", dashboardMenorPreco),
                cartaoResumo("Maior TDP", dashboardMaiorTdp));
        cartoes.setPadding(new Insets(16));
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
        tdpValores.setLabel("TDP médio (W)");
        tdpMedioPorCategoriaChart = new BarChart<>(tdpCategorias, tdpValores);
        tdpMedioPorCategoriaChart.setTitle("TDP médio por categoria");
        tdpMedioPorCategoriaChart.setLegendVisible(false);
        tdpMedioPorCategoriaChart.setAnimated(false);

        precoPorFabricanteChart = new PieChart();
        precoPorFabricanteChart.setTitle("Preço médio por fabricante");
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
        Label nome = new Label(titulo.toUpperCase(Locale.ROOT));
        nome.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        valor.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 28px; -fx-font-weight: bold;");
        VBox cartao = new VBox(4, nome, valor);
        cartao.setStyle(
                "-fx-background-color: #1a1d27;" +
                "-fx-border-color: #2e3148 #2e3148 #2e3148 #1D9E75;" +
                "-fx-border-width: 1 1 1 4;" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;" +
                "-fx-padding: 20 24 20 24;"
        );
        cartao.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cartao, Priority.ALWAYS);
        return cartao;
    }

    private void atualizarDashboard() {
        if (dashboardTotal == null) return;
        List<Componente> lista = componenteDAO.listarTodos();
        List<Categoria> categorias = categoriaDAO.listarTodas();
        animarContador(dashboardTotal, lista.size(), valor -> String.format("%.0f", valor));
        animarContador(dashboardCategorias, categorias.size(), valor -> String.format("%.0f", valor));
        if (lista.isEmpty()) {
            dashboardMenorPreco.setText("--");
            dashboardMaiorTdp.setText("--");
        } else {
            animarContador(dashboardMenorPreco, lista.stream().mapToDouble(Componente::getPreco).min().orElse(0),
                    valor -> String.format("R$ %.2f", valor));
            animarContador(dashboardMaiorTdp, lista.stream().mapToDouble(Componente::getTdpWatts).max().orElse(0),
                    valor -> String.format("%.0f W", valor));
        }

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
        Platform.runLater(() -> aplicarCoresDashboard());
    }

    private void aplicarCoresDashboard() {
        String[] barras = {"#1D9E75", "#3b82f6", "#8b5cf6"};
        BarChart<?, ?>[] charts = {componentesPorCategoriaChart, tdpMedioPorCategoriaChart};
        for (BarChart<?, ?> chart : charts) {
            for (int i = 0; i < barras.length; i++) {
                final String cor = barras[i];
                chart.lookupAll(".default-color" + i + ".chart-bar")
                        .forEach(node -> node.setStyle("-fx-bar-fill: " + cor + ";"));
            }
        }
        String[] fatias = {"#ef4444", "#3b82f6", "#1D9E75"};
        int indice = 0;
        for (PieChart.Data data : precoPorFabricanteChart.getData()) {
            if (data.getNode() != null) {
                data.getNode().setStyle("-fx-pie-color: " + fatias[indice % fatias.length] + ";");
            }
            indice++;
        }
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
        limparBusca.setGraphic(icone("fas-times"));
        limparBusca.setText("");
        limparBusca.setTooltip(new Tooltip("Limpar busca"));
        limparBusca.setOnAction(event -> buscaComponentes.clear());
        HBox busca = new HBox(6, buscaComponentes, limparBusca);
        busca.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(buscaComponentes, Priority.ALWAYS);
        mostrarCartoes = PreferencesManager.componentCards();
        Button alternarVisualizacao = new Button();
        alternarVisualizacao.setTooltip(new Tooltip("Alternar entre tabela e cartões"));
        alternarVisualizacao.setGraphic(icone(mostrarCartoes ? "fas-table" : "fas-th-large"));
        adicionarRipple(alternarVisualizacao);
        alternarVisualizacao.setOnAction(event -> {
            mostrarCartoes = !mostrarCartoes;
            PreferencesManager.setComponentCards(mostrarCartoes);
            alternarVisualizacao.setGraphic(icone(mostrarCartoes ? "fas-table" : "fas-th-large"));
            atualizarVisualizacaoComponentes();
        });
        ToolBar barra = new ToolBar(atualizar, excluir, new Separator(), alternarVisualizacao, busca);
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antigo, atual) -> mostrarDetalhes(atual));
        tabela.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                mostrarDetalheJanela(tabela.getSelectionModel().getSelectedItem());
            }
        });
        SplitPane divisao = new SplitPane(new VBox(10, barra, tabela), criarPainelDetalhes());
        divisao.setDividerPositions(.72);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        cartoesComponentes = new FlowPane(12, 12);
        cartoesComponentes.setPadding(new Insets(12));
        cartoesComponentes.setPrefWrapLength(900);
        cartoesComponentes.getStyleClass().add("component-card-grid");
        ScrollPane cartoesScroll = new ScrollPane(cartoesComponentes);
        cartoesScroll.setFitToWidth(true);
        cartoesScroll.setVisible(mostrarCartoes);
        cartoesScroll.setManaged(mostrarCartoes);
        visualizacaoComponentes = new StackPane(divisao, cartoesScroll);
        divisao.setVisible(!mostrarCartoes);
        divisao.setManaged(!mostrarCartoes);
        tamanhoPagina = new ComboBox<>(FXCollections.observableArrayList(15, 30, 50, 100));
        tamanhoPagina.setValue(PreferencesManager.pageSize());
        if (!tamanhoPagina.getItems().contains(tamanhoPagina.getValue())) tamanhoPagina.setValue(15);
        tamanhoPagina.valueProperty().addListener((obs, old, value) -> {
            PreferencesManager.setPageSize(value);
            atualizarPaginaComponentes();
        });
        paginacaoComponentes = new Pagination(1, 0);
        paginacaoComponentes.currentPageIndexProperty().addListener((obs, old, value) -> atualizarPaginaComponentes());
        VBox lista = new VBox(8, new HBox(8, new Label("Itens por página:"), tamanhoPagina), visualizacaoComponentes, paginacaoComponentes);
        VBox.setVgrow(visualizacaoComponentes, Priority.ALWAYS);
        return new Tab("Componentes", lista);
    }

    private void atualizarVisualizacaoComponentes() {
        if (visualizacaoComponentes == null || visualizacaoComponentes.getChildren().size() < 2) return;
        Node tabelaView = visualizacaoComponentes.getChildren().get(0);
        Node cartoesView = visualizacaoComponentes.getChildren().get(1);
        tabelaView.setVisible(!mostrarCartoes);
        tabelaView.setManaged(!mostrarCartoes);
        cartoesView.setVisible(mostrarCartoes);
        cartoesView.setManaged(mostrarCartoes);
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
        if (cartoesComponentes != null) {
            cartoesComponentes.getChildren().setAll(componentesVisiveis.subList(de, ate).stream()
                    .map(this::criarCartaoComponente).toList());
        }
    }

    private HBox criarCartaoComponente(Componente componente) {
        Label nome = new Label(texto(componente.getNome()));
        nome.getStyleClass().add("component-card-title");
        Label fabricante = new Label(texto(componente.getFabricante()) + " · " + texto(componente.getModelo()));
        fabricante.getStyleClass().add("component-card-subtitle");
        Label preco = new Label(String.format("R$ %.2f", componente.getPreco()));
        preco.getStyleClass().add("compatible-price");
        Label tdp = new Label(String.format("%.0f W TDP", componente.getTdpWatts()));
        VBox infos = new VBox(5, nome, fabricante, preco, tdp);
        infos.setPadding(new Insets(12));
        HBox.setHgrow(infos, Priority.ALWAYS);
        HBox card = new HBox(8, icone("fas-microchip"), infos);
        card.setPrefWidth(260);
        card.setMinHeight(112);
        card.getStyleClass().add("component-card");
        card.setOnMouseClicked(event -> {
            tabela.getSelectionModel().select(componente);
            if (event.getClickCount() == 2) mostrarDetalheJanela(componente);
        });
        return card;
    }

    private VBox criarPainelDetalhes() {
        imagemDetalhe = new ImageView();
        imagemDetalhe.setFitWidth(210);
        imagemDetalhe.setFitHeight(150);
        imagemDetalhe.setPreserveRatio(true);
        imagemPlaceholder = new Label("\u2699\nSem imagem");
        imagemPlaceholder.getStyleClass().add("image-placeholder-icon");
        StackPane imagem = new StackPane(imagemDetalhe, imagemPlaceholder);
        imagem.getStyleClass().add("image-placeholder");
        imagem.setPrefHeight(170);
        detalheNome = new Label("Selecione um componente");
        detalheNome.getStyleClass().add("section-title");
        detalheDados = new Label("Os detalhes e o histórico aparecerão aqui.");
        detalheDados.setWrapText(true);
        Button escolher = botao("Adicionar imagem");
        escolher.setDisable(!SessionManager.atual().isOperador());
        escolher.setOnAction(event -> escolherImagem());
        novoPreco = campo("Novo preço (R$)");
        Button atualizarPreco = botao("Atualizar preço");
        atualizarPreco.setDisable(!SessionManager.atual().isOperador());
        atualizarPreco.setOnAction(event -> atualizarPrecoSelecionado());
        VBox painel = new VBox(12, imagem, detalheNome, detalheDados,
                new Separator(), rotulo("Alterar preço"), novoPreco, atualizarPreco, escolher);
        painel.setPadding(new Insets(16));
        painel.setMinWidth(270);
        painel.getStyleClass().add("content-panel");
        return painel;
    }

    private void mostrarDetalhes(Componente componente) {
        if (componente == null) {
            detalheNome.setText("Selecione um componente");
            detalheDados.setText("Os detalhes e o histórico aparecerão aqui.");
            imagemDetalhe.setImage(null);
            imagemPlaceholder.setVisible(true);
            return;
        }
        detalheNome.setText(componente.getNome());
        detalheDados.setText(String.format("Fabricante: %s%nModelo: %s%nPreço: R$ %.2f%nTDP: %.0f W",
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

    private void mostrarDetalheJanela(Componente componente) {
        if (componente == null) return;
        Stage janela = new Stage();
        janela.initOwner(cenaPrincipal == null ? null : cenaPrincipal.getWindow());
        janela.initModality(Modality.NONE);
        Label titulo = new Label(componente.getNome());
        titulo.getStyleClass().add("login-title");
        Label dados = new Label(String.format("Fabricante: %s%nModelo: %s%nPreço: R$ %.2f%nTDP: %.0f W",
                texto(componente.getFabricante()), texto(componente.getModelo()),
                componente.getPreco(), componente.getTdpWatts()));
        dados.setWrapText(true);
        Label imagemTexto = new Label("⚙\nSem imagem");
        imagemTexto.getStyleClass().add("image-placeholder-icon");
        StackPane imagem = new StackPane(imagemTexto);
        imagem.setPrefSize(300, 190);
        imagem.getStyleClass().add("image-placeholder");
        if (componente.getImagePath() != null && !componente.getImagePath().isBlank()
                && Files.exists(Path.of(componente.getImagePath()))) {
            ImageView foto = new ImageView(new Image(Path.of(componente.getImagePath()).toUri().toString()));
            foto.setFitWidth(280);
            foto.setFitHeight(170);
            foto.setPreserveRatio(true);
            imagem.getChildren().setAll(foto);
        }
        VBox conteudo = new VBox(14, imagem, titulo, dados);
        conteudo.setPadding(new Insets(22));
        conteudo.getStyleClass().add("content-panel");
        Scene cena = new Scene(conteudo, 360, 390);
        aplicarEstilo(cena);
        janela.setTitle("Detalhes — " + componente.getNome());
        janela.setScene(cena);
        janela.show();
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
            registrar("ATUALIZAR", "COMPONENTE", "Preço id=" + selecionado.getId());
            atualizarTabela(componenteDAO.listarTodos());
            selecionarPorId(selecionado.getId());
            atualizarGrafico();
            mostrarInfo("Preço atualizado e registrado no histórico.");
        } catch (NumberFormatException e) {
            mostrarErro("Informe um preço válido.");
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
        TextField preco = campo("Preço em R$");
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
        formulario.addRow(4, rotulo("Preço (R$)"), campoComErro(preco, erroPreco));
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
            valido &= validarObrigatorio(nome, erroNome, "Nome é obrigatório.");
            valido &= validarObrigatorio(fabricante, erroFabricante, "Fabricante é obrigatório.");
            valido &= validarObrigatorio(modelo, erroModelo, "Modelo é obrigatório.");
            Categoria categoria = categoriaCombo.getValue();
            if (categoria == null) {
                exibirMensagem(erroCategoria, "Categoria é obrigatória.");
                valido = false;
            } else esconderMensagem(erroCategoria);
            Double precoNumerico = validarPositivo(preco, erroPreco, "Preço deve ser maior que zero.");
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
                    registrar("CRIAR", "COMPONENTE", "Importação CSV: " + nome);
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
            mostrarErro("Não foi possível ler o arquivo CSV: " + ex.getMessage());
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
            mostrarErro("Não foi possível exportar o exemplo CSV: " + ex.getMessage());
        }
    }

    private Tab criarAbaRelatorios() {
        ComboBox<String> tipo = new ComboBox<>(FXCollections.observableArrayList(
                "Todos", "Faixa de preço", "TDP máximo", "Fabricante"));
        tipo.setValue("Todos");
        TextField valor1 = campo("Valor / fabricante");
        TextField valor2 = campo("Preço máximo");
        Button aplicar = botao("Aplicar filtro");
        Button exportar = botao("Exportar PDF");
        Button excel = botao("Exportar Excel");
        TableView<Componente> resultado = criarTabela("Nenhum componente encontrado.");
        aplicar.setOnAction(event -> {
            try {
                resultado.getItems().setAll(filtrar(tipo.getValue(), valor1.getText(), valor2.getText()));
            } catch (NumberFormatException e) {
                mostrarErro("Os valores do relatório devem ser números válidos.");
            }
        });
        tipo.valueProperty().addListener((obs, antigo, atual) -> {
            boolean faixa = "Faixa de preço".equals(atual);
            valor2.setDisable(!faixa);
            valor2.setVisible(faixa);
        });
        valor2.setDisable(true);
        valor2.setVisible(false);
        exportar.setOnAction(event -> exportarComponentesPdf(new ArrayList<>(resultado.getItems()),
                "relatório filtrado"));
        excel.setOnAction(event -> exportarExcel());
        return new Tab("Relatórios", painel(new ToolBar(tipo, valor1, valor2, aplicar, exportar, excel), resultado));
    }

    private Tab criarAbaCompatibilidade() {
        ComboBox<Componente> combo = comboComponentes("Selecione um componente");
        Label socketsTitulo = new Label("Sockets e padrões cadastrados");
        socketsTitulo.getStyleClass().add("section-title");
        FlowPane sockets = new FlowPane(8, 8);
        sockets.getStyleClass().add("socket-list");
        Label compativeisTitulo = new Label("Componentes compatíveis");
        compativeisTitulo.getStyleClass().add("section-title");
        VBox compativeis = new VBox(8, new Label("Selecione um componente para consultar."));
        compativeis.getStyleClass().add("compatible-list");
        combo.setOnAction(event -> {
            sockets.getChildren().clear();
            compativeis.getChildren().clear();
            Componente selecionado = combo.getValue();
            if (selecionado == null) return;
            List<Compatibilidade> registros = compatibilidadeDAO.listarPorComponente(selecionado.getId());
            if (registros.isEmpty()) sockets.getChildren().add(new Label("Nenhum socket ou padrão cadastrado."));
            registros.forEach(registro -> {
                Label nome = new Label(registro.getSocketTipo() + formatarPadrao(registro.getPadrao()));
                Button remover = new Button("×");
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
            if (encontrados.isEmpty()) compativeis.getChildren().add(new Label("Nenhum componente compatível encontrado."));
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
                colunaComparacao("Característica", ComparacaoLinha::caracteristica),
                colunaComparacao("Componente A", ComparacaoLinha::primeiro),
                colunaComparacao("Componente B", ComparacaoLinha::segundo));
        tabelaComparacao.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        Label diferenca = new Label("Diferença de preço: -");
        Label socketsCompartilhados = new Label("Sockets compartilhados: -");
        Label veredito = new Label("Selecione dois componentes para comparar.");
        veredito.getStyleClass().add("verdict");
        Runnable atualizar = () -> {
            Componente a = primeiro.getValue();
            Componente b = segundo.getValue();
            tabelaComparacao.getItems().clear();
            if (a == null || b == null || a == b) {
                diferenca.setText("Diferença de preço: -");
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
                    new ComparacaoLinha("Preço", String.format("R$ %.2f", a.getPreco()), String.format("R$ %.2f", b.getPreco())),
                    new ComparacaoLinha("TDP", String.format("%.0f W", a.getTdpWatts()), String.format("%.0f W", b.getTdpWatts())),
                    new ComparacaoLinha("Especificações", formatarEspecificacoes(a.getId()), formatarEspecificacoes(b.getId())),
                    new ComparacaoLinha("Sockets", formatarSockets(socketsA), formatarSockets(socketsB)));
            diferenca.setText(String.format("Diferença de preço: R$ %.2f (%s)", Math.abs(a.getPreco() - b.getPreco()),
                    a.getPreco() <= b.getPreco() ? "A é mais barata" : "B é mais barata"));
            socketsCompartilhados.setText("Sockets compartilhados: " + (compartilhados.isEmpty() ? "nenhum" : String.join(", ", compartilhados)));
            boolean compativel = !compartilhados.isEmpty();
            veredito.setText(compativel ? "Veredito: componentes compatíveis." : "Veredito: nenhum socket compatível encontrado.");
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
        estilizarCabecalho(coluna, "fas-list");
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
                documento.add(new Paragraph("Nexus Studio - Comparação de componentes").setBold());
                documento.add(new Paragraph("Gerado em " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
                Table tabelaPdf = new Table(UnitValue.createPercentArray(new float[]{2, 4, 4})).useAllAvailableWidth();
                tabelaPdf.addHeaderCell("Característica"); tabelaPdf.addHeaderCell(a.getNome()); tabelaPdf.addHeaderCell(b.getNome());
                String[][] linhas = {{"Fabricante", texto(a.getFabricante()), texto(b.getFabricante())},
                        {"Modelo", texto(a.getModelo()), texto(b.getModelo())},
                        {"Preço", String.format("R$ %.2f", a.getPreco()), String.format("R$ %.2f", b.getPreco())},
                        {"TDP", String.format("%.0f W", a.getTdpWatts()), String.format("%.0f W", b.getTdpWatts())},
                        {"Especificações", formatarEspecificacoes(a.getId()), formatarEspecificacoes(b.getId())},
                        {"Sockets", formatarSockets(sockets(a)), formatarSockets(sockets(b))}};
                for (String[] linha : linhas) for (String valor : linha) tabelaPdf.addCell(valor);
                documento.add(tabelaPdf);
            }
            registrar("EXPORTAR", "COMPARACAO", arquivo.toString());
            mostrarInfo("Comparação exportada em: " + arquivo);
        } catch (Exception e) { mostrarErro("Não foi possível gerar a comparação: " + e.getMessage()); }
    }
    private Tab criarAbaHistorico() {
        historicoCombo = comboComponentes("Selecione um componente");
        historicoCombo.setOnAction(event -> atualizarGrafico());
        NumberAxis eixoX = new NumberAxis();
        NumberAxis eixoY = new NumberAxis();
        eixoX.setLabel("Registro");
        eixoY.setLabel("Preço (R$)");
        historicoChart = new LineChart<>(eixoX, eixoY);
        historicoChart.setTitle("Evolução do preço");
        historicoChart.setCreateSymbols(true);
        VBox conteudo = new VBox(12, new HBox(10, rotulo("Componente"), historicoCombo), historicoChart);
        conteudo.setPadding(new Insets(18));
        VBox.setVgrow(historicoChart, Priority.ALWAYS);
        return new Tab("Histórico de preços", conteudo);
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
                colunaEstoque("Localização", linha -> texto(linha.localizacao())));
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
        TextField localizacao = campo("Localização");
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
            } catch (NumberFormatException e) { mostrarErro("A quantidade deve ser um número inteiro não negativo."); }
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
        HBox formulario = new HBox(10, rotulo("Quantidade"), quantidade, rotulo("Localização"), localizacao, atualizar, excluirEstoque);
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
        ComboBox<Componente> placaMae = comboComponentes("Selecione a placa-mãe");
        ComboBox<Componente> memoria = comboComponentes("Selecione a memória RAM");
        ComboBox<Componente> gpu = comboComponentes("Selecione a placa de vídeo");
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
        Label veredito = new Label("Selecione os componentes para verificar a configuração.");
        veredito.getStyleClass().add("verdict");
        VBox selecionadosPainel = new VBox(6);
        selecionadosPainel.getStyleClass().add("compatible-list");
        selecionadosPainel.getChildren().add(new Label("Os componentes selecionados aparecerão aqui."));
        Button verificar = botao("Verificar compatibilidade");
        Button exportar = botao("Exportar Orçamento");
        verificar.setOnAction(event -> {
            List<Componente> selecionados = Arrays.asList(processador.getValue(), placaMae.getValue(),
                    memoria.getValue(), gpu.getValue());
            if (selecionados.stream().anyMatch(Objects::isNull)) {
                veredito.setText("Selecione processador, placa-mãe, memória RAM e placa de vídeo.");
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
            veredito.setText(ok ? "Configuração compatível!" : "Incompatibilidade detectada! " + conflitos);
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
        campos.addRow(1, rotulo("Placa-mãe"), placaMae);
        campos.addRow(2, rotulo("Memória RAM"), memoria);
        campos.addRow(3, rotulo("Placa de vídeo"), gpu);
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
            conflitos.add("processador e placa-mãe");
        }
        if (!compatibilidadeEntre(placaMae, memoria, false)) {
            conflitos.add("placa-mãe e memória RAM");
        }
        if (!compatibilidadeEntre(placaMae, gpu, false)) {
            conflitos.add("placa-mãe e placa de vídeo");
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
        estilizarCabecalho(coluna, "fas-boxes");
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
                colunaUsuario("Usuário", Usuario::username), colunaUsuario("Nome", Usuario::nome),
                colunaUsuario("Nível", Usuario::nivel), colunaUsuario("Ativo", u -> u.ativo() ? "Sim" : "Não"));
        TextField username=campo("Usuário"), nome=campo("Nome"), senha=campo("Senha");
        ComboBox<String> nivel=new ComboBox<>(FXCollections.observableArrayList("ADMIN","OPERADOR","USUARIO")); nivel.setValue("USUARIO");
        Button salvar=botao("Salvar usuário"), excluir=botao("Excluir selecionado");
        salvar.setOnAction(e->{if(username.getText().isBlank()||nome.getText().isBlank()||senha.getText().isBlank()){mostrarErro("Preencha usuário, nome e senha.");return;}try{usuarioDAO.salvar(new Usuario(0,username.getText().trim(),nome.getText().trim(),senha.getText(),nivel.getValue(),true));tabelaUsuarios.getItems().setAll(usuarioDAO.listarTodos());registrar("CRIAR","USUARIO",username.getText());mostrarInfo("Usuário salvo.");}catch(RuntimeException ex){mostrarErro(ex.getMessage());}});
        excluir.setOnAction(e->{Usuario u=tabelaUsuarios.getSelectionModel().getSelectedItem();if(u==null)return;if(!confirmarExclusao("Excluir usuário","Deseja remover "+u.username()+"?"))return;usuarioDAO.excluir(u.id());tabelaUsuarios.getItems().setAll(usuarioDAO.listarTodos());registrar("EXCLUIR","USUARIO",u.username());});
        tabelaUsuarios.getSelectionModel().selectedItemProperty().addListener((o,a,u)->{if(u!=null){username.setText(u.username());nome.setText(u.nome());senha.setText(u.senha());nivel.setValue(u.nivel());}});
        HBox form=new HBox(8,username,nome,senha,nivel,salvar,excluir);
        VBox box=new VBox(12,form,tabelaUsuarios);box.setPadding(new Insets(18));VBox.setVgrow(tabelaUsuarios,Priority.ALWAYS);
        return new Tab("Usuários",box);
    }
    private <T> TableColumn<Usuario,T> colunaUsuario(String title, java.util.function.Function<Usuario,T> fn) {
        TableColumn<Usuario,T> c=new TableColumn<>(title); c.setCellValueFactory(v->new ReadOnlyObjectWrapper<>(fn.apply(v.getValue()))); estilizarCabecalho(c, "fas-user"); return c;
    }

    private Tab criarAbaLogs() {
        TextField filtro=campo("Filtrar por usuário, ação ou entidade...");
        TableView<LogEntry> tabelaLogs=new TableView<>();
        tabelaLogs.getColumns().setAll(colunaLog("Data",l->l.dataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))),
                colunaLog("Usuário",LogEntry::usuario),colunaLog("Ação",LogEntry::acao),colunaLog("Entidade",LogEntry::entidade),colunaLog("Detalhes",LogEntry::detalhes));
        Runnable atualizar=()->tabelaLogs.getItems().setAll(logDAO.listar(filtro.getText()));
        filtro.textProperty().addListener((o,a,b)->atualizar.run()); atualizar.run();
        Button csv=botao("Exportar CSV");
        csv.setOnAction(e->{try{Path desktop=Path.of(System.getProperty("user.home"),"Desktop");Files.createDirectories(desktop);Path arq=desktop.resolve("nexus-logs-"+LocalDate.now()+".csv");StringBuilder out=new StringBuilder("data,usuario,acao,entidade,detalhes\n");for(LogEntry l:logDAO.listar(filtro.getText()))out.append('"').append(l.dataHora()).append("\",\"").append(l.usuario()).append("\",\"").append(l.acao()).append("\",\"").append(l.entidade()).append("\",\"").append(l.detalhes()).append("\"\n");Files.writeString(arq,out);mostrarInfo("Logs exportados em: "+arq);}catch(IOException ex){mostrarErro("Falha ao exportar logs: "+ex.getMessage());}});
        VBox box=new VBox(10,new HBox(8,filtro,csv),tabelaLogs);box.setPadding(new Insets(18));VBox.setVgrow(tabelaLogs,Priority.ALWAYS);
        return new Tab("Logs de atividade",box);
    }
    private <T> TableColumn<LogEntry,T> colunaLog(String title, java.util.function.Function<LogEntry,T> fn) {
        TableColumn<LogEntry,T> c=new TableColumn<>(title);c.setCellValueFactory(v->new ReadOnlyObjectWrapper<>(fn.apply(v.getValue())));estilizarCabecalho(c, "fas-clipboard-list");return c;
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
            if (fim.getValue().isBefore(inicio.getValue())) { mostrarErro("A data final deve ser posterior à inicial."); return; }
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
        estilizarCabecalho(coluna, "fas-shield-alt");
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
        if (cena != null) {
            cena.getRoot().getStyleClass().removeIf(style -> style.startsWith("font-size-"));
            cena.getRoot().getStyleClass().add("font-size-" + Math.round(tamanho));
        }
    }

    private void configurarAtalhos(Scene cena) {
        cena.getAccelerators().put(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.K, javafx.scene.input.KeyCombination.CONTROL_DOWN), this::mostrarBuscaGlobal);
        cena.getAccelerators().put(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.N, javafx.scene.input.KeyCombination.CONTROL_DOWN), () -> selecionarAba("Cadastrar"));
        cena.getAccelerators().put(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.F, javafx.scene.input.KeyCombination.CONTROL_DOWN), () -> {if(buscaComponentes!=null){selecionarAba("Componentes");buscaComponentes.requestFocus();}});
        cena.getAccelerators().put(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.R, javafx.scene.input.KeyCombination.CONTROL_DOWN), () -> atualizarTabela(componenteDAO.listarTodos()));
        cena.getAccelerators().put(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.E, javafx.scene.input.KeyCombination.CONTROL_DOWN), this::exportarExcel);
        cena.getAccelerators().put(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.S, javafx.scene.input.KeyCombination.CONTROL_DOWN), () -> mostrarInfo("Use os botões Salvar para confirmar alterações."));
        cena.getAccelerators().put(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.Z, javafx.scene.input.KeyCombination.CONTROL_DOWN), this::desfazerUltimaAcao);
        cena.getAccelerators().put(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.F1), () -> mostrarInfo("Atalhos: Ctrl+N novo, Ctrl+F buscar, Ctrl+R/F5 atualizar, Ctrl+E Excel, Delete excluir, Esc limpar, Ctrl+S salvar."));
        cena.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED,e->{if(e.getCode()==javafx.scene.input.KeyCode.F5){atualizarTabela(componenteDAO.listarTodos());e.consume();}else if(e.getCode()==javafx.scene.input.KeyCode.DELETE){excluirSelecionado();e.consume();}else if(e.getCode()==javafx.scene.input.KeyCode.ESCAPE){if(buscaOverlay != null && buscaOverlay.isVisible()) ocultarBuscaGlobal(); else {if(buscaComponentes!=null)buscaComponentes.clear();tabela.getSelectionModel().clearSelection();}}});
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
        tabela.setItems(componentesPagina);
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
        componentesPagina.addListener((javafx.collections.ListChangeListener<Componente>) change ->
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
        TableColumn<Componente, Number> preco = coluna("Preço", Componente::getPreco);
        TableColumn<Componente, Number> tdp = coluna("TDP (W)", Componente::getTdpWatts);
        return List.of(id, nome, fabricante, modelo, preco, tdp);
    }

    private <T> TableColumn<Componente, T> coluna(String titulo, java.util.function.Function<Componente, T> valor) {
        TableColumn<Componente, T> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(dado -> new ReadOnlyObjectWrapper<>(valor.apply(dado.getValue())));
        String icon = switch (titulo) {
            case "ID" -> "fas-hashtag";
            case "Nome" -> "fas-font";
            case "Fabricante" -> "fas-industry";
            case "Modelo" -> "fas-barcode";
            case "Preço" -> "fas-dollar-sign";
            case "TDP (W)" -> "fas-bolt";
            default -> "fas-microchip";
        };
        estilizarCabecalho(coluna, icon);
        return coluna;
    }

    private void estilizarCabecalho(TableColumn<?, ?> coluna, String literal) {
        String titulo = coluna.getText();
        coluna.setGraphic(new HBox(5, icone(literal), new Label(titulo)));
        coluna.setText("");
    }

    private List<Componente> filtrar(String tipo, String valor1, String valor2) {
        return switch (tipo) {
            case "Faixa de preço" -> componenteDAO.filtrarPorPreco(numero(valor1), numero(valor2));
            case "TDP máximo" -> componenteDAO.filtrarPorTdpMaximo(numero(valor1));
            case "Fabricante" -> componenteDAO.buscarPorFabricante(valor1.trim());
            default -> componenteDAO.listarTodos();
        };
    }

    private double numero(String valor) { return valor == null || valor.isBlank() ? 0 : Double.parseDouble(valor.replace(',', '.').trim()); }
    private double numero(TextField campo) { return numero(campo.getText()); }
    private TextField campo(String prompt) { TextField campo = new TextField(); campo.setPromptText(prompt); campo.setPrefWidth(220); return campo; }
    private Label rotulo(String texto) { Label rotulo = new Label(texto); rotulo.getStyleClass().add("field-label"); return rotulo; }
    private Button botao(String texto) {
        Button botao = new Button(texto);
        botao.setMinWidth(140);
        botao.setPrefHeight(34);
        botao.getStyleClass().add("primary-button");
        String icon = switch (texto) {
            case "Atualizar", "Atualizar selecionado" -> "fas-sync-alt";
            case "Excluir selecionado", "Excluir selecionada", "Excluir estoque" -> "fas-trash-alt";
            case "Cadastrar componente", "Salvar usuário", "Salvar garantia" -> "fas-save";
            case "Importar CSV" -> "fas-file-import";
            case "Exportar exemplo CSV", "Exportar CSV" -> "fas-file-export";
            case "Exportar PDF" -> "fas-file-pdf";
            case "Exportar Excel" -> "fas-file-excel";
            case "Verificar compatibilidade" -> "fas-check-circle";
            case "Exportar Orçamento" -> "fas-file-invoice-dollar";
            case "Adicionar imagem" -> "fas-image";
            case "Atualizar preço" -> "fas-tag";
            case "Fazer backup agora" -> "fas-database";
            case "Entrar" -> "fas-sign-in-alt";
            case "Desfazer" -> "fas-undo";
            default -> null;
        };
        if (icon != null) botao.setGraphic(icone(icon));
        adicionarRipple(botao);
        return botao;
    }

    private void adicionarRipple(Button botao) {
        botao.setOnMousePressed(event -> {
            ScaleTransition escala = new ScaleTransition(Duration.millis(70), botao);
            escala.setToX(.97);
            escala.setToY(.97);
            escala.play();
        });
        botao.setOnMouseReleased(event -> {
            ScaleTransition escala = new ScaleTransition(Duration.millis(120), botao);
            escala.setToX(1);
            escala.setToY(1);
            escala.setInterpolator(Interpolator.EASE_OUT);
            escala.play();
        });
    }
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
        if (lista.isEmpty()) { mostrarErro("Não há componentes para exportar."); return; }
        try {
            Path desktop = Path.of(System.getProperty("user.home"), "Desktop");
            Files.createDirectories(desktop);
            String prefixo = titulo.startsWith("configuração") ? "nexus-configuracao-" : "nexus-relatorio-";
            Path arquivo = desktop.resolve(prefixo + LocalDate.now() + ".pdf");
            try (PdfWriter writer = new PdfWriter(arquivo.toString());
                 PdfDocument pdf = new PdfDocument(writer);
                 Document documento = new Document(pdf)) {
                documento.add(new Paragraph("Nexus Studio — Relatório de Componentes"));
                documento.add(new Paragraph(titulo));
                documento.add(new Paragraph("Gerado em " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
                if (titulo.startsWith("configuração")) {
                    double totalPreco = lista.stream().mapToDouble(Componente::getPreco).sum();
                    double totalTdp = lista.stream().mapToDouble(Componente::getTdpWatts).sum();
                    documento.add(new Paragraph(String.format("Total: R$ %.2f | TDP total: %.0f W",
                            totalPreco, totalTdp)));
                }

                Table tabelaPdf = new Table(UnitValue.createPercentArray(new float[]{1, 3, 2, 2, 2, 1})).useAllAvailableWidth();
                for (String cabecalho : new String[]{"ID", "Nome", "Fabricante", "Modelo", "Preço (R$)", "TDP (W)"}) tabelaPdf.addHeaderCell(new Cell().add(new Paragraph(cabecalho)));
                for (Componente c : lista) {
                    tabelaPdf.addCell(String.valueOf(c.getId())); tabelaPdf.addCell(c.getNome());
                    tabelaPdf.addCell(texto(c.getFabricante())); tabelaPdf.addCell(c.getModelo());
                    tabelaPdf.addCell(String.format("%.2f", c.getPreco())); tabelaPdf.addCell(String.format("%.0f", c.getTdpWatts()));
                }
                documento.add(tabelaPdf);
            }
            mostrarInfo("PDF salvo em: " + arquivo);
        } catch (Exception e) { mostrarErro("Não foi possível gerar o PDF: " + e.getMessage()); }
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
        } catch (IOException e) { mostrarErro("Não foi possível gerar o Excel: " + e.getMessage()); }
    }

    private void criarPlanilhaComponentes(Workbook w, CellStyle style) {
        Sheet s = w.createSheet("Componentes");
        String[] h = {"ID","Categoria","Nome","Fabricante","Modelo","Preço (R$)","TDP (W)"};
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
        Sheet s=w.createSheet("Estoque"); String[] h={"Componente","Modelo","Quantidade","Localização"};
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
                doc.add(new Paragraph("Orçamento personalizado de computador").setFontSize(15));
                doc.add(new Paragraph("Emitido em "+LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
                Table tabela=new Table(UnitValue.createPercentArray(new float[]{4,2,2})).useAllAvailableWidth();
                tabela.addHeaderCell("Componente"); tabela.addHeaderCell("Modelo"); tabela.addHeaderCell("Preço");
                for(Componente c:itens){tabela.addCell(c.getNome());tabela.addCell(c.getModelo());tabela.addCell(String.format("R$ %.2f",c.getPreco()));}
                doc.add(tabela);
                doc.add(new Paragraph(String.format("TOTAL: R$ %.2f",itens.stream().mapToDouble(Componente::getPreco).sum())).setBold().setFontSize(16));
                doc.add(new Paragraph("Valores sujeitos a alteração. Este documento é um orçamento sem compromisso."));
            }
            registrar("EXPORTAR","ORCAMENTO",arquivo.toString()); mostrarInfo("Orçamento salvo em: "+arquivo);
        } catch(Exception e){ mostrarErro("Não foi possível gerar o orçamento: "+e.getMessage()); }
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

    private String texto(String valor) { return valor == null || valor.isBlank() ? "Não informado" : valor; }
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
        dialog.setHeaderText("Confirmação necessária");
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
