package br.com.digitalstore.api.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OptionTypeConverter implements AttributeConverter<OptionType, String> {

    @Override
    public String convertToDatabaseColumn(OptionType attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public OptionType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : OptionType.fromValue(dbData);
    }
}
