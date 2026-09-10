package gui;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class PreferencesManager {
    private static final Path FILE = Path.of("src", "main", "resources", "preferences.properties");
    private PreferencesManager() {}
    public static boolean darkTheme() {
        Properties properties = carregar();
        return Boolean.parseBoolean(properties.getProperty("darkTheme", "true"));
    }
    public static void setDarkTheme(boolean value) {
        Properties properties = carregar();
        properties.setProperty("darkTheme", Boolean.toString(value));
        salvar(properties);
    }
    public static int pageSize() {
        try { return Integer.parseInt(carregar().getProperty("componentPageSize", "15")); }
        catch (NumberFormatException e) { return 15; }
    }
    public static void setPageSize(int value) {
        Properties properties = carregar();
        properties.setProperty("componentPageSize", Integer.toString(value));
        salvar(properties);
    }
    public static boolean componentCards() {
        return carregar().getProperty("componentView", "table").equalsIgnoreCase("cards");
    }
    public static void setComponentCards(boolean value) {
        Properties properties = carregar();
        properties.setProperty("componentView", value ? "cards" : "table");
        salvar(properties);
    }
    public static double fontSize() {
        try { double value = Double.parseDouble(carregar().getProperty("fontSize", "12")); return Double.isFinite(value) ? Math.max(10, Math.min(16, value)) : 12; }
        catch (NumberFormatException e) { return 12; }
    }
    public static void setFontSize(double value) {
        Properties properties = carregar();
        properties.setProperty("fontSize", Double.toString(Math.max(10, Math.min(16, value))));
        salvar(properties);
    }
    private static Properties carregar() {
        Properties properties = new Properties();
        if (Files.exists(FILE)) {
            try (Reader input = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) { properties.load(input); }
            catch (IOException e) { System.err.println("Não foi possível ler preferências: " + e.getMessage()); }
        }
        return properties;
    }
    private static void salvar(Properties properties) {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer output = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                properties.store(output, "Nexus Studio preferences");
            }
        } catch (IOException e) {
            System.err.println("Não foi possível salvar preferências: " + e.getMessage());
        }
    }
}
