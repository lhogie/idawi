package idawi.transport;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;
import toools.exceptions.NotYetImplementedException;
import toools.math.MathsUtilities;

public class WiFiDirect extends WirelessTransport {

	public WiFiDirect(Component c) {
		super(c);
		emissionRange = MathsUtilities.randomize(emissionRange, 0.1, Idawi.prng);
	}

	@Override
	public String getName() {
		return "Wi-Fi";
	}


	@Override
	protected void sendImpl(Message msg) {
		throw new NotYetImplementedException();
	}

	@Override
	public void dispose(Link l) {
	}

	@Override
	public double typicalEmissionRange() {
		return 200;
	}
	
	@Override
	public double latency() {
		return MathsUtilities.pickRandomBetween(0.010, 0.030, Idawi.prng);
	}

}
