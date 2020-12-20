package idawi;
import java.util.Set;

import org.junit.jupiter.api.Test;

import idawi.service.ComponentDeployer;
import idawi.service.FileService;
import toools.io.file.RegularFile;

public class TestFileService {
	public static void main(String[] args) throws Throwable {
		new TestFileService().pingViaSSH();
	}

	public static final String ssh = "musclotte.inria.fr";

	@Test
	public void pingViaSSH() throws Throwable {
		Component c1 = new Component();
		ComponentInfo c2 = ComponentInfo.fromCDL("name=c2 / ssh=" + ssh);
		c1.lookupService(ComponentDeployer.class).deploy(Set.of(c2), true, 10, true, fdbck -> System.out.println(fdbck),
				p -> System.out.println("ok"));

		System.out.println(c1.lookupService(FileService.class).call(new To(c2, FileService.class, "listFiles")).collect()
				.throwAnyError().resultMessages().contents());

		c1.lookupService(FileService.class).call(new To(c2, FileService.class, "uploadFileAsOneSingleMessage"), "test",
				new RegularFile("LICENSE").getContent()).collect().throwAnyError();

		System.out.println(c1.lookupService(FileService.class).call(new To(c2, FileService.class, "listFiles")).collect()
				.throwAnyError().resultMessages().contents());

		System.out.println(new String((byte[]) c1.lookupService(FileService.class)
				.call(new To(c2, FileService.class, "downloadFileAsOneSingleMessage"), "test").collect().throwAnyError()
				.resultMessages().first().content));

		// assertNotEquals(null, pong);

		// clean
		Component.componentsInThisJVM.clear();
	}
}
