package gui;

import core.PasswordGenerator;
// Import Stage and Scene
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.util.function.UnaryOperator;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
// REMOVED: Tab and TabPane imports
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane; // ADDED
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;

/**
 * A custom window (Stage) for generating secure passwords and passphrases.
 * It is styl-ed using the provided ThemeManager.
 */
// MODIFIED: Changed from 'extends Dialog<String>' to 'extends Stage'
public class PasswordGeneratorDialog extends Stage {
    private final TextField resultField;
    private final Button generatePasswordButton;
    private final TextField lengthField;

    private double xOffset = 0;
    private double yOffset = 0;

    public PasswordGeneratorDialog(ThemeManager themeManager, PasswordField targetPasswordField) {

        initStyle(StageStyle.TRANSPARENT);
        
        VBox rootLayout = new VBox(15);
        rootLayout.setStyle("-fx-background-color: " + themeManager.getCurrentBaseSemiTransparent() + "; -fx-background-radius: 15;");
        rootLayout.setEffect(themeManager.getLightOuterShadow());
        rootLayout.setPadding(new Insets(10));
        setMinWidth(400);

        rootLayout.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        rootLayout.setOnMouseDragged(event -> {
            setX(event.getScreenX() - xOffset);
            setY(event.getScreenY() - yOffset);
        });
        
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
        
        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) {
                return change;
            }
            return null;
        };

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

        Button passwordModeButton = themeManager.createStyledButton("Password");
        Button passphraseModeButton = themeManager.createStyledButton("Passphrase");
        
        HBox modeToggleBar = new HBox(5, passwordModeButton, passphraseModeButton);
        modeToggleBar.setAlignment(Pos.CENTER_LEFT);
        
        StackPane contentStack = new StackPane(passwordTabContent, passphraseTabContent);
        contentStack.setPadding(new Insets(10, 0, 0, 0));
        
        passwordModeButton.setOnAction(e -> {
            passwordTabContent.setVisible(true);
            passphraseTabContent.setVisible(false);
            passwordModeButton.setEffect(themeManager.getLightInnerShadow());
            passphraseModeButton.setEffect(themeManager.getLightOuterShadow());
        });
        
        passphraseModeButton.setOnAction(e -> {
            passwordTabContent.setVisible(false);
            passphraseTabContent.setVisible(true);
            passwordModeButton.setEffect(themeManager.getLightOuterShadow());
            passphraseModeButton.setEffect(themeManager.getLightInnerShadow());
        });
        
        passwordModeButton.setEffect(themeManager.getLightInnerShadow());
        passphraseTabContent.setVisible(false);

        VBox layout = new VBox(15, modeToggleBar, contentStack, resultBox);
        
        Button useButton = themeManager.createStyledButton("Use This");
        themeManager.styleIconButton(useButton, null); // Apply base style
        useButton.setText("Use This"); // Set text
        useButton.setStyle(useButton.getStyle() + " -fx-text-fill: " + themeManager.getCurrentSuccessColor() + ";");
        useButton.setPrefWidth(100);
        
        Button cancelButton = themeManager.createStyledButton("Cancel");
        cancelButton.setPrefWidth(100);

        HBox buttonBar = new HBox(10, useButton, cancelButton);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        cancelButton.setOnAction(e -> close());

        useButton.setOnAction(e -> {
            String password = resultField.getText();
            if (password != null && !password.isEmpty()) {
                targetPasswordField.setText(password);
            }
            close();
        });
        rootLayout.getChildren().addAll(layout, buttonBar);

        Scene scene = new Scene(rootLayout);
        scene.setFill(Color.TRANSPARENT);
        setScene(scene);
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
        show();
    }
}