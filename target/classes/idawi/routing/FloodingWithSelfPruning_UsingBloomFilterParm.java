package idawi.routing;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import toools.exceptions.NotYetImplementedException;

public class FloodingWithSelfPruning_UsingBloomFilterParm extends EmptyRoutingParms {
	BloomFilter<Long> neighbors;

	public FloodingWithSelfPruning_UsingBloomFilterParm(int n) {
		this.neighbors = BloomFilter.create(Funnels.longFunnel(), n, 0.01);
	}

	@Override
	public long sizeOf() {
		throw new NotYetImplementedException();
	}

}