package io.github.giovannilamarmora.accesssphere.settings.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JPA AttributeConverter per la serializzazione e deserializzazione trasparente
 * di mappe categorizzate (Map<String, Object>) in colonne di tipo testo/JSON sul Database.
 * Permette di memorizzare a DB impostazioni raggruppate come singola colonna mappa (es. LOGIN_SETTINGS,
 * BRANDING_SETTINGS, POLICY_SETTINGS) migliorando notevolmente la leggibilità delle colonne a DB.
 */
@Converter
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {

  private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE_REF =
      new TypeReference<>() {};

  @Override
  public String convertToDatabaseColumn(Map<String, Object> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return null;
    }
    return Mapper.writeObjectToString(attribute);
  }

  @Override
  public Map<String, Object> convertToEntityAttribute(String dbData) {
    if (ObjectToolkit.isNullOrEmpty(dbData)) {
      return new LinkedHashMap<>();
    }
    try {
      LinkedHashMap<String, Object> map = Mapper.readObject(dbData, MAP_TYPE_REF);
      return map != null ? map : new LinkedHashMap<>();
    } catch (Exception e) {
      return new LinkedHashMap<>();
    }
  }
}
