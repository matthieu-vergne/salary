package fr.vergne.salary.report;

import java.util.function.Function;

import fr.vergne.salary.data.SalariesDataset;
import fr.vergne.salary.data.StatisticsDataset;
import fr.vergne.salary.evaluation.ErrorBounds;
import fr.vergne.salary.model.Model;

public interface GraphicalReport {
	void setReferenceStatistics(StatisticsDataset reference);

	void setModelStatistics(Model<?> model);

	void setModelBasedSalaries(SalariesDataset salaries);

	void setSalariesBasedStatistics(StatisticsDataset modelOnReferenceProfiles);

	static GraphicalReport create(//
			String title, //
			int chartWidth, int chartHeight, //
			int transitionWidth, //
			Function<Double, String> scoreFormatter, //
			int salariesPerProfile) {
		return new JFreeChartReport(title, chartWidth, chartHeight, transitionWidth, scoreFormatter,
				salariesPerProfile);
	}

	void setErrorBounds(ErrorBounds q1, ErrorBounds mean, ErrorBounds q3);

	void setScore(double score);

}
