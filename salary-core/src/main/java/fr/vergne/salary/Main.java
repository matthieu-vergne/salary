package fr.vergne.salary;

import java.util.Map;

import fr.vergne.salary.chart.GraphicalReport;
import fr.vergne.salary.data.Period;
import fr.vergne.salary.data.Profile;
import fr.vergne.salary.data.Statistics;
import fr.vergne.salary.data.StatisticsDataset;
import fr.vergne.salary.evaluation.ModelEvaluation;
import fr.vergne.salary.evaluation.ModelEvaluator;

public class Main {

	public static void main(String[] args) throws InterruptedException {

		String title = "Salary Analysis Charts";
		int chartWidth = 500;
		int chartHeight = 500;
		int transitionWidth = 50;
		GraphicalReport report = GraphicalReport.create(title, chartWidth, chartHeight, transitionWidth);

		System.out.println("Create reference statistics");
		StatisticsDataset referenceStatistics = createReferenceDataset();
		int salariesPerProfileInEvaluation = 10000;
		ModelEvaluator modelEvaluator = ModelEvaluator.create(referenceStatistics, salariesPerProfileInEvaluation);
		report.setReferenceStatistics(referenceStatistics);

		System.out.println("Create model statistics");
		int factor = 1;
		StatisticsDataset modelStatistics = referenceStatistics//
				.splitProfiles()//
				.factor((profile, statType) -> factor);
		report.setModelStatistics(modelStatistics);

		System.out.println("Evaluate model statistics");
		// TODO Evaluation should return Comparable
		// TODO If needed, split model evaluation & report data
		ModelEvaluation evaluation = modelEvaluator.evaluate(modelStatistics);
		int salariesPerProfileInReport = 10;
		report.setModelEvaluation(evaluation, salariesPerProfileInReport);

		System.out.println("Done");

		// TODO Downhill algorithm to converge factor from 0 to 1
		// TODO Increase to 1 parameter per type
		// TODO Increase to 1 parameter per profile & type
		// TODO GA to converge parameters to reference
		// TODO Parametered model on linear curves
		// TODO Parametered model on exponential curves
		// TODO Parametered model on logarithmic curves
	}

	private static StatisticsDataset createReferenceDataset() {
		Period sen_0_1 = Period.create(0, 1);
		Period sen_2_5 = Period.create(2, 5);

		Period exp_1____ = Period.create(1);
		Period exp_2_5__ = Period.create(2, 5);
		Period exp_6_9__ = Period.create(6, 9);
		Period exp_10_14 = Period.create(10, 14);

		return StatisticsDataset.fromMap(Map.of(//
				Profile.create(sen_0_1, exp_1____), Statistics.create(29.731, 34.062, 38.942), //
				Profile.create(sen_0_1, exp_2_5__), Statistics.create(31.465, 35.797, 40.640), //
				Profile.create(sen_0_1, exp_6_9__), Statistics.create(35.429, 40.476, 46.144), //
				Profile.create(sen_0_1, exp_10_14), Statistics.create(39.964, 45.670, 52.082), //

				Profile.create(sen_2_5, exp_1____), Statistics.create(29.837, 34.125, 38.948), //
				Profile.create(sen_2_5, exp_2_5__), Statistics.create(31.577, 35.863, 40.646), //
				Profile.create(sen_2_5, exp_6_9__), Statistics.create(35.555, 40.551, 46.152), //
				Profile.create(sen_2_5, exp_10_14), Statistics.create(40.107, 45.755, 52.090)));
	}
}
