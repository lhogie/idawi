package idawi;

import java.util.Set;

import org.junit.jupiter.api.Test;

import idawi.service.DeployerService;
import idawi.service.FileService;
import toools.io.file.RegularFile;

public class TestFileService {
	public static final String ssh = "musclotte.inria.fr";

	@Test
	public void pingViaSSH() throws Throwable {
		Component c1 = new Component();
		ComponentDescriptor c2 = ComponentDescriptor.fromCDL("name=c2 / ssh=" + ssh);
		c1.lookupService(DeployerService.class).deploy(Set.of(c2), true, 10, true, fdbck -> System.out.println(fdbck),
				p -> System.out.println("ok"));
		var client = new Service(c1);

		System.out.println(client.exec(new ComponentAddress(Set.of(c2)), FileService.find, true, null).returnQ.collect()
				.throwAnyError().resultMessages().contents());

		client.exec(new ComponentAddress(Set.of(c2)), FileService.upload, true,
				new OperationParameterList("test", new RegularFile("LICENSE").getContent())).returnQ.collect()
						.throwAnyError();

		System.out.println(client.exec(new ComponentAddress(Set.of(c2)), FileService.find, true,
				new OperationParameterList()).returnQ.collect().throwAnyError().resultMessages().contents());

		System.out.println(new String(
				(byte[]) client.exec(new ComponentAddress(Set.of(c2)), FileService.download, true, "test").returnQ
						.collect().throwAnyError().resultMessages().first().content));

		// assertNotEquals(null, pong);

		// clean
		Component.componentsInThisJVM.clear();
	}
}
