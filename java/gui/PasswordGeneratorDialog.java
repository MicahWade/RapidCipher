package gui;

import core.PasswordGenerator;
import java.util.Optional;
import java.util.function.UnaryOperator;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;

/**
 * A custom dialog for generating secure passwords and passphrases.
 * It is styl-ed using the provided ThemeManager.
 */
public class PasswordGeneratorDialog extends Dialog<String> {

    private final ThemeManager themeManager;
    private final PasswordField targetPasswordField;
    private final TextField resultField;
    private final Button generatePasswordButton;
    private final TextField lengthField;

    public PasswordGeneratorDialog(ThemeManager themeManager, PasswordField targetPasswordField) {
        super();
        this.themeManager = themeManager;
        this.targetPasswordField = targetPasswordField;

        initStyle(StageStyle.TRANSPARENT);
        getDialogPane().getScene().setFill(Color.TRANSPARENT);
        getDialogPane().setStyle("-fx-background-color: " + themeManager.getCurrentBaseSemiTransparent() + "; -fx-background-radius: 15;");
        getDialogPane().setEffect(themeManager.getLightOuterShadow());
        getDialogPane().setPrefWidth(400);

        setTitle("Password Generator");

        resultField = themeManager.createStyledTextField("Generated Password");
        resultField.setEditable(false);
        Button copyButton = themeManager.createCopyButton();
        copyButton.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(resultField.getText());
            clipboard.setContent(content);
        });
        HBox resultBox = new HBox(10, resultField, copyButton);
        HBox.setHgrow(resultField, Priority.ALWAYS);
        
        // --- Filter for numeric input ---
        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) { // Allow empty or all digits
                return change;
            }
            return null;
        };

        // --- Password Tab ---
        VBox passwordTabContent = new VBox(15);
        passwordTabContent.setPadding(new Insets(10));
        
        Label lengthLabel = new Label("Length:");
        lengthLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        
        lengthField = themeManager.createStyledTextField("16");
        lengthField.setText("16");
        lengthField.setPrefWidth(70);
        lengthField.setTextFormatter(new TextFormatter<>(integerFilter));

        HBox lengthBox = new HBox(10, lengthLabel, lengthField);
        lengthBox.setAlignment(Pos.CENTER_LEFT);
        
        CheckBox upperCheck = themeManager.createStyledCheckBox("Uppercase (A-Z)");
        upperCheck.setSelected(true);
        CheckBox digitsCheck = themeManager.createStyledCheckBox("Digits (0-9)");
        digitsCheck.setSelected(true);
        CheckBox symbolsCheck = themeManager.createStyledCheckBox("Symbols (!@#...)");
        symbolsCheck.setSelected(true);
        
        Label minDigitsLabel = new Label("Min Digits:");
        minDigitsLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        Spinner<Integer> minDigitsSpinner = new Spinner<>(0, 10, 1);
        minDigitsSpinner.setPrefWidth(70);
        minDigitsSpinner.disableProperty().bind(digitsCheck.selectedProperty().not());
        
        Label minSymbolsLabel = new Label("Min Symbols:");
        minSymbolsLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        Spinner<Integer> minSymbolsSpinner = new Spinner<>(0, 10, 1);
        minSymbolsSpinner.setPrefWidth(70);
        minSymbolsSpinner.disableProperty().bind(symbolsCheck.selectedProperty().not());
        
        HBox minBox = new HBox(10, minDigitsLabel, minDigitsSpinner, minSymbolsLabel, minSymbolsSpinner);
        minBox.setAlignment(Pos.CENTER_LEFT);
        
        generatePasswordButton = themeManager.createStyledButton("Generate Password");
        generatePasswordButton.setOnAction(e -> {
            int minLower = 1;
            int minUpper = upperCheck.isSelected() ? 1 : 0;
            int minDigits = digitsCheck.isSelected() ? minDigitsSpinner.getValue() : 0;
            int minSymbols = symbolsCheck.isSelected() ? minSymbolsSpinner.getValue() : 0;
            int totalMin = minLower + minUpper + minDigits + minSymbols;

            int length = 16;
            try {
                length = Integer.parseInt(lengthField.getText());
            } catch (NumberFormatException ex) {
                // Keep default
            }

            if (length < Math.max(8, totalMin)) {
                length = Math.max(8, totalMin);
                lengthField.setText(String.valueOf(length));
            }
            
            resultField.setText(PasswordGenerator.generatePassword(
                length,
                upperCheck.isSelected(),
                digitsCheck.isSelected(),
                symbolsCheck.isSelected(),
                minDigitsSpinner.getValue(),
                minSymbolsSpinner.getValue()
            ));
        });
        
        passwordTabContent.getChildren().addAll(
            lengthBox,
            upperCheck, digitsCheck, symbolsCheck,
            minBox,
            generatePasswordButton
        );
        passwordTabContent.setStyle("-fx-background-color: " + themeManager.getCurrentBaseColor() + ";");

        // --- Passphrase Tab ---
        VBox passphraseTabContent = new VBox(15);
        passphraseTabContent.setPadding(new Insets(10));
        
        Label wordsLabel = new Label("Words:");
        wordsLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        
        TextField wordsField = themeManager.createStyledTextField("4");
        wordsField.setText("4");
        wordsField.setPrefWidth(70);
        wordsField.setTextFormatter(new TextFormatter<>(integerFilter));

        HBox wordsBox = new HBox(10, wordsLabel, wordsField);
        wordsBox.setAlignment(Pos.CENTER_LEFT);

        Label separatorLabel = new Label("Separator:");
        separatorLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
        TextField separatorField = themeManager.createStyledTextField("-");
        separatorField.setMaxWidth(100);
        HBox separatorBox = new HBox(10, separatorLabel, separatorField);
        separatorBox.setAlignment(Pos.CENTER_LEFT);
        
        Button generatePassphraseButton = themeManager.createStyledButton("Generate Passphrase");
        generatePassphraseButton.setOnAction(e -> {
            int numWords = 4;
            try {
                numWords = Integer.parseInt(wordsField.getText());
            } catch (NumberFormatException ex) {
                // Keep default
            }
            
            if (numWords < 3) {
                numWords = 3;
                wordsField.setText("3");
            }
            
            resultField.setText(PasswordGenerator.generatePassphrase(
                numWords,
                separatorField.getText()
            ));
        });
        
        passphraseTabContent.getChildren().addAll(
            wordsBox,
            separatorBox,
            generatePassphraseButton
        );
        passphraseTabContent.setStyle("-fx-background-color: " + themeManager.getCurrentBaseColor() + ";");
        
        // --- Tab Pane ---
        TabPane tabPane = new TabPane();
        Tab passwordTab = new Tab("Password", passwordTabContent);
        Tab passphraseTab = new Tab("Passphrase", passphraseTabContent);
        passwordTab.setClosable(false);
        passphraseTab.setClosable(false);
        tabPane.getTabs().addAll(passwordTab, passphraseTab);
        themeManager.styleTabPane(tabPane);
        
        // --- Main Layout ---
        VBox layout = new VBox(15, tabPane, resultBox);
        layout.setPadding(new Insets(10));
        getDialogPane().setContent(layout);

        ButtonType useButtonType = new ButtonType("Use This", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(useButtonType, ButtonType.CANCEL);
        
        Button useButton = (Button) getDialogPane().lookupButton(useButtonType);
        themeManager.styleIconButton(useButton, null);
        useButton.setText("Use This");
        useButton.setStyle(useButton.getStyle() + " -fx-text-fill: " + themeManager.getCurrentSuccessColor() + ";");
        useButton.setPrefWidth(100);
        
        Button cancelButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        themeManager.styleIconButton(cancelButton, null);
        cancelButton.setText("Cancel");
        cancelButton.setPrefWidth(100);

        setResultConverter(dialogButton -> {
            if (dialogButton == useButtonType) {
                return resultField.getText();
            }
            return null;
        });
        
        // Generate a password on open
        Platform.runLater(() -> {
            generatePasswordButton.fire();
            lengthField.requestFocus();
        });
    }

    /**
     * Shows the dialog and updates the target PasswordField if a password is
     * generated and accepted.
     */
    public void showDialog() {
        Optional<String> result = showAndWait();
        result.ifPresent(password -> {
            if (password != null && !password.isEmpty()) {
                targetPasswordField.setText(password);
            }
        });
    }
}