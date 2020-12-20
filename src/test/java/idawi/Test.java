package idawi;
import idawi.net.LMI;
import idawi.service.DummyService;
import toools.thread.Threads;

public class Test {

	public static void main(String[] args) throws Throwable {
		Component a = new Component();
		Component b = new Component();
		LMI.connect(a, b);
		b.lookupService(DummyService.class).registerOperation("neverReturns", (m, r) -> Threads.sleepForever());
		MessageList r = b.lookupService(DummyService.class).call(new To(b,  DummyService.class, "neverReturns")).collect().throwAnyError();
		System.out.println(r.timeout);
		
		
		// be sure c1 got an answer
//		assertNotEquals(null, pong);

		// clean
		Component.componentsInThisJVM.clear();
	}

}