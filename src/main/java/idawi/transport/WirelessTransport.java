package idawi.transport;

import idawi.Component;

public abstract class WirelessTransport extends TransportService {

	public double emissionRange = typicalEmissionRange();


	public WirelessTransport(Component c) {
		super(c);
	}


	public abstract double typicalEmissionRange();

}
