package idawi.knowledge_base.info;

import idawi.knowledge_base.Info;

public abstract class ObjectInfo<E> extends Info {
	protected E value;

	public ObjectInfo(double date, E v) {
		super(date);
		this.value = v;
	}
}
