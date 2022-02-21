package idawi;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import idawi.MessageQueue.Enough;

public abstract class MessageIterable<T> implements Iterable<T> {
	public Stream<T> stream() {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false);
	}
	

}
