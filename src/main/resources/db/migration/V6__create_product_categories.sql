CREATE TABLE product_categories (
    product_id  INTEGER NOT NULL REFERENCES products (id)   ON DELETE CASCADE,
    category_id INTEGER NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, category_id)
);

-- A PK já indexa (product_id, category_id); este índice atende buscas por categoria
CREATE INDEX idx_product_categories_category_id ON product_categories (category_id);
