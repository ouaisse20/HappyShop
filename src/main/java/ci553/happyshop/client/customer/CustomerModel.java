package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Order;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.utility.StorageLocation;
import ci553.happyshop.utility.ProductListFormatter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * TODO
 * You can either directly modify the CustomerModel class to implement the required tasks,
 * or create a subclass of CustomerModel and override specific methods where appropriate.
 */
public class CustomerModel {
    public CustomerView cusView;
    public DatabaseRW databaseRW; // Interface type, not specific implementation
    // Benefits: Flexibility: Easily change the database implementation.

    private Product theProduct = null;                 // product found from search
    private ArrayList<Product> trolley = new ArrayList<>(); // a list of products in trolley (grouped/merged)

    // Four UI elements to be passed to CustomerView for display updates.
    private String imageName = "imageHolder.jpg";                  // Image to show in product preview (Search Page)
    private String displayLaSearchResult = "No Product was searched yet"; // Label showing search result message (Search Page)
    private String displayTaTrolley = "";                          // Text area content showing current trolley items (Trolley Page)
    private String displayTaReceipt = "";                          // Text area content showing receipt after checkout (Receipt Page)

    // SELECT productID, description, image, unitPrice, inStock quantity
    void search() throws SQLException {
        String productId = cusView.tfId.getText().trim();

        if (!productId.isEmpty()) {
            theProduct = databaseRW.searchByProductId(productId); // search database

            if (theProduct != null && theProduct.getStockQuantity() > 0) {
                double unitPrice = theProduct.getUnitPrice();
                String description = theProduct.getProductDescription();
                int stock = theProduct.getStockQuantity();

                String baseInfo = String.format("Product_Id: %s\n%s,\nPrice: £%.2f", productId, description, unitPrice);
                String quantityInfo = stock < 100 ? String.format("\n%d units left.", stock) : "";
                displayLaSearchResult = baseInfo + quantityInfo;

                System.out.println(displayLaSearchResult);
            } else {
                theProduct = null;
                displayLaSearchResult = "No Product was found with ID " + productId;
                System.out.println(displayLaSearchResult);
            }
        } else {
            theProduct = null;
            displayLaSearchResult = "Please type ProductID";
            System.out.println(displayLaSearchResult);
        }

        updateView();
    }

    void addToTrolley() {
        if (theProduct != null) {

            // Add selected product
            trolley.add(theProduct);

            // 1) Merge duplicates by product ID (combine quantities)
            trolley = groupProductsById(trolley);

            // 2) Sort by product ID
            trolley.sort(Comparator.comparing(Product::getProductId));

            // Show trolley
            displayTaTrolley = ProductListFormatter.buildString(trolley);

        } else {
            displayLaSearchResult = "Please search for an available product before adding it to the trolley";
            System.out.println("must search and get an available product before add to trolley");
        }

        // Clear receipt so UI stays on trolley page
        displayTaReceipt = "";
        updateView();
    }

    void checkOut() throws IOException, SQLException {
        if (trolley.isEmpty()) {
            displayTaTrolley = "Your trolley is empty";
            System.out.println("Your trolley is empty");
            updateView();
            return;
        }

        // (Trolley is already grouped/merged, but grouping again is safe.)
        ArrayList<Product> groupedTrolley = groupProductsById(trolley);

        // Attempt purchase. If insufficient stock, DB should roll back and return the insufficient list.
        ArrayList<Product> insufficientProducts = databaseRW.purchaseStocks(groupedTrolley);

        if (insufficientProducts.isEmpty()) {
            // Stock sufficient -> create order
            OrderHub orderHub = OrderHub.getOrderHub();
            Order theOrder = orderHub.newOrder(trolley);

            trolley.clear();
            displayTaTrolley = "";
            displayTaReceipt = String.format(
                    "Order_ID: %s\nOrdered_Date_Time: %s\n%s",
                    theOrder.getOrderId(),
                    theOrder.getOrderedDateTime(),
                    ProductListFormatter.buildString(theOrder.getProductList())
            );

            System.out.println(displayTaReceipt);

        } else {
            // Build message for user
            StringBuilder errorMsg = new StringBuilder();
            Set<String> insufficientIds = new HashSet<>();

            for (Product p : insufficientProducts) {
                insufficientIds.add(p.getProductId());
                errorMsg.append("\u2022 ")
                        .append(p.getProductId()).append(", ")
                        .append(p.getProductDescription())
                        .append(" (Only ")
                        .append(p.getStockQuantity()).append(" available, ")
                        .append(p.getOrderedQuantity()).append(" requested)\n");
            }

            // 1) Remove insufficient products from trolley
            trolley.removeIf(p -> insufficientIds.contains(p.getProductId()));

            // Rebuild trolley display text
            if (trolley.isEmpty()) {
                displayTaTrolley = "Your trolley is empty (items removed due to insufficient stock)";
            } else {
                displayTaTrolley = ProductListFormatter.buildString(trolley);
            }

            // Clear current selected product
            theProduct = null;

            // 2) Show message window (instead of changing the label as the main feedback)
            RemoveProductNotifier notifier = new RemoveProductNotifier();
            notifier.showRemovalMsg(errorMsg.toString());


            // Keep label simple (don’t dump big message into the label)
            displayLaSearchResult = "Some items were removed from your trolley due to insufficient stock.";
            System.out.println("stock is not enough - removed insufficient items");
        }

        updateView();
    }

    /**
     * Groups products by productId (merges duplicates by increasing orderedQuantity).
     */
    private ArrayList<Product> groupProductsById(ArrayList<Product> proList) {
        Map<String, Product> grouped = new HashMap<>();

        for (Product p : proList) {
            String id = p.getProductId();

            if (grouped.containsKey(id)) {
                Product existing = grouped.get(id);
                existing.setOrderedQuantity(existing.getOrderedQuantity() + p.getOrderedQuantity());
            } else {
                // Copy so we don’t mutate the original object references unexpectedly
                Product copy = new Product(
                        p.getProductId(),
                        p.getProductDescription(),
                        p.getProductImageName(),
                        p.getUnitPrice(),
                        p.getStockQuantity()
                );
                copy.setOrderedQuantity(p.getOrderedQuantity());
                grouped.put(id, copy);
            }
        }

        return new ArrayList<>(grouped.values());
    }

    void cancel() {
        trolley.clear();
        displayTaTrolley = "";
        updateView();
    }

    void closeReceipt() {
        displayTaReceipt = "";
    }

    void updateView() {
        if (cusView == null) return;

        if (theProduct != null) {
            imageName = theProduct.getProductImageName();
            String relativeImageUrl = StorageLocation.imageFolder + imageName; // e.g., images/0001.jpg
            Path imageFullPath = Paths.get(relativeImageUrl).toAbsolutePath();
            imageName = imageFullPath.toUri().toString();
            System.out.println("Image absolute path: " + imageFullPath);
        } else {
            imageName = "imageHolder.jpg";
        }

        cusView.update(imageName, displayLaSearchResult, displayTaTrolley, displayTaReceipt);
    }

    // for test only
    public ArrayList<Product> getTrolley() {
        return trolley;
    }
}
