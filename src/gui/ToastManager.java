package gui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Toasts não modais, empilhados e com descarte automático. */
public final class ToastManager {
    private final VBox container;
    public ToastManager(VBox container) {
        this.container = container;
        container.setAlignment(Pos.TOP_RIGHT);
        container.setMouseTransparent(false);
        container.setSpacing(8);
    }
    public void showUndo(String message, Runnable undo) {
        HBox toast = new HBox(10);
        Label texto = new Label(message); texto.setWrapText(true); texto.setMaxWidth(300);
        Button botao = new Button("Desfazer"); botao.setOnAction(e -> { undo.run(); container.getChildren().remove(toast); });
        toast.getChildren().addAll(texto, botao); toast.setAlignment(Pos.CENTER_RIGHT);
        toast.getStyleClass().add("toast-success"); container.getChildren().add(toast);
        PauseTransition pause = new PauseTransition(Duration.seconds(10));
        pause.setOnFinished(e -> container.getChildren().remove(toast)); pause.play();
    }

    public void show(String message, boolean error) {
        Label toast = new Label(message);
        toast.setWrapText(true);
        toast.setMaxWidth(420);
        toast.getStyleClass().add(error ? "toast-error" : "toast-success");
        container.getChildren().add(toast);
        FadeTransition in = new FadeTransition(Duration.millis(160), toast);
        in.setFromValue(0); in.setToValue(1); in.play();
        PauseTransition pause = new PauseTransition(Duration.seconds(3.5));
        pause.setOnFinished(e -> {
            FadeTransition out = new FadeTransition(Duration.millis(350), toast);
            out.setToValue(0);
            out.setOnFinished(done -> container.getChildren().remove(toast));
            out.play();
        });
        pause.play();
    }
}

