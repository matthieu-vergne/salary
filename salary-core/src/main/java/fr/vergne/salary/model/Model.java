package fr.vergne.salary.model;

import fr.vergne.salary.data.StatisticsDataset;

public interface Model<Parameter> {
	Parameter parameter();

	StatisticsDataset dataset();

	static Model<Double> create(Double parameter, String name, StatisticsDataset dataset) {
		return new Model<Double>() {

			@Override
			public Double parameter() {
				return parameter;
			}

			@Override
			public StatisticsDataset dataset() {
				return dataset;
			}
			
			@Override
			public String toString() {
				return name;
			}
		};
	}
}
