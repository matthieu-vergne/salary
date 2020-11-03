package fr.vergne.salary;

import java.util.List;
import java.util.Objects;

class Equalable<T> {
	private final Class<T> refClass;
	private final T ref;
	private final List<Extractor<T, Object>> extractors;

	interface Extractor<T, U> {
		U componentFrom(T t);
	}

	@SafeVarargs
	public Equalable(Class<T> refClass, T ref, Extractor<T, Object>... components) {
		this.refClass = refClass;
		this.ref = ref;
		this.extractors = List.of(components);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == ref) {
			return true;
		} else if (refClass.isInstance(obj)) {
			T that = refClass.cast(obj);
			return extractors.stream().//
					map(extract -> Objects.equals(extract.componentFrom(ref), extract.componentFrom(that))).//
					filter(parameterIsEqual -> !parameterIsEqual).//
					findFirst().orElse(true);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		Object[] components = extractors.stream()//
				.map(extract -> extract.componentFrom(ref))//
				.toArray();
		return Objects.hash(components);
	}
}
