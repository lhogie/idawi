package idawi.transport;

import java.util.Collection;

import idawi.Component;
import idawi.Idawi;
import toools.exceptions.NotYetImplementedException;
import toools.math.MathsUtilities;

public class WiFiDirect extends WirelessTransport {

	public WiFiDirect(Component c) {
		super(c);
		emissionRange = MathsUtilities.randomize(emissionRange, 0.1, Idawi.prng);
	}

	@Override
	public String getName() {
		return "Wi-Fi direct";
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

	@Override
	protected void multicast(byte[] msg, Collection<Link> outLinks) {
		throw new NotYetImplementedException();
	}
}
