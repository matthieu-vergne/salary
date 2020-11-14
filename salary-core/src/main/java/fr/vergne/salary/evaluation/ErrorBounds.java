package fr.vergne.salary.evaluation;

public class ErrorBounds {
	private final Double min;
	private final Double max;

	private ErrorBounds(Double min, Double max) {
		this.min = min;
		this.max = max;
	}

	public double min() {
		return min;
	}

	public double max() {
		return max;
	}

	public static ErrorBounds undefined() {
		return new ErrorBounds(null, null);
	}

	public ErrorBounds extendsTo(double error) {
		return new ErrorBounds(//
				min == null ? error : Math.min(min, error), //
				max == null ? error : Math.max(max, error));
	}

	@Override
	public String toString() {
		return String.format("[%.2f%% ; %.2f%%]", 100 * min, 100 * max);
	}

}
