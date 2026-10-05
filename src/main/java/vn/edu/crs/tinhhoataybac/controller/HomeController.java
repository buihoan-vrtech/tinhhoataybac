package vn.edu.crs.tinhhoataybac.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.crs.tinhhoataybac.service.CategoryService;
import vn.edu.crs.tinhhoataybac.service.ProductService;

@Controller
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public HomeController(ProductService productService,
                          CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping("/")
    public String home(Model model) {

        model.addAttribute("categories",
                categoryService.getAllCategories());

        model.addAttribute("products",
                productService.getFeaturedProducts());

        return "index";
    }
}