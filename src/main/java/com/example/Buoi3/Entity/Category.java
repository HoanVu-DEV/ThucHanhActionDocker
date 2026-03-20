package com.example.Buoi3.Entity;

import jakarta.persistence.*; 
import jakarta.validation.constraints.NotBlank; 
import lombok.*; 
import java.util.ArrayList;
import java.util.List;
 
@Setter 
@Getter 
@RequiredArgsConstructor 
@AllArgsConstructor 
@Entity 
@Table(name = "categories") 
public class Category { 
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id; 
 
    @NotBlank(message = "Tên là bắt buộc") 
    private String name; 

    private String icon;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Category> children = new ArrayList<>();
} 
