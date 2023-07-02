package idawi;

import java.io.Serializable;
import java.util.function.Predicate;

public abstract interface When extends Predicate<Event<?>>, Serializable {
}