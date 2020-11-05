package fr.vergne.salary.data;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface StatisticsDataset extends Dataset<Statistics> {
	static StatisticsDataset fromMap(Map<Profile, Statistics> data) {
		return () -> data;
	}

	default StatisticsDataset filterOnProfile(Profile profile) {
		return StatisticsDataset.fromMap(Dataset.filterMapOnProfile(this.toMap(), profile));
	}

	default StatisticsDataset splitProfiles() {
		return StatisticsDataset.fromMap(this.toMap().entrySet().stream()//
				.flatMap(entry -> {
					Profile aggregatedProfile = entry.getKey();
					Statistics statistics = entry.getValue();
					return Stream.of(aggregatedProfile)//
							.flatMap(Intern.splitExperienceInUnits())//
							.flatMap(Intern.splitSeniorityInUnits())//
							.map(unitProfile -> Map.entry(unitProfile, statistics));
				}).collect(Collectors.toMap(//
						Entry<Profile, Statistics>::getKey, //
						Entry<Profile, Statistics>::getValue)));
	}

	public interface Factor {
		double get(Profile profile, Statistics.Type statType);
	}

	default StatisticsDataset factor(Factor factor) {
		Map<Profile, Statistics> data = this.toMap();
		Map<Profile, Statistics> factoredData = data.keySet().stream()//
				.collect(Collectors.toMap(//
						profile -> profile, //
						profile -> data.get(profile).factor(statType -> factor.get(profile, statType))));
		return StatisticsDataset.fromMap(factoredData);
	}

	class Intern {
		private static Function<Profile, Stream<Profile>> splitSeniorityInUnits() {
			return profile -> IntStream.rangeClosed(profile.seniority().start(), profile.seniority().stop())//
					.mapToObj(year -> Period.create(year))//
					.map(instant -> Profile.create(instant, profile.experience()));
		}

		private static Function<Profile, Stream<Profile>> splitExperienceInUnits() {
			return profile -> IntStream.rangeClosed(profile.experience().start(), profile.experience().stop())//
					.mapToObj(year -> Period.create(year))//
					.map(instant -> Profile.create(profile.seniority(), instant));
		}
	}
}
