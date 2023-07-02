package idawi.demo.valentin;

import java.util.Collection;
import java.util.Random;

import idawi.Component;
import idawi.Event;
import idawi.RuntimeEngine;
import idawi.Service;
import idawi.SpecificTime;
import idawi.transport.UDPDriver;
import toools.collections.Collections;

abstract class MobilityEvent extends Event<SpecificTime> {
	protected Random prng;

	public MobilityEvent(double date, Random prng) {
		super(new SpecificTime(date));
		this.prng = prng;
	}

}