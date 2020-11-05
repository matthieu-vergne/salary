package fr.vergne.salary;

import static java.util.stream.Collectors.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import fr.vergne.salary.Main.Profile;
import fr.vergne.salary.Main.Statistics;

class SalariesStatistics implements Statistics {

	private final Map<Profile, Collection<Double>> salaries;

	private SalariesStatistics(Map<Profile, Collection<Double>> salaries) {
		this.salaries = salaries;
	}

	static SalariesStatistics fromSalariesMap(Map<Profile, Collection<Double>> salaries) {
		return new SalariesStatistics(salaries);
	}

	SalariesStatistics filterOnProfile(Profile profile) {
		Map<Profile, Collection<Double>> filteredSalaries = salaries.entrySet().stream()//
				.filter(entry -> profile.contains(entry.getKey()))//
				.collect(toMap(//
						Entry<Profile, Collection<Double>>::getKey, //
						Entry<Profile, Collection<Double>>::getValue));
		return new SalariesStatistics(filteredSalaries);
	}

	@Override
	public double Q1() {
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
	public double Q3() {
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
		return salaries.values().stream().flatMap(Collection<Double>::stream);
	}

	private int salariesCount() {
		return salaries.values().stream().mapToInt(Collection<Double>::size).sum();
	}
}
