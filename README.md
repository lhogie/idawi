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




# Example usage
## Creating components
### Creating a few components inside a single JVM
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
## Creating a new service
This installs a new service in a component:
```java
		var a = new Component();
		var s = new Service(a);
```
But here, s has no specific operations.
### Creating a new operation
```java
public static void main(String[] args) throws IOException {
	var a = new Component();
	
	// installs the service in component
	var s = new ExampleService(a);
}

public static class ExampleService extends Service {

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


### lookup a specific service
Services are identified by their class.
```java
MyService s = a.lookup(ExampleService.class);
```

### lookup a specific operation
Just like services, operation are identified by their class.
```java
var o = s.lookup(ExampleOperation.class);
```

A quicker way exists:
```java
var o = a.lookupO(MyService.ExampleOperation.class);
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
Print to incoming results from the running operation during 1s.
```java
rop.returnQ.collect(1, 1, c -> System.out.println("just received : " + c.messages.last().content));
```

Synchronously waits for 1 first result.
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

