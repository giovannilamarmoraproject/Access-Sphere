package io.github.giovannilamarmora.accesssphere.utilities;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

public class LoggerFilter implements Logger {

  private final Logger logger;

  public static Logger getLogger(Class<?> clazz) {
    return new LoggerFilter(clazz);
  }

  public LoggerFilter(Class<?> clazz) {
    this.logger = LoggerFactory.getLogger(clazz);
  }

  private Object[] filterSensitiveFields(Object... objects) {
    List<Object> clonedObjects = new ArrayList<>();
    for (Object object : objects) {
      if (object != null && !isJavaBaseClass(object.getClass())) {
        clonedObjects.add(cloneAndFilterFields(object));
      } else {
        clonedObjects.add(object); // Non filtrare oggetti del modulo java.base
      }
    }
    return clonedObjects.toArray();
  }

  private boolean isJavaBaseClass(Class<?> clazz) {
    return clazz.getModule().getName().equals("java.base");
  }

  private Object cloneAndFilterFields(Object originalObject) {
    try {
      // Effettua la clonazione profonda dell'oggetto
      Class<?> clazz = originalObject.getClass();
      Constructor<?> constructor = clazz.getDeclaredConstructor();
      constructor.setAccessible(true);
      Object clonedObject = constructor.newInstance();
      for (Field field : originalObject.getClass().getDeclaredFields()) {
        field.setAccessible(true);
        Object fieldValue = field.get(originalObject);
        if (fieldValue instanceof String) {
          String fieldName = field.getName();
          if (isSensitiveField(fieldName)) {
            fieldValue = "********"; // Maschera il valore del campo sensibile
          }
        }
        field.set(clonedObject, fieldValue);
      }
      return clonedObject;
    } catch (Exception e) {
      logger.error(
          "An error occurred during filtering sensitive information in the logger: {}",
          e.getMessage());
      return originalObject;
    }
  }

  private boolean isSensitiveField(String fieldName) {
    // Verifica se il nome del campo è sensibile
    return fieldName.matches("(?i)" + "(password|bearer|basic)");
  }

  @Override
  public String getName() {
    return logger.getName();
  }

  @Override
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  @Override
  public void info(String s) {
    logger.info(s);
  }

  @Override
  public void info(String s, Object o) {
    logger.info(s, filterSensitiveFields(o));
  }

  @Override
  public void info(String s, Object o, Object o1) {
    logger.info(s, filterSensitiveFields(o), filterSensitiveFields(o1));
  }

  @Override
  public void info(String s, Object... objects) {
    logger.info(s, filterSensitiveFields(objects));
  }

  @Override
  public void info(String s, Throwable throwable) {
    logger.info(s, throwable);
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return logger.isInfoEnabled(marker);
  }

  @Override
  public void info(Marker marker, String s) {
    logger.info(marker, s);
  }

  @Override
  public void info(Marker marker, String s, Object o) {
    logger.info(marker, s, filterSensitiveFields(o));
  }

  @Override
  public void info(Marker marker, String s, Object o, Object o1) {
    logger.info(marker, s, filterSensitiveFields(o), filterSensitiveFields(o1));
  }

  @Override
  public void info(Marker marker, String s, Object... objects) {
    logger.info(marker, s, filterSensitiveFields(objects));
  }

  @Override
  public void info(Marker marker, String s, Throwable throwable) {
    logger.info(marker, s, throwable);
  }

  @Override
  public boolean isWarnEnabled() {
    return false;
  }

  @Override
  public void trace(String s) {
    logger.trace(s);
  }

  @Override
  public void trace(String s, Object o) {
    logger.trace(s, filterSensitiveFields(o));
  }

  @Override
  public void trace(String s, Object o, Object o1) {
    logger.trace(s, filterSensitiveFields(o), filterSensitiveFields(o1));
  }

  @Override
  public void trace(String s, Object... objects) {
    logger.trace(s, filterSensitiveFields(objects));
  }

  @Override
  public void trace(String s, Throwable throwable) {
    logger.trace(s, throwable);
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return logger.isTraceEnabled(marker);
  }

  @Override
  public void trace(Marker marker, String s) {
    logger.trace(marker, s);
  }

  @Override
  public void trace(Marker marker, String s, Object o) {
    logger.trace(marker, s, filterSensitiveFields(o));
  }

