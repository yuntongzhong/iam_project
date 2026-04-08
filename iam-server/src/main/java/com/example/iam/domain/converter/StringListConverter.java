package com.example.iam.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        return attribute.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(DELIMITER));
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(dbData.split(DELIMITER))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
