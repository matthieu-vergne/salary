package fr.vergne.salary.model;

import static java.util.stream.Collectors.*;

import java.util.Set;
import java.util.stream.IntStream;

import fr.vergne.salary.data.Profile;
import fr.vergne.salary.data.SalariesDataset;

public interface Model {
	double estimateSalary(Profile profile);

	default SalariesDataset createSalariesDataset(Set<Profile> profiles, int salariesPerProfile) {
		return SalariesDataset.fromMap(profiles.stream()//
				.sorted(Profile.bySeniorityThenExperience())// Order to generate same results with same random seed
				.collect(toMap(//
						profile -> profile, //
						profile -> IntStream.range(0, salariesPerProfile)//
								.mapToObj(i -> estimateSalary(profile))//
								.collect(toList()))));
	}
}
