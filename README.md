# Description

*Idawi* is a Java middleware for decentralized applications, as they can be found in the Internet Of Things (IOT), in the fog, in Mobile Ad hoc Networks (MANETs), etc. 
Because who can do more can do less, it can also be used for usual distributed computing in cluster. 
*Idawi* is extensively described in this [working paper](https://hal.archives-ouvertes.fr/hal-03562184). To make a long story short, it provides a **structuring framework** and implementations of algorithms for the construction and operation of decentralized systems. 

 *Idawi* is developed at the
[Computer Science Laboratory of the Université Côte d'Azur](http://www.i3s.unice.fr/en/comredEn) ([Nice](https://www.google.com/maps/@43.5168069,6.6753034,5633a,35y,67.34h,76.97t/data=!3m1!1e3), France),
a joint lab of [Cnrs](https://www.cnrs.fr) and [Inria Sophia Antipolis](https://www.inria.fr)
Its design is driven by our practical experience in distributed computing applied to scientific experimentation. 

Here are some of his features of interest:
- it has a polished **mixed object/message/queue/component/service-oriented architecture**
- it has a **collective** communication and computation models
- it has **automatized deployment**/bootstrapping of components through SSH
- SSH-bootstrapping allow *Idawi*-based application to work even in the presence of firewalls/NATs.
- it provides interoperability through a **REST-based web interface**
- it enables the programmer to work in a *trials and errors* mode within his favourite IDE
- it enables the construction of **decentralized** systems
- it provides agnosticism to transport network (includes support for SSH, TCP and UDP)
- it does [lock-free](https://preshing.com/20120612/an-introduction-to-lock-free-programming/) multi-core parallelism
- it is able to do **emulation**


Team members:
- [Luc Hogie](http://www.i3s.unice.fr/~hogie/)
- Paul Gros (Engineering & Master2 degree intern in srping 2023)
-  (Engineering & Master2 degree intern in srping 2023)
-  (Engineering & Master2 degree intern in srping 2023)
-  (Engineering & Master2 degree intern in srping 2023)
-  (Engineering & Master2 degree intern in srping 2023)
-  (Engineering & Master2 degree intern in srping 2023)
- Eliel Wotobe (Master1 degree intern in autumn 2022)
- (Saad El Din Ahmed)[https://www.linkedin.com/in/saadeldinahmed] (Master1 degree intern in spring 2022)
- Mamadou Camara (Master degree intern in spring 2022)
- Khadidiatou Dieye (Master1 degree intern in autumn 2021)
- Ahmed El Hanafi Si Dehbi (Master1 degree intern in autumn 2021)
- Fedi Ghalloussi (Bachelor degree intern in spring 2021)
- Antonin Lacomme (Master degree intern in autumn 2020)

:new: **You can now [chat with other members of Idawi's community](http://webchat.ircnet.net/?channels=idawi&uio=MT11bmRlZmluZWQb1) as they are connected!** :satisfied:



# Quick start
The following tutorial shows and explains the very basic concepts in *Idawi*. It order to keep it short and useful, we show here only the concepts that help for a quick start.
## Installing

We recommend you to install *Idawi* using a package manager (like Maven), which will manage the dependancies for you.
Version history can be found [there](https://search.maven.org/artifact/io.github.lhogie/idawi).

## Creating components
Components are first-class citizens in *Idawi*. They aim a representing logical (business) entities in the user application domain.
Technically, within a given JVM, a component is plain Java object (POJO).
Two components in a same JVM can communicate using the LMI (Local Method Invocation) protocol, which relies on shared memory. But they can also be forced to use other protocols like TCP or UDP. Two components in different JVMs, on the same node or not, must communicate via the network stack.

### Creating a few components inside a single JVM
```java
public IdawiQuickStart {
	public static void main(String... args) {
		// creates 3 components in this JVM
		var a = new Component("a");
		var b = new Component("b");
		var c = new Component("c");

		// enable them to communicate via shared memory
		// b be relay messages to/from a and c
		LMI.connect(a, b);
		LMI.connect(b, c);
	}
}
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
### Deploying new components to a remote node
```java
var a = new Component();
var b = new ComponentDescriptor();
child.name = "b";
child.sshParameters.hostname = "192.168.32.44"; // this is where the b will be deployed to
a.lookup(DeployerService.class).deploy(Set.of(b), true, 10000, true,
	feedback -> System.out.println("feedback: " + feedback), ok -> System.out.println("peer ok: " + ok));
```


### Services and operations
The only thing a component can do, is exposing services to other components. A service exposes a set of operations, which can be triggered by other components. These operations constitute the API of the service. They implement the concern the service is about. They are many builtin services in a components. These can be listed like this:
```java
var services = a.services();
```
Builtin services enable node messaging, routing, service lifetime management, system monitoring, error logging, HTTP interoperability, etc.

Services in component are identified by their class.  This makes it possible to identify an operation by its class name, in a way that can be verified by the compiler.
A specific service can be searched for like this:
```java
var s = a.lookup(ExampleService.class);
```

Just like services, operation are identified by their class.
```java
var o = s.lookup(ExampleOperation.class);
```
An operation is a  piece of sequential code that is fed by a queue of messages. It is triggered by an initial message (called the *trigger message*) sent by a component. This input queue is public can it can be fed by any component in the application. Also the return of an operation is not limited to a single value. Operation send data anytime durin their execution by sending message to any other component in the system.
In many cases, an operation will obtain one single input message and will reply one single message to its *caller*. But the multiplicity of input/output has many advantages. In particular it enables:
- stream processing
- the construction of complex workflows
- the emission of any information that is not a result, such as progress/debug information

Operation are scheduled into a pool of thread, which ensure scalability.

## Creating a new service and operation
A new *Idawi* application will come as one or several new services.
Creating a new service can be done by extending the __Service__ class, just like this:
```java
public class ExampleService extends Service {

	public ExampleService(Component component) {
		super(component);
		registerOperation(new ExampleOperation());
	}

	public class ExampleOperation extends InnerOperation {
		@Override
		public void impl(MessageQueue in) throws Throwable {
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
As you can see here, an operation must be declared as an inner class of its service class.


To use this new service, let us install it into a component:

```java
public static void main(String[] args) throws IOException {
	var a = new Component();
	
	// installs the service in component
	var s = new ExampleService(a);
}
```


### Invoking an operation
```java
// the operation has to be scheduled to an address that can refer to multiple components
// in our case, let it run on component "a" only
var to = new To(a);

// the operation is run asynchronously
// we obtain a bridge to the remotely running operation
var rop = o.exec(to, true, null);
```
Operation execution is always __asynchronous___.

### Obtaining result
One strength of *Idawi* lies in it result collection algorithm. This algorithm performs __synchronous__ collection of messages, from the return input queue of the remotely running operation.

In its most general form:
Waits 1s for incoming results. Print them as they come.
Stops when 3 messages have been received.
```java
rop.returnQ.collect(1, 1, c -> {
	System.out.println("just received : " + c.messages.last().content);
	c.stop = c.messages.size() == 3;
});
```

Many variation on this pattern are proposed to the user.

Waits 1s for a result:
```java
var r = rop.returnQ.collect(1);
```

```java
rop.returnQ.collect(1, 1, c -> System.out.println("just received : " + c.messages.last().content));
```


### Dealing with remote errors
```java
r.throwExceptionsIfAny();
```

### Monitoring a running system
In progress
