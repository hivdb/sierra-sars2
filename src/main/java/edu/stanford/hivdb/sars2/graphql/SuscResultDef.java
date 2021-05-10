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

import edu.stanford.hivdb.graphql.MutationSetDef;
import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;
import edu.stanford.hivdb.sars2.drdb.SuscSummary;

import static edu.stanford.hivdb.graphql.DescriptiveStatisticsDef.*;

public class SuscResultDef {

	public static DataFetcher<SuscSummary> antibodySuscSummaryFetcher = env -> {
		String drdbVersion = env.getArgument("drdbVersion");
		MutationSet<SARS2> mutations = MutationSetDef.getMutationSetFromSource(env.getSource());
		return SuscSummary.queryAntibodySuscSummary(drdbVersion, mutations);
	};

	public static DataFetcher<SuscSummary> convPlasmaSuscSummaryFetcher = env -> {
		String drdbVersion = env.getArgument("drdbVersion");
		MutationSet<SARS2> mutations = MutationSetDef.getMutationSetFromSource(env.getSource());
		return SuscSummary.queryConvPlasmaSuscSummary(drdbVersion, mutations);
	};

	public static DataFetcher<SuscSummary> vaccPlasmaSuscSummaryFetcher = env -> {
		String drdbVersion = env.getArgument("drdbVersion");
		MutationSet<SARS2> mutations = MutationSetDef.getMutationSetFromSource(env.getSource());
		return SuscSummary.queryVaccPlasmaSuscSummary(drdbVersion, mutations);
	};

	public static GraphQLObjectType oSuscResult = newObject()
		.name("SuscResultObject")
		.description("Object for general susceptibility result")
		.field(field -> field
			.type(ArticleDef.oArticle)
			.name("reference")
			.description("Data source reference."))
		.field(field -> field
			.type(GraphQLString)
			.name("rxName")
			.description("Treatment free text name."))
		.field(field -> field
			.type(VirusVariantDef.oVirusVariant)
			.name("controlVirusVariant")
			.description("The control virus varaint of the susceptibility testing."))
		.field(field -> field
			.type(VirusVariantDef.oVirusVariant)
			.name("virusVariant")
			.description("The experimental virus variant of the susceptibility testing."))
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
	
	public static GraphQLObjectType oSuscSummary = newObject()
		.name("SuscSummary")
		.description("Root susceptibility summary.")
		.field(field -> field
			.name("lastUpdate")
			.type(GraphQLString)
			.description("Last update of the database."))
		.field(field -> field
			.name("antibodies")
			.type(new GraphQLList(AntibodyDef.oAntibody))
			.description("Group antibody/antibody combination (only available for `itemsByAntibodies`)"))
		.field(field -> field
			.name("antibodyClass")
			.type(GraphQLString)
			.description("Group antibody class (only available for `itemsByAntibodyClass`)"))
		.field(field -> field
			.name("resistanceLevel")
			.type(GraphQLString)
			.description("Resistance level (only available for `itemsByResistLevel`)"))
		.field(field -> field
			.name("vaccineName")
			.type(GraphQLString)
			.description("Vaccine name (only available for `itemsByVaccine`)"))
		.field(field -> field
			.name("vaccinePriority")
			.type(GraphQLInt)
			.description("Vaccine priority (only available for `itemsByVaccine`)"))
		.field(field -> field
			.name("vaccineType")
			.type(GraphQLString)
			.description("Vaccine type (only available for `itemsByVaccine`)"))
		.field(field -> MutationSetDef.newMutationSet("SARS2", field, "mutations")
			.description("Variant mutations (only available for `itemsByMutations`)"))
		.field(field -> field
			.type(new GraphQLList(VirusVariantDef.oVirusVariant))
			.name("hitVariants")
			.description("Virus variants matched the query mutation set (only available for `itemsByMutations`)."))
		.field(field -> field
			.type(GraphQLString)
			.name("variantMatchType")
			.description("Type of how perfectly the query mutations matches variant mutations. Valid value: `EQUAL`, `SUPERSET`, `SUBSET`, and `OVERLAP`."))
		.field(field -> field
			.type(GraphQLInt)
			.name("numVariantOnlyMutations")
			.description("Number of mutations that are only appeared in variant mutations. D614G and ranged deletions are properly handled."))
		.field(field -> field
			.type(GraphQLInt)
			.name("numQueryOnlyMutations")
			.description("Number of mutations that are only appeared in query mutations. D614G and ranged deletions are properly handled."))
		.field(field -> field
			.type(new GraphQLList(ArticleDef.oArticle))
			.name("references")
			.description("Data source references."))
		.field(field -> field
			.name("cumulativeCount")
			.type(GraphQLInt)
			.description("Total number of experiments in this group."))
		.field(field -> field
			.name("cumulativeFold")
			.type(oDescriptiveStatistics)
			.description("Descriptive statistics of cumulative fold changes."))
		.field(field -> field
			.name("items")
			.type(new GraphQLList(oSuscResult))
			.description("List of all susc results"))
		.field(field -> field
			.name("itemsByAntibody")
			.type(new GraphQLList(GraphQLTypeReference.typeRef("SuscSummary")))
			.description("Susceptibility summary by antibody."))
		.field(field -> field
			.name("itemsByAntibodyClass")
			.type(new GraphQLList(GraphQLTypeReference.typeRef("SuscSummary")))
			.description("Susceptibility summary by antibody class."))
		.field(field -> field
			.name("itemsByResistLevel")
			.type(new GraphQLList(GraphQLTypeReference.typeRef("SuscSummary")))
			.description("Susceptibility summary by resistance level."))
		.field(field -> field
			.name("itemsByVaccine")
			.type(new GraphQLList(GraphQLTypeReference.typeRef("SuscSummary")))
			.description("Susceptibility summary by vaccine."))
		.field(field -> field
			.name("itemsByMutations")
			.type(new GraphQLList(GraphQLTypeReference.typeRef("SuscSummary")))
			.description("Susceptibility summary by key mutations."))
		.build();
}
