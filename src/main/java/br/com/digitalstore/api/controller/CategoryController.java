package br.com.digitalstore.api.controller;

import br.com.digitalstore.api.controller.docs.CategoryControllerDocs;
import br.com.digitalstore.api.dto.category.CategoryRequest;
import br.com.digitalstore.api.dto.category.CategoryResponse;
import br.com.digitalstore.api.dto.common.PageResponse;
import br.com.digitalstore.api.service.CategoryService;
import jakarta.validation.Valid;
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

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/v1/category")
public class CategoryController implements CategoryControllerDocs {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Override
    @GetMapping("/search")
    public PageResponse<Map<String, Object>> search(
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "fields", required = false) String fields,
            @RequestParam(name = "use_in_menu", required = false) Boolean useInMenu) {
        return categoryService.search(limit, page, fields, useInMenu);
    }

    @Override
    @GetMapping("/{id}")
    public CategoryResponse findById(@PathVariable("id") Integer id) {
        return categoryService.findById(id);
    }

    @Override
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.create(request);
        return ResponseEntity.created(URI.create("/v1/category/" + created.id())).body(created);
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Integer id, @Valid @RequestBody CategoryRequest request) {
        categoryService.update(id, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