  @Override
  public void trace(Marker marker, String s, Object o, Object o1) {
    logger.trace(marker, s, filterSensitiveFields(o), filterSensitiveFields(o1));
  }

  @Override
  public void trace(Marker marker, String s, Object... objects) {
    logger.trace(marker, s, filterSensitiveFields(objects));
  }

  @Override
  public void trace(Marker marker, String s, Throwable throwable) {
    logger.trace(marker, s, throwable);
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  @Override
  public void debug(String s) {
    logger.debug(s);
  }

  @Override
  public void debug(String s, Object o) {
    logger.debug(s, filterSensitiveFields(o));
  }

  @Override
  public void debug(String s, Object o, Object o1) {
    logger.debug(s, filterSensitiveFields(o), filterSensitiveFields(o1));
  }

  @Override
  public void debug(String s, Object... objects) {
    logger.debug(s, filterSensitiveFields(objects));
  }

  @Override
  public void debug(String s, Throwable throwable) {
    logger.debug(s, throwable);
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return logger.isDebugEnabled(marker);
  }

  @Override
  public void debug(Marker marker, String s) {
    logger.debug(marker, s);
  }

  @Override
  public void debug(Marker marker, String s, Object o) {
    logger.debug(marker, s, filterSensitiveFields(o));
  }

  @Override
  public void debug(Marker marker, String s, Object o, Object o1) {
    logger.debug(marker, s, filterSensitiveFields(o), filterSensitiveFields(o1));
  }

  @Override
  public void debug(Marker marker, String s, Object... objects) {
    logger.debug(marker, s, filterSensitiveFields(objects));
  }

  @Override
  public void debug(Marker marker, String s, Throwable throwable) {
    logger.debug(marker, s, throwable);
  }

  @Override
  public boolean isInfoEnabled() {
    return false;
  }

  @Override
  public void error(String s) {
    logger.error(s);
  }

  @Override
  public void error(String s, Object o) {
    logger.error(s, filterSensitiveFields(o));
  }

  @Override
  public void error(String s, Object o, Object o1) {
    logger.error(s, filterSensitiveFields(o), filterSensitiveFields(o1));
  }

  @Override
  public void error(String s, Object... objects) {
    logger.error(s, filterSensitiveFields(objects));
  }

  @Override
  public void error(String s, Throwable throwable) {
    logger.error(s, throwable);
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return logger.isErrorEnabled(marker);
  }

  @Override
  public void error(Marker marker, String s) {
    logger.error(marker, s);
  }

  @Override
  public void error(Marker marker, String s, Object o) {
    logger.error(marker, s, filterSensitiveFields(o));
  }

  @Override
  public void error(Marker marker, String s, Object o, Object o1) {
    logger.error(marker, s, filterSensitiveFields(o), filterSensitiveFields(o1));
  }

  @Override
  public void error(Marker marker, String s, Object... objects) {
    logger.error(marker, s, filterSensitiveFields(objects));
  }

  @Override
  public void error(Marker marker, String s, Throwable throwable) {
    logger.error(marker, s, throwable);
  }

  @Override
  public void warn(String s) {
    logger.warn(s);
  }

  @Override
  public void warn(String s, Object o) {
    logger.warn(s, filterSensitiveFields(o));
  }

  @Override
  public void warn(String s, Object o, Object o1) {
    logger.warn(s, filterSensitiveFields(o), filterSensitiveFields(o1));
  }

  @Override
  public void warn(String s, Object... objects) {
    logger.warn(s, filterSensitiveFields(objects));
  }

  @Override
  public void warn(String s, Throwable throwable) {
    logger.warn(s, throwable);
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return logger.isWarnEnabled(marker);
  }

  @Override
  public void warn(Marker marker, String s) {
    logger.warn(marker, s);
  }

  @Override
  public void warn(Marker marker, String s, Object o) {
    logger.warn(marker, s, filterSensitiveFields(o));
  }

  @Override
  public void warn(Marker marker, String s, Object o, Object o1) {
    logger.warn(marker, s, filterSensitiveFields(o), filterSensitiveFields(o1));
  }

  @Override
  public void warn(Marker marker, String s, Object... objects) {
    logger.warn(marker, s, filterSensitiveFields(objects));
  }

  @Override
  public void warn(Marker marker, String s, Throwable throwable) {
    logger.warn(marker, s, throwable);
  }

  @Override
  public boolean isErrorEnabled() {
    return logger.isErrorEnabled();
  }
}
