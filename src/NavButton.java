
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;

class NavButton extends Button {
    public NavButton(String text, String id, EventHandler<ActionEvent> action) {
        super(text);
        setId(id);
        setMaxWidth(Double.MAX_VALUE);
        setAlignment(Pos.CENTER_LEFT);
        setOnAction(action);

        String baseStyle =
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 12 20;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;";

        setStyle(baseStyle);

        setOnMouseEntered(e -> setStyle(baseStyle + "-fx-background-color: rgba(255,255,255,0.1);"));
        setOnMouseExited(e -> setStyle(baseStyle));
    }
}