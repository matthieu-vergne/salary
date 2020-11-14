package fr.vergne.salary.salary;

import fr.vergne.salary.data.StatisticsDataset;

public class SalaryEstimatorFactory {
	private final long seed;

	public SalaryEstimatorFactory(long seed) {
		this.seed = seed;
	}

	public SalaryEstimator createFromStatistics(StatisticsDataset dataset) {
		RandomFactory rand = new RandomFactory(seed);
		/**
		 * FIXME Generate salaries with non-symmetric (around mean) distribution
		 * 
		 * A symmetric distribution around the mean, like the normal distribution,
		 * imposes to keep the mean between Q1 and Q3. Real life is of course different,
		 * so we want at least to allow slight differences to produce better
		 * approximations. To do so, we need to use distributions which do not assume
		 * mean-centered values.
		 * 
		 * https://en.wikipedia.org/wiki/Skew_normal_distribution
		 */
		return profile -> rand.gaussian(dataset.toMap().get(profile));
	}
}
