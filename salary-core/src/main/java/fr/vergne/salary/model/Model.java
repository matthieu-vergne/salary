package fr.vergne.salary.model;

import fr.vergne.salary.data.StatisticsDataset;

public interface Model<P> {
	P parameter();

	StatisticsDataset dataset();

	static <P> Model<P> create(P parameter, String name, StatisticsDataset dataset) {
		return new Model<P>() {

			@Override
			public P parameter() {
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
