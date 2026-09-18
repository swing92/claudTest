package com.lifehub.finance.controller;

import com.lifehub.common.security.CurrentUserId;
import com.lifehub.finance.dto.request.CategoryCreateRequest;
import com.lifehub.finance.dto.request.CategoryUpdateRequest;
import com.lifehub.finance.dto.response.CategoryResponse;
import com.lifehub.finance.entity.TransactionType;
import com.lifehub.finance.service.TransactionCategoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/categories")
public class TransactionCategoryController {

    private final TransactionCategoryService categoryService;

    public TransactionCategoryController(TransactionCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@CurrentUserId Long userId,
                                                     @Valid @RequestBody CategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(userId, request));
    }

    @GetMapping
    public List<CategoryResponse> list(@CurrentUserId Long userId,
                                        @RequestParam(required = false) TransactionType type) {
        return categoryService.list(userId, type);
    }

    @GetMapping("/{categoryId}")
    public CategoryResponse get(@CurrentUserId Long userId, @PathVariable Long categoryId) {
        return categoryService.get(userId, categoryId);
    }

    @PutMapping("/{categoryId}")
    public CategoryResponse update(@CurrentUserId Long userId, @PathVariable Long categoryId,
                                    @Valid @RequestBody CategoryUpdateRequest request) {
        return categoryService.update(userId, categoryId, request);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(@CurrentUserId Long userId, @PathVariable Long categoryId) {
        categoryService.delete(userId, categoryId);
        return ResponseEntity.noContent().build();
    }
}
