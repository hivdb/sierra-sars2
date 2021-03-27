/*

    Copyright (C) 2017 Stanford HIVDB team

    Sierra is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Sierra is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package edu.stanford.hivdb.sars2.graphql;

import graphql.schema.*;
import static graphql.Scalars.*;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.List;
import java.util.stream.Collectors;

import edu.stanford.hivdb.graphql.MutationDef;
import edu.stanford.hivdb.graphql.MutationSetDef;
import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.AntibodySuscResult;
import edu.stanford.hivdb.sars2.SARS2;
import edu.stanford.hivdb.sars2.SuscSummary;
import edu.stanford.hivdb.sars2.SuscSummary.SuscSummaryByMutationSet;

public class AntibodySuscResultDef {

	public static DataFetcher<List<SuscSummaryByMutationSet>> antibodySuscSummaryFetcher = env -> {
		String drdbVersion = env.getArgument("drdbVersion");
		MutationSet<SARS2> mutations = DRDBDef.getMutationSetFromSource(env.getSource());
		return SuscSummary.getAntibodySuscSummaryItems(drdbVersion, mutations);
	};
	
	public static DataFetcher<List<AntibodySuscResult>> antibodySuscResultListFetcher = env -> {
		String drdbVersion = env.getArgument("drdbVersion");
		MutationSet<SARS2> mutations = DRDBDef.getMutationSetFromSource(env.getSource());
		Boolean includeAll = env.getArgument("includeAll");
		List<AntibodySuscResult> results = AntibodySuscResult.query(drdbVersion, mutations);
		if (!includeAll) {
			results = results.stream()
				.filter(r -> (
					r.getAntibodies().stream().allMatch(
						ab -> (
							ab.getAvailability() != null ||
							ab.getAntibodyClass() != null		
						)
					)
				))
				.collect(Collectors.toList());
		}
		results.sort((a, b) -> {
			int aHit = a.getNumHitMutations();
			int bHit = b.getNumHitMutations();
			if (aHit == bHit) {
				int aMiss = a.getNumMissMutations();
				int bMiss = b.getNumMissMutations();
				return aMiss - bMiss;
			}
			else {
				return bHit - aHit;  // descending order
			}
		});
		return results;
	};

	public static GraphQLObjectType oAntibodySuscResult = newObject()
		.name("AntibodySuscResultObject")
		.description("Object for susceptibility result of an antibody")
		.field(field -> field
			.type(ArticleDef.oArticle)
			.name("reference")
			.description("Reference unique identifier provided by COVDB."))
		.field(field -> field
			.type(GraphQLString)
			.name("rxName")
			.description("Treatment free text name."))
		.field(field -> field
			.type(new GraphQLList(AntibodyDef.oAntibody))
			.name("antibodies")
			.description("Antibodies used in this treatment."))
		.field(field -> field
			.type(VirusVariantDef.oVirusVariant)
			.name("controlVirusVariant")
			.description("The control virus varaint of the susceptibility testing."))
		.field(field -> field
			.type(VirusVariantDef.oVirusVariant)
			.name("virusVariant")
			.description("The experimental virus variant of the susceptibility testing."))
		.field(field -> MutationSetDef.newMutationSet("SARS2", field, "hitMutations")
			.description("Mutations matched the query mutation set."))
		.field(field -> MutationSetDef.newMutationSet("SARS2", field, "missMutations")
				.description("Mutations mismatched the query mutation set."))
		.field(field -> field
			.type(new GraphQLList(MutationDef.oGenePosition.get("SARS2")))
			.name("hitPositions")
			.description("Positions matched the query mutation set."))
		.field(field -> field
			.type(new GraphQLList(MutationDef.oGenePosition.get("SARS2")))
			.name("missPositions")
			.description("Positions mismatched the query mutation set."))	
		.field(field -> field
			.type(GraphQLInt)
			.name("numHitMutations")
			.description("Number of mutations that matched the query mutation set."))
		.field(field -> field
			.type(GraphQLInt)
			.name("numMissMutations")
			.description("Number of mutations that mismatched the query mutation set."))
		.field(field -> field
			.type(GraphQLInt)
			.name("numHitPositions")
			.description("Number of positions that matched the query mutation set."))
		.field(field -> field
			.type(GraphQLInt)
			.name("numMissPositions")
			.description("Number of positions that mismatched the query mutation set."))
		.field(field -> field
			.type(GraphQLString)
			.name("assay")
			.description("Indicate if the test was against psuedovirus or authentic virus."))
		.field(field -> field
			.type(GraphQLString)
			.name("section")
			.description("Data source section(s) from the reference."))
		.field(field -> field
			.type(GraphQLInt)
			.name("ordinalNumber")
			.description("Tell apart results when multiple ones are available for the same `refName-rxName-controlVariantName-variantName` combinations."))
		.field(field -> field
			.type(GraphQLString)
			.name("foldCmp")
			.description("Indicate if the `fold` is precise or a range."))
		.field(field -> field
			.type(GraphQLFloat)
			.name("fold")
			.description("Fold change: defined as the IC50 (IC80/IC90 if IC50 is not avaiable) number of experimental virus variant divided by the control virus variant."))
		.field(field -> field
			.type(GraphQLString)
			.name("ineffective")
			.description("When value is provided, the treatment has no effect on control variant or experimental variant."))
		.field(field -> field
			.type(GraphQLString)
			.name("resistanceLevel")
			.description("Resistance level calculated by `fold` and `foldCmp`. TODO: add formulas."))
		.field(field -> field
			.type(GraphQLInt)
			.name("cumulativeCount")
			.description(
				"When `cumulativeCount` is greater than 1, this susceptibility data is aggregated from `cumulativeCount` experiments; " +
				"in this case, the `fold` is the median value (if `foldCmp` is \"=\" or \"~\"), or the range  (if `foldCmp` is \">\" or \"<\")."))
		.build();
	
	public static GraphQLObjectType oSuscSummaryByRLevel = newObject()
		.name("SuscSummaryByRLevel")
		.description("Susceptibility summary grouped by resistance level")
		.field(field -> field
			.name("resistanceLevel")
			.type(GraphQLString)
			.description("Group resistance level"))
		.field(field -> field
			.name("cumulativeCount")
			.type(GraphQLInt)
			.description("Total number of experiments in this group."))
		.field(field -> field
			.name("items")
			.type(new GraphQLList(oAntibodySuscResult))
			.description("Susc results"))
		.build();
	
	public static GraphQLObjectType oSuscSummaryByAntibody = newObject()
		.name("SuscSummaryByAntibody")
		.description("Susceptibility summary grouped by antibody")
		.field(field -> field
			.name("antibodies")
			.type(new GraphQLList(AntibodyDef.oAntibody))
			.description("Group antibody/antibody combination"))
		.field(field -> field
			.name("items")
			.type(new GraphQLList(oSuscSummaryByRLevel))
			.description("Susc summary grouped by resistance level"))
		.build();
	
	public static GraphQLObjectType oSuscSummaryByAntibodyClass = newObject()
		.name("SuscSummaryByAntibodyClass")
		.description("Susceptibility summary grouped by antibody class")
		.field(field -> field
			.name("antibodyClass")
			.type(GraphQLString)
			.description("Group antibody class"))
		.field(field -> field
			.name("items")
			.type(new GraphQLList(oSuscSummaryByRLevel))
			.description("Susc summary grouped by resistance level"))
		.build();
	
	public static GraphQLObjectType oSuscSummaryByMutationSet = newObject()
		.name("SuscSummaryByMutationSet")
		.description("Susceptibility summary grouped by mutations")
		.field(field -> MutationSetDef.newMutationSet("SARS2", field, "mutations"))
		.field(field -> MutationSetDef.newMutationSet("SARS2", field, "hitMutations"))
		.field(field -> field
			.type(new GraphQLList(MutationDef.oGenePosition.get("SARS2")))
			.name("hitPositions")
			.description("Positions matched the query mutation set."))
		.field(field -> field
			.name("itemsByAntibody")
			.type(new GraphQLList(oSuscSummaryByAntibody))
			.description("Susc summary grouped by antibody."))
		.field(field -> field
			.name("itemsByAntibodyClass")
			.type(new GraphQLList(oSuscSummaryByAntibodyClass))
			.description("Susc summary grouped by antibody class."))
		.build();

}
