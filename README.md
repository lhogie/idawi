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
		child.sshParameters.hostname = "192.168.32.44";
		a.lookup(DeployerService.class).deploy(Set.of(child), true, 10000, true,
				feedback -> System.out.println("feedback: " + feedback), ok -> System.out.println("peer ok: " + ok));
```
## Creating a new service
### Creating a new operation




# People involved
 *Idawi* is developed at the
[Computer Science Laboratory of the Universté Côte d'Azur](http://www.i3s.unice.fr/en/comredEn) ([Nice](https://www.google.com/maps/@43.5168069,6.6753034,5633a,35y,67.34h,76.97t/data=!3m1!1e3), France),
a joint lab of [Cnrs](https://www.cnrs.fr) and [Inria Sophia Antipolis](https://www.inria.fr), by:
- [Luc Hogie](http://www.i3s.unice.fr/~hogie/) (project manager and main developer)
- Antonin Lacomme (Master degree intern)
- Fedi Ghalloussi (Bachelor degree intern)

:new: **You can now [chat with other members of Idawi's community](http://webchat.ircnet.net/?channels=idawi&uio=MT11bmRlZmluZWQb1) as they are connected!** :satisfied:

