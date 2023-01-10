package idawi.demo.website;

import java.io.IOException;

import idawi.Component;
import idawi.InnerOperation;
import idawi.Message;
import idawi.MessageQueue;
import idawi.Service;
import idawi.To;
import idawi.demo.website.CreateNewService2.ExampleService.ExampleOperation;

public class CreateNewService2 {
	public static void main(String[] args) throws IOException {
		var a = new Component();

		// installs the service in component
		new ExampleService(a);

		ExampleService s = a.lookup(ExampleService.class);

		var o = s.lookup(ExampleOperation.class);

		o = a.operation(ExampleService.ExampleOperation.class);

		// the operation has to be scheduled to an addess that can refer to multiple
		// components
		var to = new To(a);

		// we obtain a bridge to the remotely running operation
		var rop = o.exec(to, true, null);

		rop.returnQ.collect(1, 1, c -> {
			System.out.println("just received : " + c.messages.last().content);
			c.stop = true;
		});
	}

	public static class ExampleService extends Service {

		public ExampleService(Component component) {
			super(component);
			registerOperation(new ExampleOperation());
		}

		public class ExampleOperation extends InnerOperation {
			@Override
			public void impl(MessageQueue in) throws Throwable {
				Message triggerMessage = in.poll_sync();
				reply(triggerMessage, "a result");
				reply(triggerMessage, "another result");
			}

			@Override
			public String getDescription() {
				return "an operation that does nothing";
			}
		}
	}
}
