## Support for Swagger (OpenAPI) documentation

The end-users of our service were surprised there was no documentation on how to use the service.

Now you could react in a defensive (or offended manner) here, but in an Agile world it's only now
they've stated they want to pay for it. So this functionality must have some value for them, now we can crack on
and do it. If we'd have developed it before - maybe that time and effort would have been wasted.

After all; the requirement started out as a single URL context of `/status/{userIdentifier}`, hardly
needs any documentation. But now we've added the `/email/{emailAddress}` URL context maybe some
documentation is a good idea.

### Adding in Swagger 3 OpenAPI support

Now the first question you now need to answer is are you a 'bottom up' or a 'top down' developer.

As I'm a 'code monkey' - I'm a bottom up developer. I'll define all my documentation via code
and have it generated as documentation using `org.springdoc:springdoc-openapi-ui`.

#### The test
Firstly we need to ensure we have a test to access the url for the documents.
```
@SpringBootTest
@AutoConfigureMockMvc
class Boot2OpenAPITests {

  @Test
  void testGetOpenAPIDocumentationPage(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/api-docs")).andExpect(status().isOk());
  }

  @Test
  void testGetOpenAPIDocumentationYAMLPage(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/api-docs.yaml")).andExpect(status().isOk());
  }

  @Test
  void testGetOpenAPIEndUserDocumentation(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/api-ui.html"))
        .andExpect(status().is(302))
        .andExpect(redirectedUrl("/swagger-ui/index.html"));
  }
}
```

I plan to map the specific URLS that come out of the box to my own end points (so that it is
consistent with other projects).

So the above tests currently fail - because I've not added configuration yet.

#### The basic changes
First lets add in some gradle configuration (`springdoc-openapi-ui`) to `build.gradle`.
```
...
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springdoc:springdoc-openapi-ui:1.6.12'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
...
```
Now lets map those bespoke URLS in application.properties:
```
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/api-ui.html
springdoc.swagger-ui.operationsSorter=method
```

We can run our tests again they should now pass! They do.

#### Manual checking

You can now use your browser and go to `http://localhost:8080/api-ui.html` and try those
methods out.

Also, if you access `http://localhost:8080/api-docs.yaml` you'll get the following:
```
openapi: 3.0.1
info:
  title: OpenAPI definition
  version: v0
servers:
- url: http://localhost:8080
  description: Generated server url
paths:
  /status/{userIdentifier}:
    get:
      tags:
      - basic-process-controller
      operationId: checkInputValueStatus
      parameters:
      - name: userIdentifier
        in: path
        required: true
        schema:
          maxLength: 30
          minLength: 2
          type: string
      responses:
        "200":
          description: OK
          content:
            '*/*':
              schema:
                $ref: '#/components/schemas/Status'
  /email/{emailAddress}:
    get:
      tags:
      - email-validation-controller
      operationId: checkEmailAddress
      parameters:
      - name: emailAddress
        in: path
        required: true
        schema:
          type: string
      responses:
        "200":
          description: OK
          content:
            '*/*':
              schema:
                $ref: '#/components/schemas/Status'
components:
  schemas:
    Status:
      type: object
      properties:
        acceptable:
          type: boolean
        reasonUnacceptable:
          type: string
```

So for very little work, we've met the basic needs of the requirement!

Check it in, job done?

### Is there more we could do?
Well yes, we can improve on the documentation a bit. But first things first, get that code in
and check it actually runs up in a deployed environment (like K8S).

So lets bump our, Application version in [build.gradle](build.gradle) and [Chart.yaml](src/main/helm/boot2/Chart.yaml).
Do a local build and deploy via helm and make sure those URLS all still work OK (manually).

```
helm list
# The output
NAME            NAMESPACE       REVISION        UPDATED                                 STATUS          CHART           APP VERSION
boot2           default         1               2022-10-31 14:58:09.672714 +0000 UTC    deployed        boot2-1.0.1     1.0.4      
my-release      default         1               2022-06-30 19:27:51.07704 +0100 BST     deployed        kafka-18.0.0    3.2.0

./gradlew jib; helm upgrade boot2 ./src/main/helm/boot2

helm list                                                  
NAME            NAMESPACE       REVISION        UPDATED                                 STATUS          CHART           APP VERSION
boot2           default         2               2022-11-01 10:23:26.665446 +0000 UTC    deployed        boot2-1.0.1     1.0.5      
my-release      default         1               2022-06-30 19:27:51.07704 +0100 BST     deployed        kafka-18.0.0    3.2.0

```

Now we can check on the Kubernetes service IP/URLS.
```
curl http://192.168.64.95/api-docs

# The output
{"openapi":"3.0.1","info":{"title":"OpenAPI definition","version":"v0"},"servers":[{"url":"http://192.168.64.95","description":"Generated server url"}],"paths":{"/status/{userIdentifier}":{"get":{"tags":["basic-process-controller"],"operationId":"checkInputValueStatus","parameters":[{"name":"userIdentifier","in":"path","required":true,"schema":{"maxLength":30,"minLength":2,"type":"string"}}],"responses":{"200":{"description":"OK","content":{"*/*":{"schema":{"$ref":"#/components/schemas/Status"}}}}}}},"/email/{emailAddress}":{"get":{"tags":["email-validation-controller"],"operationId":"checkEmailAddress","parameters":[{"name":"emailAddress","in":"path","required":true,"schema":{"type":"string"}}],"responses":{"200":{"description":"OK","content":{"*/*":{"schema":{"$ref":"#/components/schemas/Status"}}}}}}}},"components":{"schemas":{"Status":{"type":"object","properties":{"acceptable":{"type":"boolean"},"reasonUnacceptable":{"type":"string"}}}}}}

curl http://192.168.64.95/api-docs

# The output
openapi: 3.0.1
info:
  title: OpenAPI definition
  version: v0
servers:
- url: http://192.168.64.95
  description: Generated server url
paths:
  /status/{userIdentifier}:
    get:
      tags:
      - basic-process-controller
      operationId: checkInputValueStatus
      parameters:
      - name: userIdentifier
        in: path
        required: true
        schema:
          maxLength: 30
          minLength: 2
          type: string
      responses:
        "200":
          description: OK
          content:
            '*/*':
              schema:
                $ref: '#/components/schemas/Status'
  /email/{emailAddress}:
    get:
      tags:
      - email-validation-controller
      operationId: checkEmailAddress
      parameters:
      - name: emailAddress
        in: path
        required: true
        schema:
          type: string
      responses:
        "200":
          description: OK
          content:
            '*/*':
              schema:
                $ref: '#/components/schemas/Status'
components:
  schemas:
    Status:
      type: object
      properties:
        acceptable:
          type: boolean
        reasonUnacceptable:
          type: string
```

The using a browser, you can go to `http://192.168.64.95/api-ui.html` and interact with the side.

So we're good to 'check-in' our code and next we can refine the documentation - if the team things this is
worthwhile.

How long this take upto this point? About an hour - including making tea and writing these pages.

