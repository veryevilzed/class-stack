class-stack
===========

Class Object mapper


maven:

```
<dependency>
            <groupId>ru.veryevilzed.tools</groupId>
            <artifactId>class-stack</artifactId>
            <version>1.0-SNAPSHOT</version>
            <scope>compile</scope>
</dependency>
```


usage:

```java

package com.example;

public class RequestTestClass {

    public class EntityTest {
        public int id;
        public String name;

        public EntityTest(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public EntityTest entityA;
    public EntityTest entityB;

    public RequestTestClass() {
        entityA = new EntityTest(1, "Test User A");
        entityB = null;
    }
}

public class ContextTestClass {

    public int iteration = 0;
    public RequestTestClass.EntityTest lastEntity;

}

@SmartClassService(incoming = "com.example.RequestTestClass", 
                   context = "com.example.ContextTestClass")
@Service
public class TestService {

    @SmartClassMethod("entityA")
    public void EntityA(RequestTestClass.EntityTest entity, 
                        ContextTestClass context) {
        log.info("EntA");
        context.iteration++;
    }


    @SmartClassMethod("entityB")
    public void EntityB(RequestTestClass.EntityTest entity, 
                        ContextTestClass context) {
        log.info("EntB");
        context.iteration++;
    }
}


@SpringBootApplication
@ComponentScan(basePackages = {"com.example", "ru.veryevilzed.tools"})
public class Application implements CommandLineRunner {

    public static void main(final String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }

    @Autowired
    ClassService classService;

    @Override
    public void run(String... strings) throws Exception {
        ContextTestClass resp = 
            (ContextTestClass)classService.execute(new RequestTestClass(), 
                                                   new ContextTestClass());
        System.out.println("Iterations:" + resp.getIteration());
    }
}

```
