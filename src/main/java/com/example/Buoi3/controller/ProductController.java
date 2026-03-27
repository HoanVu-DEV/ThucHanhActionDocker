package com.example.Buoi3.controller;

import com.example.Buoi3.Entity.Product;
import com.example.Buoi3.service.CategoryService;
import com.example.Buoi3.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping("/")
    public String home(Model model) {
        return listProducts(null, "all", null, model);
    }

    @GetMapping("/products")
    public String listProducts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "type", required = false, defaultValue = "all") String type,
            @RequestParam(value = "category", required = false) Long categoryId,
            Model model) {
        List<Product> products;
        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productService.searchProducts(keyword);
            model.addAttribute("keyword", keyword);
        } else if ("discount".equals(type)) {
            products = productService.getDiscountProducts();
        } else if (categoryId != null) {
            products = productService.getProductsByCategory(categoryId);
            model.addAttribute("selectedCategoryId", categoryId);
        } else {
            products = productService.getAllProducts();
        }
        model.addAttribute("products", products);
        model.addAttribute("type", type);
        model.addAttribute("parentCategories", categoryService.getParentCategories());
        return "products/products-list";
    }

    @GetMapping("/products/detail/{id}")
    public String viewProductDetail(@PathVariable("id") Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product Id: " + id));
        model.addAttribute("product", product);
        model.addAttribute("parentCategories", categoryService.getParentCategories());
        return "products/product-detail";
    }

    @GetMapping("/products/add")
    public String showAddForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "products/add-product";
    }

    @PostMapping("/products/add")
    public String addProduct(
            @Valid @ModelAttribute Product product,
            BindingResult result,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllCategories());
            return "products/add-product";
        }

        try {
            // Handle image upload
            if (imageFile != null && !imageFile.isEmpty()) {
                String imageUrl = productService.saveProductImage(imageFile);
                product.setImageUrl(imageUrl);
            } else {
                // Set default image if no image uploaded
                product.setImageUrl("/uploads/products/default.jpg");
            }

            productService.addProduct(product);
            return "redirect:/products";
        } catch (IOException e) {
            model.addAttribute("error", "Lỗi khi tải ảnh lên: " + e.getMessage());
            model.addAttribute("categories", categoryService.getAllCategories());
            return "products/add-product";
        }
    }

    @GetMapping("/products/edit/{id}")
    public String showUpdateForm(@PathVariable("id") Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product Id: " + id));

        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.getAllCategories());
        return "products/update-product";
    }

    @PostMapping("/products/update/{id}")
    public String updateProduct(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute Product product,
            BindingResult result,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "currentImageUrl", required = false) String currentImageUrl,
            Model model) {

        if (result.hasErrors()) {
            product.setId(id);
            model.addAttribute("categories", categoryService.getAllCategories());
            return "products/update-product";
        }

        try {
            // Handle image upload
            if (imageFile != null && !imageFile.isEmpty()) {
                String imageUrl = productService.saveProductImage(imageFile);
                product.setImageUrl(imageUrl);
            } else {
                // Keep current image if no new image uploaded
                product.setImageUrl(currentImageUrl);
            }

            product.setId(id);
            productService.updateProduct(product);
            return "redirect:/products";
        } catch (IOException e) {
            model.addAttribute("error", "Lỗi khi tải ảnh lên: " + e.getMessage());
            model.addAttribute("categories", categoryService.getAllCategories());
            return "products/update-product";
        }
    }

    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable("id") Long id) {
        productService.deleteProductById(id);
        return "redirect:/products";
    }
}
