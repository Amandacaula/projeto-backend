package br.com.digitalstore.api.service;

import br.com.digitalstore.api.dto.category.CategoryRequest;
import br.com.digitalstore.api.dto.category.CategoryResponse;
import br.com.digitalstore.api.dto.common.PageResponse;
import br.com.digitalstore.api.exception.BadRequestException;
import br.com.digitalstore.api.exception.NotFoundException;
import br.com.digitalstore.api.model.Category;
import br.com.digitalstore.api.repository.CategoryRepository;
import br.com.digitalstore.api.util.FieldSelector;
import br.com.digitalstore.api.util.SearchParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Service
public class CategoryService {

    static final String SLUG_TAKEN = "Slug já cadastrado";

    private final CategoryRepository categoryRepository;
    private final FieldSelector fieldSelector;

    public CategoryService(CategoryRepository categoryRepository, FieldSelector fieldSelector) {
        this.categoryRepository = categoryRepository;
        this.fieldSelector = fieldSelector;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> search(Integer limit, Integer page, String fields, Boolean useInMenu) {
        SearchParams params = SearchParams.of(limit, page);
        Set<String> selectedFields = fieldSelector.parse(fields, CategoryResponse.FIELDS);
        Pageable pageable = params.toPageable(Sort.by("id").ascending());

        Page<Category> result = useInMenu == null
                ? categoryRepository.findAll(pageable)
                : categoryRepository.findByUseInMenu(useInMenu, pageable);

        var data = result.getContent().stream().map(CategoryResponse::from).toList();
        return new PageResponse<>(fieldSelector.select(data, selectedFields),
                result.getTotalElements(), params.limit(), params.page());
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Integer id) {
        return CategoryResponse.from(getOrThrow(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String slug = request.slug().trim();
        if (categoryRepository.existsBySlug(slug)) {
            throw BadRequestException.forField("slug", SLUG_TAKEN);
        }
        Category category = new Category();
        apply(category, request, slug);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void update(Integer id, CategoryRequest request) {
        Category category = getOrThrow(id);
        String slug = request.slug().trim();
        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw BadRequestException.forField("slug", SLUG_TAKEN);
        }
        apply(category, request, slug);
    }

    @Transactional
    public void delete(Integer id) {
        categoryRepository.delete(getOrThrow(id));
    }

    private void apply(Category category, CategoryRequest request, String slug) {
        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setUseInMenu(Boolean.TRUE.equals(request.useInMenu()));
    }

    private Category getOrThrow(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoria " + id + " não encontrada"));
    }
}
