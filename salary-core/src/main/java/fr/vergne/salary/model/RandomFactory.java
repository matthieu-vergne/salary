package fr.vergne.salary.model;

import fr.vergne.salary.data.Statistics;

public class RandomFactory {
	private final java.util.Random rand;

	public RandomFactory(long seed) {
		this.rand = new java.util.Random(seed);
	}

	Double gaussian(Statistics stat) {
		return gaussian(stat.mean(), stat.Q1(), stat.Q3());
	}

	Double gaussian(double mean, double Q1, double Q3) {
		double μ = mean;
		double σ = standardDeviation(Q1, Q3);
		return gaussian(μ, σ);
	}

	Double gaussian(double μ, double σ) {
		double X = rand.nextGaussian();
		return σ * X + μ;
	}

	static double standardDeviation(double Q1, double Q3) {
		return (Q3 - Q1) / 1.35;
	}
}
