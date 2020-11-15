package fr.vergne.salary.report;

import java.util.function.Function;

import fr.vergne.salary.data.SalariesDataset;
import fr.vergne.salary.data.StatisticsDataset;
import fr.vergne.salary.evaluation.ErrorBounds;
import fr.vergne.salary.model.Model;

public interface GraphicalReport<P, S> {
	void setReferenceStatistics(StatisticsDataset reference);

	void setModelStatistics(Model<P> model);

	void setModelBasedSalaries(SalariesDataset salaries);

	void setSalariesBasedStatistics(StatisticsDataset modelOnReferenceProfiles);

	void setErrorBounds(ErrorBounds q1, ErrorBounds mean, ErrorBounds q3);

	void setScore(S score);

	static <P, S> GraphicalReport<P, S> create(//
			String title, //
			int chartWidth, int chartHeight, //
			int transitionWidth, //
			Function<P, String> parametersFormatter, //
			Function<S, String> scoreFormatter, //
			int salariesPerProfile) {
		return new JFreeChartReport<P, S>(title, chartWidth, chartHeight, transitionWidth, parametersFormatter,
				scoreFormatter, salariesPerProfile);
	}

}
