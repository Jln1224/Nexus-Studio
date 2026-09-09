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
import database.DatabaseManager;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Categoria;
import model.Componente;
import model.Compatibilidade;
import model.HistoricoPreco;
import model.Inventario;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class NexusStudioApp extends Application {
    private static final String TEAL = "#1D9E75";
    private final ComponenteDAO componenteDAO = new ComponenteDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final CompatibilidadeDAO compatibilidadeDAO = new CompatibilidadeDAO();
    private final HistoricoPrecoDAO historicoDAO = new HistoricoPrecoDAO();
    private final InventarioDAO inventarioDAO = new InventarioDAO();
    private final TableView<Componente> tabela = new TableView<>();
    private final ObservableList<Componente> componentes = FXCollections.observableArrayList();
    private final ObservableList<EstoqueLinha> estoque = FXCollections.observableArrayList();
    private Label statusLabel;
    private ComboBox<Categoria> categoriaCombo;
    private ImageView imagemDetalhe;
    private Label imagemPlaceholder;
    private Label detalheNome;
    private Label detalheDados;
    private TextField novoPreco;
    private ComboBox<Componente> historicoCombo;
    private LineChart<Number, Number> historicoChart;
    private Label estoqueResumo;

    @Override
    public void start(Stage stage) {
        DatabaseManager.inicializarBanco();
        configurarTabela(tabela, "Nenhum componente cadastrado.");
        TabPane abas = new TabPane(
                criarAbaComponentes(), criarAbaCadastro(), criarAbaRelatorios(),
                criarAbaCompatibilidade(), criarAbaHistorico(), criarAbaEstoque(), criarAbaMonteSeuPc()
        );
        abas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        BorderPane raiz = new BorderPane();
        raiz.setTop(criarCabecalho());
        raiz.setCenter(abas);
        raiz.setBottom(criarBarraStatus());
        Scene cena = new Scene(raiz, 1220, 780);
        aplicarEstilo(cena);
        stage.setTitle("Nexus Studio");
        stage.setScene(cena);
        atualizarTabela(componenteDAO.listarTodos());
        atualizarEstoque();
        stage.show();
    }

    private HBox criarCabecalho() {
        Label icone = new Label("\u2699");
        icone.getStyleClass().add("brand-icon");
        Label titulo = new Label("NEXUS STUDIO");
        titulo.getStyleClass().add("brand-title");
        Label subtitulo = new Label("Catálogo e centro de compatibilidade de hardware");
        subtitulo.getStyleClass().add("brand-subtitle");
        HBox cabecalho = new HBox(12, icone, new VBox(2, titulo, subtitulo));
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

    private Tab criarAbaComponentes() {
        Button atualizar = botao("Atualizar");
        atualizar.setOnAction(event -> atualizarTabela(componenteDAO.listarTodos()));
        Button excluir = botao("Excluir selecionado");
        excluir.setOnAction(event -> excluirSelecionado());
        ToolBar barra = new ToolBar(atualizar, excluir);
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antigo, atual) -> mostrarDetalhes(atual));
        SplitPane divisao = new SplitPane(new VBox(10, barra, tabela), criarPainelDetalhes());
        divisao.setDividerPositions(.72);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        return new Tab("Componentes", divisao);
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
        detalheDados = new Label("Os detalhes e o histórico aparecerão aqui.");
        detalheDados.setWrapText(true);
        Button escolher = botao("Adicionar imagem");
        escolher.setOnAction(event -> escolherImagem());
        novoPreco = campo("Novo preço (R$)");
        Button atualizarPreco = botao("Atualizar preço");
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
        categoriaCombo = new ComboBox<>();
        categoriaCombo.setPromptText("Selecione uma categoria");
        categoriaCombo.setMaxWidth(Double.MAX_VALUE);
        carregarCategorias();
        formulario.addRow(0, rotulo("Categoria"), categoriaCombo);
        formulario.addRow(1, rotulo("Nome"), nome);
        formulario.addRow(2, rotulo("Fabricante"), fabricante);
        formulario.addRow(3, rotulo("Modelo"), modelo);
        formulario.addRow(4, rotulo("Preço (R$)"), preco);
        formulario.addRow(5, rotulo("TDP (W)"), tdp);
        formulario.getColumnConstraints().addAll(new ColumnConstraints(130), colunaExpansivel());
        Button salvar = botao("Cadastrar componente");
        salvar.setOnAction(event -> {
            try {
                Categoria categoria = categoriaCombo.getValue();
                if (categoria == null || nome.getText().isBlank() || modelo.getText().isBlank()) {
                    mostrarErro("Categoria, nome e modelo são obrigatórios.");
                    return;
                }
                Componente novo = new Componente(categoria.getId(), nome.getText().trim(), fabricante.getText().trim(),
                        modelo.getText().trim(), numero(preco), numero(tdp));
                componenteDAO.inserir(novo);
                atualizarTabela(componenteDAO.listarTodos());
                nome.clear(); fabricante.clear(); modelo.clear(); preco.clear(); tdp.clear();
                mostrarInfo("Componente cadastrado com sucesso.");
            } catch (NumberFormatException e) {
                mostrarErro("Preço e TDP devem ser números válidos.");
            }
        });
        return new Tab("Cadastrar", formularioComBotao(formulario, salvar));
    }

    private Tab criarAbaRelatorios() {
        ComboBox<String> tipo = new ComboBox<>(FXCollections.observableArrayList(
                "Todos", "Faixa de preço", "TDP máximo", "Fabricante"));
        tipo.setValue("Todos");
        TextField valor1 = campo("Valor / fabricante");
        TextField valor2 = campo("Preço máximo");
        Button aplicar = botao("Aplicar filtro");
        Button exportar = botao("Exportar PDF");
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
        return new Tab("Relatórios", painel(new ToolBar(tipo, valor1, valor2, aplicar, exportar), resultado));
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
            registros.forEach(registro -> sockets.getChildren().add(
                    new Label(registro.getSocketTipo() + formatarPadrao(registro.getPadrao()))));
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
        atualizar.setOnAction(event -> {
            EstoqueLinha linha = tabelaEstoque.getSelectionModel().getSelectedItem();
            if (linha == null) { mostrarErro("Selecione um item do estoque."); return; }
            try {
                int qtd = Integer.parseInt(quantidade.getText().trim());
                if (qtd < 0) throw new NumberFormatException();
                inventarioDAO.atualizarQuantidade(linha.componente().getId(), qtd, localizacao.getText().trim());
                atualizarEstoque();
                mostrarInfo("Estoque atualizado.");
            } catch (NumberFormatException e) { mostrarErro("A quantidade deve ser um número inteiro não negativo."); }
        });
        tabelaEstoque.getSelectionModel().selectedItemProperty().addListener((obs, antigo, atual) -> {
            if (atual != null) { quantidade.setText(String.valueOf(atual.quantidade())); localizacao.setText(texto(atual.localizacao())); }
        });
        estoqueResumo = new Label();
        HBox formulario = new HBox(10, rotulo("Quantidade"), quantidade, rotulo("Localização"), localizacao, atualizar);
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
        Label veredito = new Label("Selecione os componentes para verificar a configuração.");
        veredito.getStyleClass().add("verdict");
        VBox selecionadosPainel = new VBox(6);
        selecionadosPainel.getStyleClass().add("compatible-list");
        selecionadosPainel.getChildren().add(new Label("Os componentes selecionados aparecerão aqui."));
        Button verificar = botao("Verificar compatibilidade");
        Button exportar = botao("Exportar configuração PDF");
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
            String conflitos = montarDescricaoConflitos(processador.getValue(), placaMae.getValue(),
                    memoria.getValue(), gpu.getValue());
            veredito.setText(ok ? "Configuração compatível!" : "Incompatibilidade detectada! " + conflitos);
            veredito.getStyleClass().removeAll("verdict-ok", "verdict-error");
            veredito.getStyleClass().add(ok ? "verdict-ok" : "verdict-error");
        });
        exportar.setOnAction(event -> {
            List<Componente> itens = Arrays.asList(processador.getValue(), placaMae.getValue(), memoria.getValue(), gpu.getValue());
            if (itens.stream().anyMatch(Objects::isNull)) { mostrarErro("Selecione todos os componentes antes de exportar."); return; }
            exportarComponentesPdf(itens, "configuração Monte seu PC");
        });
        GridPane campos = new GridPane();
        campos.setHgap(12); campos.setVgap(12); campos.setPadding(new Insets(20));
        campos.getStyleClass().add("form-card");
        campos.addRow(0, rotulo("Processador"), processador);
        campos.addRow(1, rotulo("Placa-mãe"), placaMae);
        campos.addRow(2, rotulo("Memória RAM"), memoria);
        campos.addRow(3, rotulo("Placa de vídeo"), gpu);
        campos.getColumnConstraints().addAll(new ColumnConstraints(130), colunaExpansivel());
        VBox conteudo = new VBox(14, campos, new HBox(10, verificar, exportar),
                new Label("Componentes selecionados"), selecionadosPainel, total, veredito);
        conteudo.setPadding(new Insets(20));
        return new Tab("Monte seu PC", conteudo);
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
        tabela.setItems(componentes);
        tabela.getColumns().setAll(criarColunas());
        tabela.setPlaceholder(new Label(vazio));
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
        resultado.setPlaceholder(new Label(vazio));
        resultado.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        resultado.getItems().addListener((javafx.collections.ListChangeListener<Componente>) change ->
                ajustarAlturaTabela(resultado));
        ajustarAlturaTabela(resultado);
        return resultado;
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
        return coluna;
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
        for (int i = 0; i < componentes.size(); i++) if (componentes.get(i).getId() == id) {
            tabela.getSelectionModel().select(i); tabela.scrollTo(i); break;
        }
    }

    private void excluirSelecionado() {
        Componente selecionado = tabela.getSelectionModel().getSelectedItem();
        if (selecionado == null) { mostrarErro("Selecione um componente para excluir."); return; }
        componenteDAO.deletar(selecionado.getId());
        atualizarTabela(componenteDAO.listarTodos());
        atualizarEstoque();
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

    private void aplicarEstilo(Scene cena) {
        cena.getRoot().setStyle("-fx-font-family: 'Segoe UI';");
        cena.getStylesheets().add("data:text/css;charset=utf-8," +
                java.net.URLEncoder.encode(ESTILO, StandardCharsets.UTF_8).replace("+", "%20"));
    }

    private String texto(String valor) { return valor == null || valor.isBlank() ? "Não informado" : valor; }
    private boolean iguais(String a, String b) { return a != null && b != null && !a.isBlank() && a.equalsIgnoreCase(b); }
    private String normalizar(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
    }
    private void mostrarErro(String mensagem) { new Alert(Alert.AlertType.ERROR, mensagem, ButtonType.OK).showAndWait(); }
    private void mostrarInfo(String mensagem) { new Alert(Alert.AlertType.INFORMATION, mensagem, ButtonType.OK).showAndWait(); }
    private record EstoqueLinha(Componente componente, int quantidade, String localizacao) {}

    private static final String ESTILO = """
            .root { -fx-background-color: white; }
            .app-header { -fx-background-color: #1e1e2e; -fx-padding: 16px 24px; }
            .brand-icon { -fx-text-fill: #1D9E75; -fx-font-size: 30px; }
            .brand-title { -fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; }
            .brand-subtitle { -fx-text-fill: #b9b9c9; -fx-font-size: 11px; }
            .status-bar { -fx-background-color: #f1f3f5; -fx-padding: 8px 18px; }
            .status-label { -fx-text-fill: #555b66; -fx-font-size: 12px; }
            .content-panel { -fx-background-color: white; }
            .form-card { -fx-background-color: #f8faf9; -fx-border-color: #e2e8e5; -fx-border-radius: 6px; -fx-background-radius: 6px; }
            .field-label, .section-title { -fx-text-fill: #1e1e2e; -fx-font-weight: bold; }
            .section-title { -fx-font-size: 14px; }
            .primary-button { -fx-background-color: #1D9E75; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4px; }
            .primary-button:hover { -fx-background-color: #168462; }
            .table-view { -fx-border-color: #e2e8e5; -fx-border-radius: 5px; }
            .table-view .column-header { -fx-background-color: #1e1e2e; -fx-padding: 10px 8px; }
            .table-view .column-header .label { -fx-text-fill: white; -fx-font-weight: bold; }
            .table-row-cell { -fx-cell-size: 40px; -fx-padding: 0 5px; }
            .table-row-cell.even-row { -fx-background-color: white; }
            .table-row-cell.odd-row { -fx-background-color: #f4f8f6; }
            .table-row-cell:selected { -fx-background-color: #bdebdc; -fx-text-fill: #1e1e2e; }
            .table-row-cell.estoque-zero { -fx-background-color: #ffd8d8; }
            .table-row-cell.estoque-baixo { -fx-background-color: #fff1bd; }
            .empty-state, .verdict { -fx-text-fill: #737b83; -fx-padding: 16px; }
            .verdict-ok { -fx-text-fill: #146d53; -fx-font-weight: bold; }
            .verdict-error { -fx-text-fill: #b42318; -fx-font-weight: bold; }
            .image-placeholder { -fx-background-color: #f1f3f5; -fx-border-color: #dce9e3; -fx-border-radius: 6px; }
            .image-placeholder .label { -fx-text-fill: #737b83; }
            .socket-list { -fx-padding: 4px 0 14px 0; }
            .socket-list .label { -fx-background-color: #d9f3e9; -fx-text-fill: #146d53; -fx-padding: 7px 11px; -fx-background-radius: 14px; }
            .compatible-list { -fx-padding: 4px 0; }
            .compatible-card { -fx-background-color: #f7faf8; -fx-border-color: #dce9e3; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 12px; }
            .compatibility-check { -fx-background-color: #1D9E75; -fx-background-radius: 20px; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 1px 6px; }
            .compatible-name { -fx-font-weight: bold; -fx-text-fill: #1e1e2e; }
            .compatible-details { -fx-text-fill: #69737a; -fx-font-size: 12px; }
            .compatible-price { -fx-text-fill: #146d53; -fx-font-weight: bold; }
            .tab-pane .tab-header-area { -fx-padding: 6px 10px 0 10px; }
            """;

    public static void main(String[] args) { launch(args); }
}
