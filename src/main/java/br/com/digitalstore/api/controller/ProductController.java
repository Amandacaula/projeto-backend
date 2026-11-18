package br.com.digitalstore.api.controller;

import br.com.digitalstore.api.controller.docs.ProductControllerDocs;
import br.com.digitalstore.api.dto.common.PageResponse;
import br.com.digitalstore.api.dto.product.ProductRequest;
import br.com.digitalstore.api.dto.product.ProductResponse;
import br.com.digitalstore.api.repository.spec.ProductSearchCriteria;
import br.com.digitalstore.api.service.ProductService;
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
@RequestMapping("/v1/product")
public class ProductController implements ProductControllerDocs {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Override
    @GetMapping("/search")
    public PageResponse<Map<String, Object>> search(
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "fields", required = false) String fields,
            @RequestParam(name = "match", required = false) String match,
            @RequestParam(name = "category_ids", required = false) String categoryIds,
            @RequestParam(name = "price-range", required = false) String priceRange,
            @RequestParam Map<String, String> allParams) {
        ProductSearchCriteria criteria = ProductSearchCriteria.parse(match, categoryIds, priceRange, allParams);
        return productService.search(limit, page, fields, criteria);
    }

    @Override
    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable("id") Integer id) {
        return productService.findById(id);
    }

    @Override
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        return ResponseEntity.created(URI.create("/v1/product/" + created.id())).body(created);
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Integer id, @Valid @RequestBody ProductRequest request) {
        productService.update(id, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
