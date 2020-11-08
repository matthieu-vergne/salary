package fr.vergne.salary.data;

import static java.util.stream.Collectors.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface SalariesDataset extends Dataset<Collection<Double>> {
	static SalariesDataset fromMap(Map<Profile, Collection<Double>> data) {
		return () -> data;
	}

	default SalariesDataset filterOnProfile(Profile profile) {
		return SalariesDataset.fromMap(Dataset.filterMapOnProfile(this.toMap(), profile));
	}

	default Statistics toStatistics() {
		SalariesDataset dataset = this;
		return new Statistics() {
			@Override
			public double q1() {
				double approximatedIndex = (double) (salariesCount() + 3) / 4.0;
				int index = (int) Math.rint(approximatedIndex);
				List<Double> sortedSalaries = salariesStream().sorted().limit(index + 2).collect(toList());
				double factor = approximatedIndex - index;
				if (factor == 0) {
					return sortedSalaries.get(index);
				} else {
					Double lowSalary = sortedSalaries.get(index);
					Double highSalary = sortedSalaries.get(index + 1);
					return (1 - factor) * lowSalary + factor * highSalary;
				}
			}

			@Override
			public double mean() {
				return salariesStream().mapToDouble(salary -> salary).average().getAsDouble();
			}

			@Override
			public double q3() {
				double approximatedIndex = (double) (salariesCount() + 3) / 4.0;
				int index = (int) Math.rint(approximatedIndex);
				List<Double> sortedSalaries = salariesStream().sorted(Comparator.reverseOrder()).limit(index + 2)
						.collect(toList());
				double factor = approximatedIndex - index;
				if (factor == 0) {
					return sortedSalaries.get(index);
				} else {
					Double highSalary = sortedSalaries.get(index);
					Double lowSalary = sortedSalaries.get(index + 1);
					return (1 - factor) * highSalary + factor * lowSalary;
				}
			}

			private Stream<Double> salariesStream() {
				return dataset.toMap().values().stream().flatMap(Collection<Double>::stream);
			}

			private int salariesCount() {
				return dataset.toMap().values().stream().mapToInt(Collection<Double>::size).sum();
			}
		};
	}
}
