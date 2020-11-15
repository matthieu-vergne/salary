package fr.vergne.salary.util;

public interface SuccessFailureObserver {
	void notifySuccess();

	void notifyFailure();
}
