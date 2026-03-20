package com.example.Buoi3.controller;

import com.example.Buoi3.Entity.Category;
import com.example.Buoi3.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/categories/add")
    public String showAddForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("parentCategories", categoryService.getParentCategories());
        return "categories/add-category";
    }

    @PostMapping("/categories/add")
    public String addCategory(@Valid Category category, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("parentCategories", categoryService.getParentCategories());
            return "categories/add-category";
        }
        categoryService.addCategory(category);
        return "redirect:/categories";
    }

    // REST endpoint for AJAX category creation
    @PostMapping("/api/categories")
    @ResponseBody
    public ResponseEntity<?> createCategoryAjax(@RequestBody Category category) {
        try {
            if (category.getName() == null || category.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Tên danh mục không được để trống");
            }
            categoryService.addCategory(category);
            return ResponseEntity.ok(category);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping("/categories")
    public String listCategories(Model model) {
        List<Category> parentCategories = categoryService.getParentCategories();
        model.addAttribute("parentCategories", parentCategories);
        model.addAttribute("allCategories", categoryService.getAllCategories());
        return "categories/categories-list";
    }

    @GetMapping("/api/categories/{parentId}/subcategories")
    @ResponseBody
    public ResponseEntity<List<Category>> getSubcategories(@PathVariable Long parentId) {
        List<Category> subcategories = categoryService.getSubcategoriesByParentId(parentId);
        return ResponseEntity.ok(subcategories);
    }

    @GetMapping("/categories/edit/{id}")
    public String showUpdateForm(@PathVariable("id") Long id, Model model) {
        Category category = categoryService.getCategoryById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category Id: " + id));

        model.addAttribute("category", category);
        model.addAttribute("parentCategories", categoryService.getParentCategories());
        return "categories/update-category";
    }

    @PostMapping("/categories/update/{id}")
    public String updateCategory(
            @PathVariable("id") Long id,
            @Valid Category category,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            category.setId(id);
            model.addAttribute("parentCategories", categoryService.getParentCategories());
            return "categories/update-category";
        }

        categoryService.updateCategory(category);
        return "redirect:/categories";
    }

    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable("id") Long id) {
        categoryService.deleteCategoryById(id);
        return "redirect:/categories";
    }
}
