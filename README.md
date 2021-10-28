:new: **You can now [chat with other members of Idawi's community](http://webchat.ircnet.net/?channels=idawi&uio=MT11bmRlZmluZWQb1) !** :satisfied:

*Idawi* is a Java decentralized middleware for distributed applications. It is developed at the
[Computer Science Laboratory of the Universté Côte d'Azur](http://www.i3s.unice.fr/en/comredEn) ([Nice](https://www.google.com/maps/@43.5168069,6.6753034,5633a,35y,67.34h,76.97t/data=!3m1!1e3), France),
a joint lab of [Cnrs](https://www.cnrs.fr) and [Inria Sophia Antipolis](https://www.inria.fr).

*Idawi* is described in this [draft](http://www.i3s.unice.fr/~hogie/idawi.pdf).

Its most notable features include:
- it has a carefully object-oriented designed with component and SOA flavors
- it has automatized deployment/bootstrapping of components through SSH, even in the presence of firewalls and/or NATs
- it has a novel queue-oriented model for distributed computing
- it has native group-based stream/object-oriented asynchronous/synchronous communication model
- it exposes a REST-based web interface
- it enables the programmer to work in a *trials and errors* mode within his favourite IDE
- it is a fully [decentralized](https://en.wikipedia.org/wiki/Decentralised_system) P2P [service-oriented](https://en.wikipedia.org/wiki/Service-oriented_architecture) architecture
- it has independance to transport network (includes support for SSH, TCP and UDP)
- it does massive [lock-free](https://preshing.com/20120612/an-introduction-to-lock-free-programming/) multi-core parallelism
- it provides the ability to do emulation

Target applications for *Idawi* include distributed computing, High Performance Computing (HPC), Internet of Things (IOT), Fog and Edge Computing, Artificial Intelligence (IA), Research in distributed/parallel algorithms, and emulation.

Contact: [Luc Hogie](http://www.i3s.unice.fr/~hogie/) (project manager and main developer)

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
- Antonin Lacomme (Master degree intern)
- Fedi Ghalloussi (Bachelor degree intern)
