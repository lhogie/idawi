package idawi.demo;
import idawi.Component;
import idawi.Service;
import idawi.service.DeployerService;
import idawi.service.ServiceManager;
import idawi.service.rest.RESTService;
import toools.thread.Threads;

public class Rest {

	public static void main(String[] args) throws Throwable {
		Component a = new Component();
		var deployer = a.lookupService(DeployerService.class);
		deployer.deployInThisJVM(2, i -> "newc" + i, true, p -> System.out.println(p.friendlyName + " is ok"));

		var s = new Service(a);
		a.lookupService(ServiceManager.class).ensureStarted(RESTService.class);
		a.lookupService(RESTService.class).startHTTPServer();
		Threads.sleepForever();
	}

}