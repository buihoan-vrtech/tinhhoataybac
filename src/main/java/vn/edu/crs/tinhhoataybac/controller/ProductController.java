package vn.edu.crs.tinhhoataybac.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.crs.tinhhoataybac.model.Product;
import vn.edu.crs.tinhhoataybac.service.CategoryService;
import vn.edu.crs.tinhhoataybac.service.ProductService;

import java.util.List;

@Controller
public class ProductController {
    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Long id, Model model) {

        Product product = productService.getProductById(id);

        if (product == null) {
            return "redirect:/products";
        }

        model.addAttribute("product", product);

        return "product-detail";
    }
    private final ProductService productService;
    private final CategoryService categoryService;

    public ProductController(ProductService productService,
                             CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping("/products")
    public String products(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            Model model) {

        List<Product> products;

        if (keyword != null && !keyword.trim().isEmpty()) {

            products = productService.searchProducts(keyword);

        } else if (categoryId != null) {

            products = productService.getProductsByCategory(categoryId);

        } else {

            products = productService.getAllProducts();
        }

        model.addAttribute("products", products);

        model.addAttribute(
                "categories",
                categoryService.getAllCategories()
        );

        model.addAttribute("keyword", keyword);

        model.addAttribute("categoryId", categoryId);

        return "products";
    }
}