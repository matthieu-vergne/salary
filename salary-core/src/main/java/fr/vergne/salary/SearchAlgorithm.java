package fr.vergne.salary;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Supplier;

import fr.vergne.salary.model.Model;

public interface SearchAlgorithm {

	void iterate();

	interface Solution<P, S> {
		Model<P> model();

		S score();

		static <P, S> Solution<P, S> create(Model<P> model, S score) {
			return new Solution<P, S>() {
				@Override
				public Model<P> model() {
					return model;
				}

				@Override
				public S score() {
					return score;
				}
			};
		}
	}

	public interface IterationListener<P, S> {
		void startIteration();

		void parameterGenerated(P parameter);
		
		void modelGenerated(Model<P> model);
		
		void modelScored(Model<P> model, S score);
		
		void bestModelSelected(Model<P> model);
	}

	static <P, S> SearchAlgorithm createDownHill(//
			Supplier<P> parameterGenerator, Function<P, P> parameterAdapter, //
			Function<P, Model<P>> modelFactory, //
			Function<Model<P>, S> modelEvaluator, //
			Comparator<S> scoreComparator, Function<S, String> scoreFormatter, //
			IterationListener<P, S> listener) {
		return new SearchAlgorithm() {
			Solution<P, S> best = null;
			Comparator<Solution<P, S>> solutionComparator = Comparator.comparing(Solution::score, scoreComparator);

			@Override
			public void iterate() {
				listener.startIteration();
				P parameter;
				if (best == null) {
					parameter = parameterGenerator.get();
				} else {
					parameter = parameterAdapter.apply(best.model().parameter());
				}
				listener.parameterGenerated(parameter);

				Model<P> model = modelFactory.apply(parameter);
				listener.modelGenerated(model);

				S score = modelEvaluator.apply(model);
				listener.modelScored(model, score);

				Solution<P, S> candidate = Solution.create(model, score);
				best = best == null ? candidate //
						: solutionComparator.compare(candidate, best) > 0 ? candidate : best;
				listener.bestModelSelected(best.model());
			}
		};
	}
}
