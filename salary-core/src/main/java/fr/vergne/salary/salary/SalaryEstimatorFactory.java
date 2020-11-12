package fr.vergne.salary.salary;

import fr.vergne.salary.data.StatisticsDataset;

public class SalaryEstimatorFactory {
	private final long seed;

	public SalaryEstimatorFactory(long seed) {
		this.seed = seed;
	}

	public SalaryEstimator createFromStatistics(StatisticsDataset dataset) {
		RandomFactory rand = new RandomFactory(seed);
		return profile -> rand.gaussian(dataset.toMap().get(profile));
	}
}
