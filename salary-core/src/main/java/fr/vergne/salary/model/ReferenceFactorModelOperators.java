package fr.vergne.salary.model;

import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

import fr.vergne.salary.data.StatisticsDataset;

public class ReferenceFactorModelOperators {
	private final Random rand;
	private final StatisticsDataset referenceStatistics;

	public ReferenceFactorModelOperators(Random rand, StatisticsDataset referenceStatistics) {
		this.rand = rand;
		this.referenceStatistics = referenceStatistics;
	}

	public Supplier<Double> parameterGenerator() {
		return () -> 1.0;
	}

	public Function<Double, Double> parameterAdapter() {
		return factor -> factor + (rand.nextDouble() - 0.5);
	}

	public Function<Double, Model<Double>> modelFactory() {
		return factor -> {
			String name = "Ref x " + factor;
			StatisticsDataset dataset = referenceStatistics//
					.splitProfiles()//
					.factor((profile, statType) -> factor);
			return Model.create(factor, name, dataset);
		};
	}

}
