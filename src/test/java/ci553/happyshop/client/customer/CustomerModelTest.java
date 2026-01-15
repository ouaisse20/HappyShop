package customer;

import ci553.happyshop.catalogue.Product;
import ci553.happyshop.client.customer.CustomerModel;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class CustomerModelTest {

    private void injectProduct(CustomerModel model, Product product) throws Exception {
        Field field = CustomerModel.class.getDeclaredField("theProduct");
        field.setAccessible(true);
        field.set(model, product);
    }

    @Test
    void trolleyMergesDuplicateProducts() throws Exception {
        CustomerModel model = new CustomerModel();

        Product p1 = new Product("0002", "DAB Radio", "0002.jpg", 29.99, 50);
        Product p2 = new Product("0002", "DAB Radio", "0002.jpg", 29.99, 50);

        injectProduct(model, p1);
        model.addToTrolley();

        injectProduct(model, p2);
        model.addToTrolley();

        ArrayList<Product> trolley = model.getTrolley();

        assertEquals(1, trolley.size(), "Duplicate products should be merged");
        assertEquals(2, trolley.get(0).getOrderedQuantity(), "Quantity should be combined");
    }

    @Test
    void trolleyIsSortedByProductId() throws Exception {
        CustomerModel model = new CustomerModel();

        Product p4 = new Product("0004", "Watch", "0004.jpg", 29.99, 50);
        Product p2 = new Product("0002", "DAB Radio", "0002.jpg", 29.99, 50);

        injectProduct(model, p4);
        model.addToTrolley();

        injectProduct(model, p2);
        model.addToTrolley();

        ArrayList<Product> trolley = model.getTrolley();

        assertEquals("0002", trolley.get(0).getProductId());
        assertEquals("0004", trolley.get(1).getProductId());
    }
}
