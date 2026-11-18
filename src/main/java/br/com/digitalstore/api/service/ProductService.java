package br.com.digitalstore.api.service;

import br.com.digitalstore.api.dto.common.PageResponse;
import br.com.digitalstore.api.dto.product.ImageRequest;
import br.com.digitalstore.api.dto.product.OptionRequest;
import br.com.digitalstore.api.dto.product.ProductRequest;
import br.com.digitalstore.api.dto.product.ProductResponse;
import br.com.digitalstore.api.exception.BadRequestException;
import br.com.digitalstore.api.exception.NotFoundException;
import br.com.digitalstore.api.model.Category;
import br.com.digitalstore.api.model.Product;
import br.com.digitalstore.api.model.ProductImage;
import br.com.digitalstore.api.model.ProductOption;
import br.com.digitalstore.api.repository.CategoryRepository;
import br.com.digitalstore.api.repository.ProductRepository;
import br.com.digitalstore.api.repository.spec.ProductSearchCriteria;
import br.com.digitalstore.api.repository.spec.ProductSpecifications;
import br.com.digitalstore.api.util.FieldSelector;
import br.com.digitalstore.api.util.RadiusParser;
import br.com.digitalstore.api.util.SearchParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductService {

    static final String SLUG_TAKEN = "Slug já cadastrado";
    private static final int VALUES_MAX_LENGTH = 255;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ImageStorageService imageStorage;
    private final FieldSelector fieldSelector;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          ImageStorageService imageStorage, FieldSelector fieldSelector) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.imageStorage = imageStorage;
        this.fieldSelector = fieldSelector;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> search(Integer limit, Integer page, String fields,
                                                    ProductSearchCriteria criteria) {
        SearchParams params = SearchParams.of(limit, page);
        Set<String> selectedFields = fieldSelector.parse(fields, ProductResponse.FIELDS);

        Page<Product> result = productRepository.findAll(ProductSpecifications.from(criteria),
                params.toPageable(Sort.by("id").ascending()));

        List<ProductResponse> data = result.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(fieldSelector.select(data, selectedFields),
                result.getTotalElements(), params.limit(), params.page());
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Integer id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        validatePrices(request);
        String slug = request.slug().trim();
        if (productRepository.existsBySlug(slug)) {
            throw BadRequestException.forField("slug", SLUG_TAKEN);
        }

        Product product = new Product();
        applyFields(product, request, slug);
        product.setEnabled(Boolean.TRUE.equals(request.enabled()));
        product.setUseInMenu(Boolean.TRUE.equals(request.useInMenu()));
        product.setStock(request.stock() == null ? 0 : request.stock());
        if (request.categoryIds() != null) {
            product.setCategories(resolveCategories(request.categoryIds()));
        }
        product = productRepository.save(product);

        List<ImageRequest> images = request.images() == null ? List.of() : request.images();
        for (int i = 0; i < images.size(); i++) {
            ImageRequest image = images.get(i);
            String field = "images[" + i + "]";
            if (image.id() != null || image.deleted() != null) {
                throw BadRequestException.forField(field, "id e deleted só podem ser usados na atualização");
            }
            addNewImage(product, image, field);
        }

        List<OptionRequest> options = request.options() == null ? List.of() : request.options();
        for (int i = 0; i < options.size(); i++) {
            OptionRequest option = options.get(i);
            String field = "options[" + i + "]";
            if (option.id() != null || option.deleted() != null) {
                throw BadRequestException.forField(field, "id e deleted só podem ser usados na atualização");
            }
            addNewOption(product, option, field);
        }

        return toResponse(productRepository.saveAndFlush(product));
    }

    @Transactional
    public void update(Integer id, ProductRequest request) {
        Product product = getOrThrow(id);
        validatePrices(request);
        String slug = request.slug().trim();
        if (productRepository.existsBySlugAndIdNot(slug, id)) {
            throw BadRequestException.forField("slug", SLUG_TAKEN);
        }

        applyFields(product, request, slug);
        if (request.enabled() != null) {
            product.setEnabled(request.enabled());
        }
        if (request.useInMenu() != null) {
            product.setUseInMenu(request.useInMenu());
        }
        if (request.stock() != null) {
            product.setStock(request.stock());
        }
        if (request.categoryIds() != null) {
            product.setCategories(resolveCategories(request.categoryIds()));
        }
        if (request.images() != null) {
            applyImageOperations(product, request.images());
        }
        if (request.options() != null) {
            applyOptionOperations(product, request.options());
        }
    }

    private void applyImageOperations(Product product, List<ImageRequest> operations) {
        Map<Integer, ProductImage> existing = indexById(product.getImages(), ProductImage::getId);

        for (int i = 0; i < operations.size(); i++) {
            ImageRequest op = operations.get(i);
            String field = "images[" + i + "]";

            if (op.id() == null) {
                if (Boolean.TRUE.equals(op.deleted())) {
                    throw BadRequestException.forField(field + ".id", "Informe o id da imagem a remover");
                }
                addNewImage(product, op, field);
                continue;
            }

            ProductImage image = existing.get(op.id());
            if (image == null) {
                throw BadRequestException.forField(field + ".id",
                        "Imagem " + op.id() + " não pertence ao produto " + product.getId());
            }
            if (Boolean.TRUE.equals(op.deleted())) {
                product.getImages().remove(image);
                existing.remove(op.id());
                imageStorage.deleteAfterCommit(image.getPath());
            } else if (op.content() != null && !isUrl(op.content())) {
                String oldPath = image.getPath();
                image.setPath(imageStorage.save(product.getId(), op.type(), op.content(), field));
                imageStorage.deleteAfterCommit(oldPath);
            }
        }
    }

    private void applyOptionOperations(Product product, List<OptionRequest> operations) {
        Map<Integer, ProductOption> existing = indexById(product.getOptions(), ProductOption::getId);

        for (int i = 0; i < operations.size(); i++) {
            OptionRequest op = operations.get(i);
            String field = "options[" + i + "]";

            if (op.id() == null) {
                if (Boolean.TRUE.equals(op.deleted())) {
                    throw BadRequestException.forField(field + ".id", "Informe o id da opção a remover");
                }
                addNewOption(product, op, field);
                continue;
            }

            ProductOption option = existing.get(op.id());
            if (option == null) {
                throw BadRequestException.forField(field + ".id",
                        "Opção " + op.id() + " não pertence ao produto " + product.getId());
            }
            if (Boolean.TRUE.equals(op.deleted())) {
                product.getOptions().remove(option);
                existing.remove(op.id());
            } else {
                applyOption(option, op, field, false);
            }
        }
    }

    @Transactional
    public void delete(Integer id) {
        Product product = getOrThrow(id);
        productRepository.delete(product);
        imageStorage.deleteProductFolderAfterCommit(id);
    }

    private Product getOrThrow(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Produto " + id + " não encontrado"));
    }

    private static void validatePrices(ProductRequest request) {
        if (request.priceWithDiscount().compareTo(request.price()) > 0) {
            throw BadRequestException.forField("price_with_discount",
                    "O campo price_with_discount não pode ser maior que price");
        }
    }

    private static void applyFields(Product product, ProductRequest request, String slug) {
        product.setName(request.name().trim());
        product.setSlug(slug);
        product.setPrice(request.price());
        product.setPriceWithDiscount(request.priceWithDiscount());
        if (request.description() != null) {
            product.setDescription(request.description().isBlank() ? null : request.description().trim());
        }
    }

    private Set<Category> resolveCategories(List<Integer> ids) {
        Set<Integer> requested = new HashSet<>(ids);
        List<Category> found = categoryRepository.findAllById(requested);
        if (found.size() != requested.size()) {
            Set<Integer> foundIds = found.stream().map(Category::getId).collect(Collectors.toSet());
            List<Integer> missing = requested.stream().filter(i -> !foundIds.contains(i)).sorted().toList();
            throw BadRequestException.forField("category_ids", "Categorias não encontradas: " + missing);
        }
        return new HashSet<>(found);
    }

    private void addNewImage(Product product, ImageRequest request, String field) {
        if (request.type() == null || request.type().isBlank()) {
            throw BadRequestException.forField(field + ".type", "O campo type é obrigatório para novas imagens");
        }
        ProductImage image = new ProductImage();
        image.setPath(imageStorage.save(product.getId(), request.type(), request.content(), field));
        image.setEnabled(true);
        product.addImage(image);
    }

    private void addNewOption(Product product, OptionRequest request, String field) {
        ProductOption option = new ProductOption();
        applyOption(option, request, field, true);
        product.addOption(option);
    }

    private static void applyOption(ProductOption option, OptionRequest request, String field, boolean isNew) {
        if (isNew || request.title() != null) {
            if (request.title() == null || request.title().isBlank()) {
                throw BadRequestException.forField(field + ".title", "O campo title é obrigatório");
            }
            if (request.title().trim().length() > 100) {
                throw BadRequestException.forField(field + ".title", "O campo title deve ter no máximo 100 caracteres");
            }
            option.setTitle(request.title().trim());
        }
        if (request.shape() != null) {
            option.setShape(request.shape());
        }
        if (request.type() != null) {
            option.setType(request.type());
        }
        if (request.radius() != null) {
            option.setRadius(RadiusParser.parse(request.radius(), field + ".radius"));
        }
        if (isNew || request.values() != null) {
            option.setValues(joinValues(request.values(), field + ".values"));
        }
    }

    private static String joinValues(List<String> values, String field) {
        if (values == null || values.isEmpty()) {
            throw BadRequestException.forField(field, "Informe ao menos um valor");
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw BadRequestException.forField(field, "Os valores não podem ser vazios");
            }
            if (value.contains(",")) {
                throw BadRequestException.forField(field, "Os valores não podem conter vírgula: " + value);
            }
        }
        String joined = values.stream().map(String::trim).collect(Collectors.joining(","));
        if (joined.length() > VALUES_MAX_LENGTH) {
            throw BadRequestException.forField(field, "Os valores somados devem ter no máximo 255 caracteres");
        }
        return joined;
    }

    private static boolean isUrl(String content) {
        String c = content.trim().toLowerCase();
        return c.startsWith("http://") || c.startsWith("https://");
    }

    private static <T> Map<Integer, T> indexById(List<T> items, Function<T, Integer> id) {
        Map<Integer, T> map = new HashMap<>();
        items.forEach(item -> map.put(id.apply(item), item));
        return map;
    }

    private ProductResponse toResponse(Product product) {
        List<Integer> categoryIds = product.getCategories().stream()
                .map(Category::getId).sorted().toList();
        List<ProductResponse.Image> images = product.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::getId))
                .map(img -> new ProductResponse.Image(img.getId(), imageStorage.toUrl(img.getPath())))
                .toList();
        List<ProductResponse.Option> options = product.getOptions().stream()
                .sorted(Comparator.comparing(ProductOption::getId))
                .map(opt -> new ProductResponse.Option(opt.getId(), opt.getTitle(), opt.getShape(),
                        opt.getRadius(), opt.getType(), Arrays.asList(opt.getValues().split(","))))
                .toList();
        return new ProductResponse(product.getId(), product.getEnabled(), product.getName(), product.getSlug(),
                product.getUseInMenu(), product.getStock(), product.getDescription(), product.getPrice(),
                product.getPriceWithDiscount(), categoryIds, images, options);
    }
}
