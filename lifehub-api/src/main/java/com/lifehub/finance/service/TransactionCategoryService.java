package com.lifehub.finance.service;

import com.lifehub.common.exception.ConflictException;
import com.lifehub.common.exception.ResourceNotFoundException;
import com.lifehub.finance.dto.request.CategoryCreateRequest;
import com.lifehub.finance.dto.request.CategoryUpdateRequest;
import com.lifehub.finance.dto.response.CategoryResponse;
import com.lifehub.finance.entity.TransactionCategory;
import com.lifehub.finance.entity.TransactionType;
import com.lifehub.finance.repository.TransactionCategoryRepository;
import com.lifehub.finance.repository.TransactionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TransactionCategoryService {

    private final TransactionCategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public TransactionCategoryService(TransactionCategoryRepository categoryRepository,
                                       TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public CategoryResponse create(Long userId, CategoryCreateRequest request) {
        boolean isDefault = Boolean.TRUE.equals(request.isDefault());
        TransactionCategory category = new TransactionCategory(
                userId, request.name(), request.type(), request.colorHex(), isDefault);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    public List<CategoryResponse> list(Long userId, TransactionType type) {
        List<TransactionCategory> categories = type == null
                ? categoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                : categoryRepository.findAllByUserIdAndTypeOrderByCreatedAtDesc(userId, type);
        return categories.stream().map(CategoryResponse::from).toList();
    }

    public CategoryResponse get(Long userId, Long categoryId) {
        return CategoryResponse.from(findOwnedOrThrow(userId, categoryId));
    }

    @Transactional
    public CategoryResponse update(Long userId, Long categoryId, CategoryUpdateRequest request) {
        TransactionCategory category = findOwnedOrThrow(userId, categoryId);
        boolean isDefault = Boolean.TRUE.equals(request.isDefault());
        category.update(request.name(), request.colorHex(), isDefault);
        // Force the pending UPDATE (and its @PreUpdate-driven updatedAt refresh) to run now,
        // so the response reflects it instead of the pre-flush in-memory value.
        categoryRepository.flush();
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long userId, Long categoryId) {
        TransactionCategory category = findOwnedOrThrow(userId, categoryId);
        if (transactionRepository.existsByCategoryId(categoryId)) {
            throw new ConflictException("이 카테고리를 참조하는 거래 내역이 있어 삭제할 수 없습니다.");
        }
        categoryRepository.delete(category);
    }

    private TransactionCategory findOwnedOrThrow(Long userId, Long categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }
}
