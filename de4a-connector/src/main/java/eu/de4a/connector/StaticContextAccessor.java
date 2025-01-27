package eu.de4a.connector;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class StaticContextAccessor {

  private static StaticContextAccessor instance;

  @Autowired
  private ApplicationContext applicationContext;

  @PostConstruct
  public void registerInstance() {
    instance = this;
  }

  public static <T> T getBean(Class<T> clazz) {
    if (instance == null)
      throw new IllegalStateException("No instance is yet set");
    if (instance.applicationContext == null)
      throw new IllegalStateException("No applicationContext is yet set");
    return instance.applicationContext.getBean(clazz);
  }
}
