package io.github.giovannilamarmora.accesssphere.roles.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JPA AttributeConverter per la serializzazione e deserializzazione trasparente
 * della lista dei permessi (List&lt;String&gt;) in una colonna di tipo testo/JSON (PERMISSIONS)
 * direttamente nella tabella ROLE_DEFINITIONS, evitando una tabella collezionale separata.
 */
@Converter
public class JsonStringListConverter implements AttributeConverter<List<String>, String> {

  private static final TypeReference<List<String>> LIST_TYPE_REF = new TypeReference<>() {};

  @Override
  public String convertToDatabaseColumn(List<String> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return "[]";
    }
    return Mapper.writeObjectToString(attribute);
  }

  @Override
  public List<String> convertToEntityAttribute(String dbData) {
    if (ObjectToolkit.isNullOrEmpty(dbData)) {
      return new ArrayList<>();
    }
    try {
      if (dbData.trim().startsWith("[")) {
        List<String> list = Mapper.readObject(dbData, LIST_TYPE_REF);
        return list != null ? list : new ArrayList<>();
      } else {
        // Supporto a stringhe delimitate da virgola o spazi
        return Arrays.stream(dbData.split("[,\\s]+"))
            .filter(s -> !s.isBlank())
            .toList();
      }
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }
}
