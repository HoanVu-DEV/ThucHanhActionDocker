package com.example.Buoi3.service;

import com.example.Buoi3.Entity.Product;
import com.example.Buoi3.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for managing products.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/products/";

    /**
     * Retrieve all products from the database.
     * 
     * @return a list of products
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * Retrieve all discount products from the database.
     * 
     * @return a list of discount products
     */
    public List<Product> getDiscountProducts() {
        return productRepository.findByDiscountTrue();
    }

    /**
     * Retrieve products by category or its subcategories.
     * 
     * @param categoryId the id of the category
     * @return a list of products in the category and its subcategories
     */
    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryOrParentId(categoryId);
    }

    /**
     * Search products by name.
     * 
     * @param keyword the keyword to search for
     * @return a list of products matching the keyword
     */
    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllProducts();
        }
        return productRepository.findByNameContainingIgnoreCase(keyword);
    }

    /**
     * Retrieve a product by its id.
     * 
     * @param id the id of the product to retrieve
     * @return an Optional containing the found product or empty if not found
     */
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    /**
     * Add a new product to the database.
     * 
     * @param product the product to add
     */
    public void addProduct(Product product) {
        productRepository.save(product);
    }

    /**
     * Update an existing product.
     * 
     * @param product the product with updated information
     */
    public void updateProduct(@NotNull Product product) {
        Product existingProduct = productRepository.findById(product.getId())
                .orElseThrow(() -> new IllegalStateException("Product with ID " +
                        product.getId() + " does not exist."));
        existingProduct.setName(product.getName());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setImageUrl(product.getImageUrl());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setDiscount(product.getDiscount());
        existingProduct.setPromoPrice(product.getPromoPrice());
        existingProduct.setPromoQuantity(product.getPromoQuantity());
        productRepository.save(existingProduct);
    }

    /**
     * Delete a product by its id.
     * 
     * @param id the id of the product to delete
     */
    public void deleteProductById(Long id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalStateException("Product with ID " + id + " does not exist.");
        }
        productRepository.deleteById(id);
    }

    /**
     * Save product image and return the URL.
     * 
     * @param file the image file to save
     * @return the URL path to the saved image
     * @throws IOException if file saving fails
     */
    public String saveProductImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalStateException("Cannot upload empty file");
        }

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // Save file
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Return URL path
        return "/uploads/products/" + uniqueFilename;
    }
}
