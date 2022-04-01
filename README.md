*Idawi* is a Java middleware for Research in distributing computing, and it application fields (HPC, IOT, fog/edge computing, IA, etc). It is developed at the
[Computer Science Laboratory of the Universté Côte d'Azur](http://www.i3s.unice.fr/en/comredEn) ([Nice](https://www.google.com/maps/@43.5168069,6.6753034,5633a,35y,67.34h,76.97t/data=!3m1!1e3), France),
a joint lab of [Cnrs](https://www.cnrs.fr) and [Inria Sophia Antipolis](https://www.inria.fr).

*Idawi* is extensively described in this [working paper](https://hal.archives-ouvertes.fr/hal-03562184). To make a long story short, it makes applications based on it to be organized as **distributed component system**. To this purpose it comes with the following features:
- it has a carefully object-oriented with SOA flavors
- it has a collective message+queue-oriented communication model
- it has a collective computation model
- it has automatized deployment/bootstrapping of components through SSH
- it provides interoperability through a REST-based web interface
- it enables the programmer to work in a *trials and errors* mode within his favourite IDE
- it is a fully [decentralized](https://en.wikipedia.org/wiki/Decentralised_system) P2P [service-oriented](https://en.wikipedia.org/wiki/Service-oriented_architecture) architecture
- it has independance to transport network (includes support for SSH, TCP and UDP)
- it does [lock-free](https://preshing.com/20120612/an-introduction-to-lock-free-programming/) multi-core parallelism
- it has the ability to do emulation

Here is an example code of a component deploying another one in another JVM and then invoking an operation on it.
```java
// creates a component in this JVM
var c1 = new Component();

// prints the list of its builtin services
c1.services().forEach(s -> System.out.println(s));

// among them, picks up the service for component deployments
var deployer = c1.lookupService(DeployerService.class);

// and prints the operations exposed by it
System.out.println(deployer.listOperationNames());

// we'll put another component in a different JVM
var c2d = new ComponentDescriptor();
c2d.friendlyName = "other component";
c1.lookupService(DeployerService.class).deployOtherJVM(c2d, true, feedback -> {}, ok -> {});

// creates a new service that asks the other component to compute something
new Service(c1) {
	public void run() {
		// executes an operation (exposed by DummyService) which computes the length of
		// a given string
		var l = exec(new ServiceAddress(Set.of(c2d), DummyService.class),
				DummyService.stringLength2, 1, 1, "Hello Idawi!");
		System.out.println(l);
	}
}.run();
```



Contributors:
- Luc Hogie(http://www.i3s.unice.fr/~hogie/) (project manager and main developer)
- Antonin Lacomme (Master degree intern)
- Fedi Ghalloussi (Bachelor degree intern)

:new: **You can now [chat with other members of Idawi's community](http://webchat.ircnet.net/?channels=idawi&uio=MT11bmRlZmluZWQb1) as they are connected!** :satisfied:

