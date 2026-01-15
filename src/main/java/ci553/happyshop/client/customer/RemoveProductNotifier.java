package ci553.happyshop.client.customer;

import ci553.happyshop.utility.UIStyle;
import ci553.happyshop.utility.WindowBounds;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * The RemoveProductNotifier class provides a dependent window that displays messages
 * and suggested actions to the customer when certain products are removed from their trolley.
 *
 * Key fixes:
 * - Never crash if cusView is null (fallback to centered window).
 * - Never crash if window was closed (recreate window safely).
 * - Always ensure taRemoveMsg exists before setting text.
 * - OK button closes the window safely.
 */
public class RemoveProductNotifier {

    public CustomerView cusView; // tracking the window of cusView (optional)

    private static final int WIDTH = UIStyle.removeProNotifierWinWidth;
    private static final int HEIGHT = UIStyle.removeProNotifierWinHeight;

    private Stage window;     // notifier stage
    private Scene scene;      // notifier scene (reused)
    private TextArea taRemoveMsg; // message area

    /** Create the Scene (only once) */
    private void createScene() {
        Label laTitle = new Label("\u26A0 Some changes have been made to your trolley."); // ⚠️
        laTitle.setStyle(UIStyle.alertTitleLabelStyle);

        taRemoveMsg = new TextArea();
        taRemoveMsg.setEditable(false);
        taRemoveMsg.setWrapText(true);
        taRemoveMsg.setPrefHeight(80);
        taRemoveMsg.setStyle(UIStyle.alertContentTextAreaStyle);

        Label laCustomerAction = new Label(customerActionBuilder());
        laCustomerAction.setWrapText(true);
        laCustomerAction.setStyle(UIStyle.alertContentUserActionStyle);

        Button btnOk = new Button("Ok");
        btnOk.setStyle(UIStyle.alertBtnStyle);
        btnOk.setOnAction(e -> {
            if (window != null) {
                window.close();
            }
        });

        HBox hbCustomerAction = new HBox(20, laCustomerAction, btnOk);
        hbCustomerAction.setAlignment(Pos.CENTER_LEFT);

        GridPane pane = new GridPane();
        pane.setHgap(5);
        pane.setVgap(5);
        pane.add(laTitle, 0, 0);
        pane.add(taRemoveMsg, 0, 1);
        pane.add(hbCustomerAction, 0, 2);
        pane.setStyle(UIStyle.rootStyleGray);

        scene = new Scene(pane, WIDTH, HEIGHT);
    }

    private String customerActionBuilder() {
        StringBuilder actions = new StringBuilder(" \u26A1 You can now: \n");
        actions.append("\u2022 Checkout your trolley as it is \n");
        actions.append("\u2022 Re-add the removed products (up to the available quantity) \n");
        actions.append("\u2022 Or cancel your trolley if you no longer wish to proceed.\n");
        actions.append("Thank you for understanding! \n");
        return actions.toString();
    }

    /** Ensure scene exists */
    private void ensureScene() {
        if (scene == null || taRemoveMsg == null) {
            createScene();
        }
    }

    /** Create (or recreate) the window safely */
    private void createWindow() {
        ensureScene();

        window = new Stage();
        window.initModality(Modality.NONE);
        window.setTitle("🛒 Products removal notifier");
        window.setScene(scene);

        // If cusView exists, place near it. Otherwise, center on screen.
        try {
            if (cusView != null) {
                WindowBounds bounds = cusView.getWindowBounds();
                window.setX(bounds.x + bounds.width - WIDTH - 10);
                window.setY(bounds.y + bounds.height / 2 + 40);
            } else {
                window.centerOnScreen();
            }
        } catch (Exception ex) {
            // Any issue reading bounds → just center
            window.centerOnScreen();
        }

        // If user clicks X, mark window as gone so we recreate next time
        window.setOnHidden(e -> window = null);
    }

    /** Show remove product message */
    public void showRemovalMsg(String removalMsg) {
        ensureScene();

        if (window == null || !window.isShowing()) {
            createWindow();
            window.show();
        }

        taRemoveMsg.setText(removalMsg == null ? "" : removalMsg);
        window.toFront();
    }

    /** Close notifier window from outside */
    public void closeNotifierWindow() {
        if (window != null && window.isShowing()) {
            window.close();
        }
        window = null;
    }
}
