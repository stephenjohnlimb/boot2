## A SpringBoot App with Gradle deployed in Kubernetes

This covers, the creation of a SpringBoot app, using gradle as the build mechanism,
then finally deploying it via a helm Chart into a Kubernetes environment (microk8s).

I also cover an Agile approach on how to do the above.

### How did this project get created

I used IntelliJ with the `Spring Boot / Spring Initializr project wizard` plugin.

This means I can just use: `File -> New -> Project` and then use the following dialog to create the project.

#### IntelliJ Spring Initializr dialog

![Spring Initializr dialog](NewProject.png "New Project Dialog")

#### Project Settings
I didn't really need to do anything to alter this project, other than write these few notes.

### So what now?

Well first you can just use the IntelliJ `Build -> Rebuild Project` or you can use the `Gradle view` to
trigger builds. Finally, it is also possible to use the `Terminal` to issue direct gradle commands.

#### Gradle commands via terminal
I find this approach most useful; because I'm used to CLI in general. You can just the following to test the
default SpringBoot app context loads: `./gradlew clean test --info`.

This will give you quite a bit of information and also show that you Spring boot app runs up OK (which it should as it is minimal).

So, I suppose this brings me to the first 'talking point', everytime I make any changes I like to ensure the whole solution
still runs OK (even though there is no functionality yet). I don't tend to spend lots of time developing without running and testing. 
### Pushing to GitHub

So this new project only exists on this laptop at present, so now I'll export it up to my GitHub repo.
Then I can work on it from various computers.

#### How to export a project
First off you'll need a GitHub account and have that setup in IntelliJ (or other IDE).
I use the token mechanism that works well GitHub and IntelliJ.

See the `VCS -> Share Project on GitHub` option, you will be prompted with a dialog like this below:

![GitHub Initial Commit dialog](InitialCommit.png "Commit new project to GitHub")

### Profiles and Conditional Beans

See [Profiles and Beans](ProfilesAndBeans.md) on a simple way to conditionally use different beans.
But note this is really only useful in very simple situations. If you need lots of conditional beans
for a variety of situations things are going to get complex. You may need to read
[Avoiding Profiles](https://reflectoring.io/dont-use-spring-profile-annotation/) as this outlines the issues
relating to just using profiles.

## On to Actual Development

### Initial basic development
See [Agile Development](AgileDevelopment.md) for how I'm going to approach adding in functionality
to this simple app. This also covers how to wrap it for deployment into a Kubernetes environment. 

### Making the application deployable
The link above also covers [making the SpringBoot app deployable](Dockerizing.md).

### Adding in more functionality
I decided that I would add more functionality to this existing Microservice to demonstrate how additional
services can be included - again in an Agile way.

See [Adding Email Validation](AgileEmailValidationDevelopment.md) for the changes that are made.

### Another new requirement
This requirement is to ensure the microservices have some form of external documentation (swagger/OpenAPI).
See [Adding Swagger/OPEN API Documentation](AgileOpenAPIDevelopment.md) for the changes need to be made to meet this new requirement.

## Summary
Hopefully from this little project, you can see that actually adopting an 'Agile' and incremental approach
to development can actually work.

As I said at the top of this page, I can't tell you when it will all be finished. Because in reality
software is never really finished, there's always more that can be added/updated/improved.

Unfortunately, the people with the money and finite timelines - never really get this (I don't think they ever will).

Don't get frustrated by this - just drive forward with development. 