# pafadipo


# Description

*Pafadipo* is a Java middleware for distributed applications.


# Features
Main features of *Pafadipo* include:
- decentralized, P2P
- sync and async messaging
- queue-based messaging
- support SSH, TCP, UDP, file-based communication
- deployment through SSH
- comes with many base services for deployment, publish-subscribe, system-monitoring, service lifecycle.
- support futures

# Motivation and design objectives

Distributed computed is notouriously difficult field. Writing distributed applications is hard because programmers have to deal with concurent access to resources, sophisticated protocols and complex middleware.
These middleware most of the time assume characteristics that are found on specific architecture (shared filesystem, single LAN, homogeneous computer architectures, etc).

https://www.reactivemanifesto.org/?fbclid=IwAR1MLUre_3kmwjyZJaPLGOgtwpPjdRXwGODQvxBd6W_7qL5fWv9OeSd_smY
## Transparency


# Comparison to existing platforms
JGroups
Akka
Hazelcasts
Terracota
Apache River

## Jade
Jade is supported by Telecom Italia.
Its naming service is cenralized
In accordance to the FIPA standard, Jade imposes a number of agent-specific high-level asbractions which concern their behavior, their interactions, etc.
It receives messages in blocking mode.



# Overview of the architecture
## A thing
is a container for one or multiple agents.
It features a network bridge allowing its agents to communicate with other agents in other things in the same JVM/computer/LAN or not.
### Networking
Networking functionality relies on the concept of *protocol driver*.
A protocol driver enables the reception and the emission of messages through network protocols. 
#### Local Method Invocation
Is used when the two agents involved in the communication are in the same JVM.
#### UDP
is the preferred communication protocol because its fast.
Unreliability of UDP is overcomed by higher level communication control in *Pafadipo*.

#### TCP
is slower than UDP but has the advantage of allowing the use of SSH-tunneling, which is required when the plain TCP ports are inacessiible because of a firewall or a NAS.

#### SSH execution
is primarily used to deploy things on remote computers. The communication streams between the SSH client and SSH server are used to transport messages. This mechanism can be used even when SSH tunelling not available.

#### file-based commi
Sometimes nodes have no way to reach one another, but they share a common file system. This is the case of two computers in different LANs, each one connected to a common cloud storage.



## A agent
The upfront concept is the one of an *agent*. An agent is a plain Java object augmented with a functionalities for

## Execution of agent-specific code
An agent can execute code. To do this, it has to use its own threads. Helpers methods have been defined to assist the programmers.

## Communication
An agent is able to communicate. Communication is done through message queues.
### Message
A message is an object. It features a set of recipents, 
### Queues
Each agent has message queues.

