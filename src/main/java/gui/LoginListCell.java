package gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LoginListCell extends ListCell<LoginEntry> {
    private VBox content;
    private Label nameLabel;
    private Label usernameLabel;
    private ThemeManager themeManager;
    
    public LoginListCell(ThemeManager themeManager) {
        super();
        this.themeManager = themeManager;

        nameLabel = new Label();
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        usernameLabel = new Label();

        content = new VBox(5, nameLabel, usernameLabel);
        content.setPadding(new Insets(10));
        content.setAlignment(Pos.CENTER_LEFT);
        
        setStyle("-fx-background-color: transparent; -fx-padding: 5 10 5 10;"); 
    }

    @Override
    protected void updateItem(LoginEntry item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            nameLabel.setText(item.getName());
            nameLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentTextColor() + ";");
            
            usernameLabel.setText(item.getUsername());
            usernameLabel.setStyle("-fx-text-fill: " + themeManager.getCurrentMutedTextColor() + ";");
            
            String style = "-fx-background-color: " + themeManager.getCurrentBaseColor() + "; -fx-background-radius: 10;";
            content.setStyle(style);
            
            if (isSelected()) {
                content.setEffect(themeManager.getLightInnerShadow()); 
            } else {
                content.setEffect(themeManager.getLightOuterShadow());
            }
            
            setGraphic(content);
        }
    }
}