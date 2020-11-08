package fr.vergne.salary.util;

import java.util.List;
import java.util.Objects;

public class Equalable<T> {
	private final Class<T> referenceClass;
	private final T reference;
	private final List<Extractor<T, Object>> extractors;

	public interface Extractor<T, U> {
		U componentFrom(T t);
	}

	@SafeVarargs
	public Equalable(Class<T> referenceClass, T reference, Extractor<T, Object>... components) {
		this.referenceClass = referenceClass;
		this.reference = reference;
		this.extractors = List.of(components);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == reference) {
			return true;
		} else if (referenceClass.isInstance(obj)) {
			T other = referenceClass.cast(obj);
			return extractors.stream().//
					map(extract -> Objects.equals(extract.componentFrom(reference), extract.componentFrom(other))).//
					filter(parameterIsEqual -> !parameterIsEqual).//
					findFirst().orElse(true);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		Object[] components = extractors.stream()//
				.map(extract -> extract.componentFrom(reference))//
				.toArray();
		return Objects.hash(components);
	}
}
