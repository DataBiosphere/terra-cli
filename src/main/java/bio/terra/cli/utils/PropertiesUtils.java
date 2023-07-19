package bio.terra.cli.utils;

import bio.terra.workspace.model.Properties;
import bio.terra.workspace.model.Property;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class PropertiesUtils {

  public static Map<String, String> propertiesToStringMap(@Nullable Properties properties) {
    if (properties == null) {
      return new HashMap<>();
    }
    return properties.stream().collect(Collectors.toMap(Property::getKey, Property::getValue));
  }

  public static Properties stringMapToProperties(@Nullable Map<String, String> map) {
    Properties properties = new Properties();
    if (map == null) {
      return properties;
    }
    for (Map.Entry<String, String> entry : map.entrySet()) {
      Property property = new Property().key(entry.getKey()).value(entry.getValue());
      properties.add(property);
    }
    return properties;
  }
}
