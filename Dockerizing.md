## Dockerizing this SpringBoot Application

I'd like to run this app in a Kubernetes docker container.

So first thing I'm going to do is use `JIB`, rather than create Dockerfiles;
as `JIB` makes it very easy to package Java SpringBoot apps into a container.

Alter [build.gradle](build.gradle) and add in the following:
```
plugins {
    id 'org.springframework.boot' version '2.7.5'
    id 'com.google.cloud.tools.jib' version '3.3.1'
    id 'io.spring.dependency-management' version '1.0.15.RELEASE'
    id 'java'
}

...
ext {
    //Default of where we will push the resulting jib generated docker image.
    //I run a microk8s repo for this locally.
    targetDockerRepository = "192.168.64.2:32000"
}

...

jib {
    allowInsecureRegistries = true
    from {
        image = 'openjdk:17-jdk-alpine'
    }

    to {
        //Double quotes important here like bash shell, single quotes are literal
        //but double quotes allow variable expansion.
        image = "${targetDockerRepository}/${rootProject.name}:${version}"
    }
}
```

I can now build the docker image using `./gradlew jib`.

As I'm compiling and using Java 17 - I've based the docker image on `alpine 17 jdk`. If you take a look
in one of my other [repos](https://github.com/stephenjohnlimb/boot/blob/master/K8s.md) you'll see I have a local
`microk8s` setup running with `multipass`. It is inside one of those virtual machines where I store my docker images.

I can then check that got pushed to my local docker repository with `curl http://192.168.64.2:32000/v2/boot2/tags/list`

### Helm charts
Now I need to create a helm chart to deploy this application, because I plan to use Kubernetes.

So use the `terminal` window in IntelliJ and in your `boot2` directory issue the following command: `mkdir src/main/helm;  helm create boot2`.

Now you can edit [Chart.yaml](src/main/helm/boot2/Chart.yaml) (mainly just the version numbers you want to use),
and [values.yaml](src/main/helm/boot2/values.yaml).

But make sure that the version number in your `build.gradle` and the `appVersion` you reference in your `Chart.yaml` match up.

The `values.yaml` is a bit more involved:
```
...
image:
  repository: localhost:32000/boot2

...
service:
  type: LoadBalancer
  port: 80
  targetPort: 8080

ingress:
  enabled: true
  ...
```

I need to state where to get the docker image from (this needs to be with respect to the microk8s system).
I also want to use port 80 and route that through to port 8080 on my SpringBoot app. As I have
`metallb` configured I'll expose a point of ingress from my local MacBook into the Kubernetes cluster.
I'll show how the IP address is set and fixed later on.

But we also need to alter the templates and add in `actuator` so that Kubernetes can check the container is alive and also ready.
This is really important and Kubernetes needs to know if the app has started OK and is able to service requests.

I've altered [build.gradle](build.gradle) and added the `actuator`:
```
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

Now I have to alter a few of the `helm` templates as follows:

The [deployment.yaml](src/main/helm/boot2/templates/deployment.yaml) is the first thing to alter:
Specifically this section:
```
...
          ports:
            - name: http
              containerPort: {{ .Values.service.targetPort }}
              protocol: TCP
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: {{ .Values.service.targetPort }}
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: {{ .Values.service.targetPort }}
...              
```

The `actuator` context is added by Spring, but also needs this entry in `application.properties`:
```
management.endpoints.web.exposure.include=health,info
```

The other file is [service.yaml](src/main/helm/boot2/templates/service.yaml).
Specifically this section:
```
...
spec:
  type: {{ .Values.service.type }}
  # Fixing IP address
  loadBalancerIP: 192.168.64.95
  ports:
    - port: {{ .Values.service.port }}
      targetPort: {{ .Values.service.targetPort }}
      protocol: TCP
      name: http
...
```

As I've set the `metallb` load balancer with a fixed range of IP addresses I can pick out one
and expose it on my MacBook.

## Helm
List out the deployed helm charts as follows:
```
helm list

# You would not normally get any output here, but I've deployed kafka so I get
NAME            NAMESPACE       REVISION        UPDATED                                 STATUS          CHART           APP VERSION
my-release      default         1               2022-06-30 19:27:51.07704 +0100 BST     deployed        kafka-18.0.0    3.2.0
```

Now lets deploy our SpringBoot application:
```
helm install boot2 ./src/main/helm/boot2

# Check the pods
kubectl get pods

# The output
NAME                                   READY   STATUS    RESTARTS       AGE
my-release-zookeeper-0                 1/1     Running   11 (20m ago)   121d
nginx-pv-7657c86c55-jb8s9              1/1     Running   12 (20m ago)   123d
spring-boot-for-k8s-59b8c49c55-v2nx9   1/1     Running   25 (20m ago)   137d
my-release-kafka-0                     1/1     Running   26 (19m ago)   121d
boot2-6b784484ff-kxwlh                 1/1     Running   0              22s
```

You can see I have other stuff running, but importantly you can see:
```
boot2-6b784484ff-kxwlh                 1/1     Running   0              22s
```

Check deployments:
```
kubectl get deployments

# The output
NAME                  READY   UP-TO-DATE   AVAILABLE   AGE
nginx-pv              1/1     1            1           123d
spring-boot-for-k8s   1/1     1            1           137d
boot2                 1/1     1            1           102s
```

Now the services, specifically we are interested in the IP addresses:
```
kubectl get services

# The output
NAME                            TYPE           CLUSTER-IP       EXTERNAL-IP     PORT(S)                      AGE
kubernetes                      ClusterIP      10.152.183.1     <none>          443/TCP                      207d
spring-boot-service             LoadBalancer   10.152.183.187   192.168.64.50   80:30275/TCP                 137d
nginx-pv-service                NodePort       10.152.183.42    <none>          9095:32349/TCP               123d
my-release-zookeeper-headless   ClusterIP      None             <none>          2181/TCP,2888/TCP,3888/TCP   121d
my-release-kafka-headless       ClusterIP      None             <none>          9092/TCP,9093/TCP            121d
my-release-zookeeper            ClusterIP      10.152.183.199   <none>          2181/TCP,2888/TCP,3888/TCP   121d
my-release-kafka                ClusterIP      10.152.183.95    <none>          9092/TCP                     121d
my-release-kafka-0-external     LoadBalancer   10.152.183.112   192.168.64.90   9094:32052/TCP               121d
boot2                           LoadBalancer   10.152.183.68    192.168.64.95   80:30509/TCP                 2m36s
```

It is exposed on the local MacBook via IP address `192.168.64.95` on port 80.

Let's try it:
```
curl http://192.168.64.95/status/SteveLimbX

# The output
{"acceptable":false,"reasonUnacceptable":"Fails Business Logic Check"}
```

We can now try the helm listing again:
```
helm list

# The output
NAME            NAMESPACE       REVISION        UPDATED                                 STATUS          CHART           APP VERSION
boot2           default         1               2022-10-30 17:06:25.163645 +0000 UTC    deployed        boot2-1.0.1     1.0.2      
my-release      default         1               2022-06-30 19:27:51.07704 +0100 BST     deployed        kafka-18.0.0    3.2.0 
```

Looks like we're all good.

## Alterations and rebuilds
If you make a change to the Java SpringBoot application, then you must do the following:
- Update the build.gradle - i.e. change `version = '1.0.3'` in this case
- Update the Chart.yaml - i.e. change `appVersion: "1.0.3"` in this case
- Rebuild the application regenerate the docker image - i.e. `./gradlew jib`
- Redeploy the helm chart - i.e. `helm upgrade boot2 ./src/main/helm/boot2`

Now run `helm list` again:
```
NAME            NAMESPACE       REVISION        UPDATED                                 STATUS          CHART           APP VERSION
boot2           default         2               2022-10-30 17:26:23.146163 +0000 UTC    deployed        boot2-1.0.1     1.0.3      
my-release      default         1               2022-06-30 19:27:51.07704 +0100 BST     deployed        kafka-18.0.0    3.2.0
```

You can see the Chart version is the same but the application version is now 1.0.3.

This is important, the Chart version is how the Kubernetes configuration is set up. The
`appVersion` is the actual application itself.

To remove the application just use `helm uninstall boot2`

## Summary

So that's it, we've got our basic SpringBoot app.

- It is fully tested - from a developer point of view
- It has 100% code coverage
- It passes all `CheckStyle` (Google Style) checks
- It passes all `SonarLint` checks
- It meets all our requirements
- It can be deployed via helm into a Kubernetes cluster.
