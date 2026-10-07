package vn.edu.crs.tinhhoataybac.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.crs.tinhhoataybac.repository.CategoryRepository;

@Controller
public class CategoryBrowseController {
  private final CategoryRepository categories;

  public CategoryBrowseController(CategoryRepository categories) {
    this.categories = categories;
  }

  @GetMapping("/categories")
  public String categories(Model m) {
    m.addAttribute("categories", categories.findAll());
    return "categories";
  }

  @GetMapping("/categories/{id}")
  public String category(@PathVariable Long id) {
    return "redirect:/products?categoryId=" + id;
  }
}
