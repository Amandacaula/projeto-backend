package br.com.digitalstore.api.model;

import br.com.digitalstore.api.model.enums.OptionShape;
import br.com.digitalstore.api.model.enums.OptionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_options")
@Getter
@Setter
@NoArgsConstructor
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 10)
    private OptionShape shape = OptionShape.SQUARE;

    @Column(nullable = false)
    private Integer radius = 0;

    @Column(nullable = false, length = 10)
    private OptionType type = OptionType.TEXT;

    @Column(name = "\"values\"", nullable = false)
    private String values;
}
