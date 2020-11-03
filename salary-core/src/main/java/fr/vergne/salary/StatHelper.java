package fr.vergne.salary;

import static java.util.stream.Collectors.*;

import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;

class StatHelper {

	private final Collection<Double> salaries;

	public StatHelper(Collection<Double> salaries) {
		this.salaries = salaries;
	}

	double mean() {
		return salaries.stream().mapToDouble(salary -> salary).average().getAsDouble();
	}

	double Q1() {
		return estimateSalary(salaries, (double) (salaries.size() + 3) / 4.0);
	}

	double Q3() {
		return estimateSalary(salaries, (double) (3 * salaries.size() + 1) / 4.0);
	}

	private double estimateSalary(Collection<Double> salaries, double approximatedIndex) {
		int index = (int) Math.rint(approximatedIndex);
		List<Double> sortedSalaries = salaries.stream().sorted().limit(index + 2).collect(toList());
		double factor = approximatedIndex - index;
		if (factor == 0) {
			return sortedSalaries.get(index);
		} else {
			Double lowSalary = sortedSalaries.get(index);
			Double highSalary = sortedSalaries.get(index + 1);
			return (1 - factor) * lowSalary + factor * highSalary;
		}
	}

	static UnaryOperator<Double> normaliseInPercentOf(double target) {
		double denominator = target / 100;
		return value -> value / denominator;
	}
	
	static double standardDeviation(double Q1, double Q3) {
		return (Q3 - Q1) / 1.35;
	}
}
