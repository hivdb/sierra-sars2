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
import java.util.Map;

import org.apache.commons.lang3.tuple.Triple;

import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.DRDB;
import edu.stanford.hivdb.sars2.SARS2;
import edu.stanford.hivdb.seqreads.SequenceReads;
import edu.stanford.hivdb.sequences.AlignedSequence;

public class DRDBDef {

	private static SARS2 sars2 = SARS2.getInstance();
	
	protected static MutationSet<SARS2> getMutationSetFromSource(Object src) {
		MutationSet<?> mutations;
		if (src instanceof AlignedSequence) {
			mutations = ((AlignedSequence<?>) src).getMutations(); 
		}
		else if (src instanceof SequenceReads) {
			mutations = ((SequenceReads<?>) src).getMutations();
		}
		else if (src instanceof Triple) {
			Object middle = ((Triple<?, ?, ?>) src).getMiddle();
			if (middle instanceof MutationSet) {
				mutations = (MutationSet<?>) middle;
			}
			else {
				throw new UnsupportedOperationException();
			}
		}
		else {
			throw new UnsupportedOperationException();
		}
		@SuppressWarnings("unchecked")
		MutationSet<SARS2> sars2Mutations = (MutationSet<SARS2>) mutations;
		return sars2Mutations;
	}
	
	public static DataFetcher<List<Map<String, Object>>> suscResultsForAntibodiesDataFetcher = env -> {
		DRDB drdb = sars2.getDRDBObj();
		MutationSet<SARS2> mutations = getMutationSetFromSource(env.getSource());
		return drdb.querySuscResultsForAntibodies(mutations);
	};
	
	public static DataFetcher<List<Map<String, ?>>> suscResultsForConvPlasmaDataFetcher = env -> {
		DRDB drdb = sars2.getDRDBObj();
		MutationSet<SARS2> mutations = getMutationSetFromSource(env.getSource());
		return drdb.querySuscResultsForConvPlasma(mutations);
	};

	public static DataFetcher<List<Map<String, ?>>> suscResultsForImmuPlasmaDataFetcher = env -> {
		DRDB drdb = sars2.getDRDBObj();
		MutationSet<SARS2> mutations = getMutationSetFromSource(env.getSource());
		return drdb.querySuscResultsForImmuPlasma(mutations);
	};
	
	private static GraphQLObjectType.Builder newSuscResultObject() {
		return newObject()
			.field(field -> field
				.type(GraphQLString)
				.name("refName")
				.description("Reference unique identifier provided by COVDB."))
			.field(field -> field
				.type(GraphQLString)
				.name("refDOI")
				.description("Reference DOI."))
			.field(field -> field
				.type(GraphQLString)
				.name("refURL")
				.description("Reference URL. Provided when DOI is unavailable."))
			.field(field -> field
				.type(GraphQLString)
				.name("rxName")
				.description("Treatment free text name."))
			.field(field -> field
				.type(GraphQLString)
				.name("controlStrainName")
				.description("The control virus strain of the susceptibility testing."))
			.field(field -> field
				.type(GraphQLString)
				.name("strainName")
				.description("The experimental virus strain of the susceptibility testing."))
			.field(field -> field
				.type(new GraphQLList(GraphQLString))
				.name("mutations")
				.description("Mutations contained by the target virus strain."))
			.field(field -> field
				.type(GraphQLString)
				.name("section")
				.description("Data source section(s) from the reference."))
			.field(field -> field
				.type(GraphQLInt)
				.name("ordinalNumber")
				.description("Tell apart results when multiple ones are available for the same `refName-rxName-controlStrainName-strainName` combinations."))
			.field(field -> field
				.type(GraphQLString)
				.name("foldCmp")
				.description("Indicate if the `fold` is precise or a range."))
			.field(field -> field
				.type(GraphQLFloat)
				.name("fold")
				.description("Fold change: defined as the IC50 (IC80/IC90 if IC50 is not avaiable) number of experimental virus strain divided by the control virus strain."))
			.field(field -> field
				.type(GraphQLString)
				.name("ineffective")
				.description("When value is provided, the treatment has no effect on control strain or experimental strain."))
			.field(field -> field
				.type(GraphQLString)
				.name("resistanceLevel")
				.description("Resistance level calculated by `fold` and `foldCmp`. TODO: add formulas."))
			.field(field -> field
				.type(GraphQLInt)
				.name("cumulativeCount")
				.description(
					"When `cumulativeCount` is greater than 1, this susceptibility data is aggregated from `cumulativeCount` experiments; " +
					"in this case, the `fold` is the median value (if `foldCmp` is \"=\" or \"~\"), or the range  (if `foldCmp` is \">\" or \"<\")."));
	}
	
	
	public static GraphQLObjectType oAntibodySuscResult = newSuscResultObject()
		.name("AntibodySuscResultObject")
		.description("Object for susceptibility result of an antibody")
		.field(field -> field
			.type(new GraphQLList(GraphQLString))
			.name("antibodies")
			.description("Antibody names."))
		.build();

	public static GraphQLObjectType oConvPlasmaSuscResult = newSuscResultObject()
		.name("ConvPlasmaSuscResultObject")
		.description("Object for susceptibility result of a convalescent plasma")
		.build();

	public static GraphQLObjectType oImmuPlasmaSuscResult = newSuscResultObject()
		.name("ImmuPlasmaSuscResultObject")
		.description("Object for susceptibility result of a vaccine-recipient plasma")
		.field(field -> field
			.type(GraphQLString)
			.name("vaccineName")
			.description("Vaccine name."))
		.build();

}
