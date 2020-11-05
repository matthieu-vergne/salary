package fr.vergne.salary.model;

import fr.vergne.salary.data.StatisticsDataset;

public class ModelFactory {
	private final RandomFactory rand;

	public ModelFactory(RandomFactory rand) {
		this.rand = rand;
	}

	public Model createDataBasedModel(StatisticsDataset dataset) {
		return profile -> rand.gaussian(dataset.toMap().get(profile));
	}
}
