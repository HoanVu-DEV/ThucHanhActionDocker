package com.example.Buoi3.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseRepairRunner implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            // Check and add 'phone' column
            jdbcTemplate.execute("IF COL_LENGTH('orders', 'phone') IS NULL ALTER TABLE orders ADD phone VARCHAR(255)");
            // Check and add 'address' column
            jdbcTemplate.execute("IF COL_LENGTH('orders', 'address') IS NULL ALTER TABLE orders ADD address VARCHAR(255)");
            // Check and add 'notes' column
            jdbcTemplate.execute("IF COL_LENGTH('orders', 'notes') IS NULL ALTER TABLE orders ADD notes VARCHAR(1000)");
            // Check and add 'payment_method' column
            jdbcTemplate.execute("IF COL_LENGTH('orders', 'payment_method') IS NULL ALTER TABLE orders ADD payment_method VARCHAR(255)");
            
            // Phase 5: Product Promo fields
            jdbcTemplate.execute("IF COL_LENGTH('products', 'promo_price') IS NULL ALTER TABLE products ADD promo_price FLOAT");
            jdbcTemplate.execute("IF COL_LENGTH('products', 'promo_quantity') IS NULL ALTER TABLE products ADD promo_quantity INT");

            // Phase 7: Reward Points
            jdbcTemplate.execute("IF COL_LENGTH('users', 'reward_points') IS NULL ALTER TABLE users ADD reward_points INT DEFAULT 0");

            System.out.println("=================================================");
            System.out.println("DATABASE SCHEMA VALIDATED AND UPDATED SUCCESSFULLY");
            System.out.println("=================================================");
        } catch (Exception e) {
            System.out.println("Could not auto-update orders table: " + e.getMessage());
        }
    }
}
