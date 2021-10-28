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

```java=
// prints out the Java version
System.out.println("You are using JDK " + System.getProperty("java.version"));

// creates a *local* peer that will drive the deployment
Component t = new Component(ComponentDescriptor.fromCDL("name=parent"));

// describes the child peer that will be deployed to
ComponentDescriptor child = new ComponentDescriptor();
InetAddress childHost = InetAddress.getByName(args[0]);
child.inetAddresses.add(childHost);
child.friendlyName = childHost.getHostName();
child.sshParameters.hostname = childHost.getHostName();

// deploy
t.lookupService(DeployerService.class).deploy(Set.of(child), true, 10000, true,
		feedback -> System.out.println("feedback: " + feedback), ok -> System.out.println("peer ok: " + ok));

// at this step the child is running on the remote host. We can interact with
// it.
long pingTime = System.currentTimeMillis();
Message pong = PingService.ping(t.lookupService(PingService.class), child, 1000);

if (pong == null) {
	System.err.println("ping timeout");
} else {
	long pongDuration = System.currentTimeMillis() - pingTime;
	System.out.println("pong received after " + pongDuration + "ms");
}

```



Contributors:
- Antonin Lacomme (Master degree intern)
- Fedi Ghalloussi (Bachelor degree intern)
