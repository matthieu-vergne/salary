package fr.vergne.salary.model;

import java.util.function.Function;
import java.util.function.Supplier;

public interface Mutator<T> {
	T apply(T t);

	static <T> Mutator<T> create(Supplier<String> nameSupplier, Function<T, T> definition) {
		return new Mutator<>() {

			@Override
			public T apply(T t) {
				return definition.apply(t);
			}

			@Override
			public String toString() {
				return nameSupplier.get();
			}
		};
	}
}
