package idawi.routing;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

public class FloodingWithSelfPruning_UsingBloomFilterParm extends RoutingParms {
	BloomFilter<Long> neighbors;

	public FloodingWithSelfPruning_UsingBloomFilterParm(int n) {
		this.neighbors = BloomFilter.create(Funnels.longFunnel(), n, 0.01);
	}
}