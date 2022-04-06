# Description

*Idawi* is a Java middleware for distributed applications. It is extensively described in this [working paper](https://hal.archives-ouvertes.fr/hal-03562184). To make a long story short, it provides a structuring framework and implementations of algorithms for the construction of **distributed component systems**. Besides, *Idawi* aims at maximizing its usefulness to Researchers working in the application fields of distributing computing (HPC, IOT, fog/edge computing, IA, etc).
To this purpose it comes with the following features:
- it has a carefully object-oriented with SOA flavors
- it has a collective (message/queue)-oriented communication model
- it has a collective computation model
- it has automatized deployment/bootstrapping of components through SSH
- it provides interoperability through a REST-based web interface
- it enables the programmer to work in a *trials and errors* mode within his favourite IDE
- it is a fully [decentralized](https://en.wikipedia.org/wiki/Decentralised_system) P2P [service-oriented](https://en.wikipedia.org/wiki/Service-oriented_architecture) architecture
- it has independance to transport network (includes support for SSH, TCP and UDP)
- it does [lock-free](https://preshing.com/20120612/an-introduction-to-lock-free-programming/) multi-core parallelism
- it has the ability to do emulation




# Quick start
The following tutorial shows and explains the very basic concepts in *Idawi*. It order to keep it short and useful, we show here only the concepts that help for a quick start.
## Installing

We recommend you to install *Idawi* using Maven. To do this, simply add the following dependency to the POM file of your project:
```maven
<dependency>
  <groupId>io.github.lhogie</groupId>
  <artifactId>idawi</artifactId>
  <version>0.0.5</version>
</dependency>
```
As this tutorial is not updated every single time a new version of the code is released, please first make sure you will get the very last version: [Maven central](https://search.maven.org/artifact/io.github.lhogie/idawi).

## Creating components
### Creating a few components inside a single JVM
In a JVM, a component is POJO with a few things in it.
Two components in a same JVM can communicate using the LMI (Local Method Invocation) protocol, which relies on shared memory. But they can also be forced to use other protocols like TCP or UDP.
```java
// creates 3 components in this JVM
var a = new Component("a");
var b = new Component("b");
var c = new Component("c");

// enable them to communicate via shared memory
// b be relay messages to/from a and c
LMI.connect(a, b);
LMI.connect(b, c);
```
### Deploying new components to another JVM in same node

Here is an example code of a component deploying another one in another JVM and then invoking an operation on it.
```java
var a = new Component();

// remote components are referred to as a descriptor
var b = new ComponentDescriptor();
b.name = "b";

// ask the deployment service on "a" to deploy "b" according to its description
a.lookup(DeployerService.class).deployOtherJVM(b, true, feedback -> {}, ok -> {});
```
### Deploying new components to another JVM in other node
```java
var a = new Component();
		
ComponentDescriptor child = new ComponentDescriptor();
child.name = "b";
child.sshParameters.hostname = "192.168.32.44"; // this is where the b will be deployed to
a.lookup(DeployerService.class).deploy(Set.of(child), true, 10000, true,
	feedback -> System.out.println("feedback: " + feedback), ok -> System.out.println("peer ok: " + ok));
```


### lookup a specific service
Services in component are identified by their class. A specific service can be searched for like this:
```java
MyService s = a.lookup(ExampleService.class);
```

## Creating a new service
Let us create a new service. This can be done by extending the __Service__ class, just like this:
```java
public class ExampleService extends Service {

	public ExampleService(Component component) {
		super(component);
		registerOperation(new ExampleOperation());
	}

	public class ExampleOperation extends InnerOperation {
		@Override
		public void exec(MessageQueue in) throws Throwable {
			Message triggerMessage = in.get_blocking();
			reply(triggerMessage, "a result");
			reply(triggerMessage, "another result");
		}

		@Override
		public String getDescription() {
			return "an operation that does nothing";
		}
	}
}
```
As you can see here, an operation must be declared as an inner class of its service class. This makes it possible to identify an operation by its class name, in a way that can be verified by the compiler.


To use this new service, let us install it into a component:

```java
public static void main(String[] args) throws IOException {
	var a = new Component();
	
	// installs the service in component
	var s = new ExampleService(a);
}




### lookup a specific operation
Just like services, operation are identified by their class.
```java
var o = s.lookup(ExampleOperation.class);
```

### Invoking an operation
```java
// the operation has to be scheduled to an address that can refer to multiple components
var to = new To(a);

// the operation is run asynchronously
// we obtain a bridge to the remotely running operation
var rop = o.exec(to, true, null);
```

### Obtaining result
Synchronously waits 1s for incoming results. Print them as they come.
```java
rop.returnQ.collect(1, 1, c -> System.out.println("just received : " + c.messages.last().content));
```

Same thing but stops waiting when one message has arrived.
```java
rop.returnQ.collect(1, 1, c -> {
	System.out.println("just received : " + c.messages.last().content);
	c.stop = true;
});
```



# People involved
 *Idawi* is developed at the
[Computer Science Laboratory of the Universté Côte d'Azur](http://www.i3s.unice.fr/en/comredEn) ([Nice](https://www.google.com/maps/@43.5168069,6.6753034,5633a,35y,67.34h,76.97t/data=!3m1!1e3), France),
a joint lab of [Cnrs](https://www.cnrs.fr) and [Inria Sophia Antipolis](https://www.inria.fr), by:
- [Luc Hogie](http://www.i3s.unice.fr/~hogie/) (project manager and main developer)
- Antonin Lacomme (Master degree intern)
- Fedi Ghalloussi (Bachelor degree intern)

:new: **You can now [chat with other members of Idawi's community](http://webchat.ircnet.net/?channels=idawi&uio=MT11bmRlZmluZWQb1) as they are connected!** :satisfied:

