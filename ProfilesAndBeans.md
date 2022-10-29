## Using Profiles for Conditional Beans

There are times when it is necessary to use different implementations of components/beans based on
different runtime situations.

Now some of those situations can be dealt with by just using different configuration data. For example
connecting to remove production/pre-production or test services with different URLS and credentials.

But in other cases; we actually need to change the code. A typical example of this would be when running
tests. Ideally all our tests should be local and not make remove calls at all. This is so they can both
- Execute quickly
- Provide a range of different responses that we can control.

The above can be implemented via `@ConditionalOnProperty` and then just using a `profile` to ensure the correct
properties are set. So if you have lots of conditionality in your application; you are adding complexity.

Ideally you want as few properties and as few conditional beans as possible. The example below is a very simple solution
to conditionality.

So as a concrete example, lets say we need to connect to a remote service and pass some data in;
but we need to cater for a range of different types of error condition.

When deployed in production or other real runtime environment we'd want to connect to the real live service.
But during unit/integration-test runs we really want to simulate that remote system and a range of different errors.

### Spring Profiles
Normally Spring runs with a `profile` called `default`. But during testing we'd like to alter
that `profile` to something like `"dev"`.

This can be done in maven and gradle. For Gradle we edit the [build.gradle](build.gradle) file as below:
```
...
tasks.named('test') {
    useJUnitPlatform()
    systemProperty "spring.profiles.active", "dev"
}
...
```
Now during tests the system property `spring.profiles.active` is set to `dev`. 

We need to pick up that setting and use it in our application.

### Conditional Beans
We can use the `active spring profile` to conditionally use beans. This is where a Java Interface becomes important.

Define a Java Interface say [ExternalSystem.java](src/main/java/com/example/boot2/ExternalSystem.java) as below:
```
package com.example.boot2;

/**
 * Designed to check how conditional beans can be created in production and test
 */
public interface ExternalSystem {
  String getExternalSystemName();
}
```

This is just a signature/abstraction of the 'contract' our application will have with such a remote system.
We can provide two implementation of this interface. The first is our development test implementation,
the second would be the real production implementation we'd really want to use.

[Test Implementation](src/test/java/com/example/boot2/TestExternalSystem.java)
```
package com.example.boot2;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class TestExternalSystem implements ExternalSystem {
  @Override
  public String getExternalSystemName() {
    return "Test";
  }
}
```

Note that implementation is defined in the `src/test/java` directory of the project and so cannot be shipped
in the final build with the deployable application.

[Production Implementation](src/main/java/com/example/boot2/ProductionExternalSystem.java)
```
package com.example.boot2;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!dev")
public class ProductionExternalSystem implements ExternalSystem{
  @Override
  public String getExternalSystemName() {
    return "Production";
  }
}
```

### So how does it work?

If we take a look at our SpringBoot Application (it still does not really do much!).
```
package com.example.boot2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Boot2Application {

  private final Logger logger = LoggerFactory.getLogger(Boot2Application.class);
  private final ExternalSystem externalSystem;

  public Boot2Application(ExternalSystem externalSystem) {
    this.externalSystem = externalSystem;
    displayExternalSystemInUse();
  }

  private void displayExternalSystemInUse() {
    logger.info("Created with externalSystem {}", externalSystem.getExternalSystemName());
  }

  public static void main(String[] args) {
    SpringApplication.run(Boot2Application.class, args);
  }

}
```

You can see that the `Boot2Application` is constructed with
an instance of `ExternalSystem` (i.e. the interface we defined earlier).
That variable is then held as a `final private property` and is used in method `displayExternalSystemInUse`.

#### So what did Spring do?
Spring looks at the constructor of `Boot2Application` and sees that it needs instance of
`ExternalSystem` and so it looks for a compatible Spring bean.
This is where the profiles come in. If the profile `dev` has been set then `TestExternalSystem` will have
been instantiated and added to the Spring context - so Spring would have resolved that.

If on the other hand; if the profile `dev` had not been set (which in none-test situation here),
then the `ProductionExternalSystem` would have been instantiated and added to the Spring context.

The alternative to using `@Profile` and the 'magic name' of `dev` is to conditionally load properties via the `profile`;
then use `@ConditionalOnProperty`.

Let's say we use the `dev` profile to conditionally load `properties` and specifically defined a property called
`external.system` being `prd` if we wanted a test implementation and `stub` if we wanted the real live service.

Our beans would change as below:
##### Test External System
```
package com.example.boot2;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="external.system", havingValue="stub")
public class TestExternalSystem implements ExternalSystem {
  @Override
  public String getExternalSystemName() {
    return "Test";
  }
}
```

##### Production External System
```
package com.example.boot2;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="external.system", havingValue="prd")
public class ProductionExternalSystem implements ExternalSystem{
  @Override
  public String getExternalSystemName() {
    return "Production";
  }
}
```

But we must ensure that those properties get set. So we update
[application.properties](src/main/resources/application.properties) and add in the following:
```
external.system=prd
logging.level.com.example.boot2=INFO
```

We also need to add in our configuration for test as well.
We create [application-dev.properties](src/test/resources/application-dev.properties).
The name `application-dev` is driven by the fact we chose the name `dev` as the profile. Spring will
automatically pick this up. The content is as follows:
```
external.system=stub
```

## Conclusion
Well in short, lots of options and implicit stuff being picked up (but that's frameworks).

All I can say is:
- Reduce optionality as much as possible - few paths is better
- While we don't have @Profile and 'magic' profile names everywhere, we do have complexity
- Make sure the number an amount of variation and indirection is worth it
- Keep stuff small and as simple as possible
- By using `@ConditionalOnProperty` we have added two 'magic' names, the "property" and the "value"
- Which every option you go for, make sure it is consistent, documented and everyone in the team knows