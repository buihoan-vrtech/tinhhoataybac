package vn.edu.crs.tinhhoataybac.model;

public class CartItem {

    private Product product;

    private double quantity;

    public CartItem() {
    }

    public CartItem(Product product, double quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getSubtotal() {

        if (product == null || product.getEffectivePrice() == null) {
            return 0;
        }

        return product.getEffectivePrice() * quantity;
    }
}