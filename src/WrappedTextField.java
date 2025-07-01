import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

// Custom class to wrap text field with floating label
class WrappedTextField extends TextField {
    private final StackPane container;

    public WrappedTextField(StackPane container) {
        this.container = container;
    }

    @Override
    public Node getStyleableNode() {
        return container;
    }
}