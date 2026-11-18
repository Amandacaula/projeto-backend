package br.com.digitalstore.api.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OptionShapeConverter implements AttributeConverter<OptionShape, String> {

    @Override
    public String convertToDatabaseColumn(OptionShape attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public OptionShape convertToEntityAttribute(String dbData) {
        return dbData == null ? null : OptionShape.fromValue(dbData);
    }
}
