package com.lifehub.jobapplications.controller;

import com.lifehub.common.security.CurrentUserId;
import com.lifehub.jobapplications.dto.request.CompanyCreateRequest;
import com.lifehub.jobapplications.dto.request.CompanyUpdateRequest;
import com.lifehub.jobapplications.dto.response.CompanyResponse;
import com.lifehub.jobapplications.service.CompanyService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    public ResponseEntity<CompanyResponse> create(@CurrentUserId Long userId,
                                                    @Valid @RequestBody CompanyCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.create(userId, request));
    }

    @GetMapping
    public List<CompanyResponse> list(@CurrentUserId Long userId) {
        return companyService.list(userId);
    }

    @GetMapping("/{companyId}")
    public CompanyResponse get(@CurrentUserId Long userId, @PathVariable Long companyId) {
        return companyService.get(userId, companyId);
    }

    @PutMapping("/{companyId}")
    public CompanyResponse update(@CurrentUserId Long userId, @PathVariable Long companyId,
                                   @Valid @RequestBody CompanyUpdateRequest request) {
        return companyService.update(userId, companyId, request);
    }

    @DeleteMapping("/{companyId}")
    public ResponseEntity<Void> delete(@CurrentUserId Long userId, @PathVariable Long companyId) {
        companyService.delete(userId, companyId);
        return ResponseEntity.noContent().build();
    }
}
