package idawi.transport;

import java.io.IOException;
import java.util.Base64;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;
import toools.math.MathsUtilities;

public class PipeFromToParentProcess extends TransportService {
	public boolean suicideIfLoseParent = true;

	public PipeFromToParentProcess(Component me) {
		super(me);

		Idawi.agenda.threadPool.submit(() -> {
			try {
				while (true) {
					processIncomingMessage((Message) component.secureSerializer.read(System.in));
				}
			} catch (Exception e) {
				e.printStackTrace();

				if (suicideIfLoseParent) {
					System.exit(0);
				}
			}
		});
	}

	@Override
	protected void sendImpl(Message msg) {
		send(msg);
	}

	public void send(Object o) {
		sendBytes(component.secureSerializer.toBytes(o));
	}

	public static void sendBytes(byte[] bytes) {
		var base64 = new String(Base64.getEncoder().encode(bytes));
		base64 = base64.replace("\n", "");
		System.out.println(PipesFromToChildrenProcess.base64ObjectMark + base64);
	}

	@Override
	public String getName() {
		return "pipe to parent";
	}

	
	@Override
	public void dispose(Link l) {
		try {
			System.in.close();
		} catch (IOException e) {
		}

		System.out.close();
	}
	
	@Override
	public double latency() {
		return MathsUtilities.pickRandomBetween(0.000010, 0.000030, Idawi.prng);
	}

}
