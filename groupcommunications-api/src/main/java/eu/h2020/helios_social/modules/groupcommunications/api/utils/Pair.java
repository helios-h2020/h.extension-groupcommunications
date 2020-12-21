package eu.h2020.helios_social.modules.groupcommunications.api.utils;

import javax.annotation.concurrent.Immutable;

@Immutable
public class Pair<A, B> {

	private final A first;
	private final B second;

	public Pair(A first, B second) {
		this.first = first;
		this.second = second;
	}

	public A getFirst() {
		return first;
	}

	public B getSecond() {
		return second;
	}
}
