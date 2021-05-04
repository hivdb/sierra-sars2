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

package edu.stanford.hivdb.graphql;

import graphql.schema.*;
import static graphql.Scalars.*;
import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLCodeRegistry.newCodeRegistry;
import static graphql.schema.FieldCoordinates.coordinates;

import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.mutations.MutationType;
import edu.stanford.hivdb.sequences.UnsequencedRegions;
import edu.stanford.hivdb.utilities.SimpleMemoizer;
import edu.stanford.hivdb.viruses.Gene;
import edu.stanford.hivdb.viruses.Virus;

import static edu.stanford.hivdb.graphql.GeneDef.oGene;
import static edu.stanford.hivdb.graphql.ConditionalCommentDef.oBoundComment;

public class MutationDef {

	private static DataFetcher<String> mutConsDataFetcher = env -> {
		Mutation<?> mutation = env.getSource();
		return mutation.getReference();
	};
	
	private static <T extends Virus<T>> Boolean isUnsequenced(DataFetchingEnvironment env) {
		Mutation<T> mutation = env.getSource();
		Gene<T> gene = mutation.getGene();
		Object src = env.getLocalContext();
		UnsequencedRegions<T> unseqRegions = UnsequencedRegionsDef.getUnsequencedRegionsFromSource(src, gene);
		if (unseqRegions == null) {
			return mutation.isUnsequenced();
		}
		else {
			return mutation.isUnsequenced(unseqRegions);
		}
	}

	private static DataFetcher<Boolean> isUnsequencedFetcher = env -> {
		return isUnsequenced(env);
	};

	public static SimpleMemoizer<GraphQLEnumType> oMutationType = new SimpleMemoizer<>(
		name -> {
			Virus<?> virusIns = Virus.getInstance(name);
			GraphQLEnumType.Builder mutationTypeBuilder = GraphQLEnumType.newEnum()
				.name("MutationType")
				.description("Mutation type.");
			for (MutationType<?> mutType : virusIns.getMutationTypes()) {
				mutationTypeBuilder.value(mutType.toString(), mutType);
			}
			return mutationTypeBuilder.build();
		}
	);
	
	public static GraphQLObjectType oAAReads = newObject()
		.name("AAReads")
		.field(field -> field
			.type(GraphQLChar)
			.name("aminoAcid")
			.description("A single amino acid."))
		.field(field -> field
			.type(GraphQLFloat)
			.name("percent")
			.description("Percent (range: 0 - 100) of this amino acid in the mutation."))
		.field(field -> field
			.type(GraphQLLong)
			.name("numReads")
			.description(
				"The number of reads for this amino acid in the mutation."))
		.build();

	
	public static GraphQLCodeRegistry mutationCodeRegistry = newCodeRegistry()
		.dataFetcher(
			coordinates("Mutation", "consensus"),
			mutConsDataFetcher
		)
		.dataFetcher(
			coordinates("Mutation", "isUnsequenced"),
			isUnsequencedFetcher
		)
		.build();
	
	public static SimpleMemoizer<GraphQLObjectType> oGenePosition = new SimpleMemoizer<>(
		name -> (
				newObject()
				.name("GenePosition")
				.field(field -> field
					.type(oGene.get(name))
					.name("gene")
					.description("Gene."))
				.field(field -> field
					.type(GraphQLInt)
					.name("position")
					.description("Position."))
				.build()
		)
	);
	
	public static SimpleMemoizer<GraphQLObjectType> oMutation = new SimpleMemoizer<>(
		name -> (
			newObject()
			.name("Mutation")
			.field(field -> field
				.type(oGene.get(name))
				.name("gene")
				.description("Mutation gene."))
			.field(field -> field
				.type(GraphQLString)
				.name("reference")
				.description(
					"Amino acid reference at this gene sequence position."))
			.field(field -> field
				.type(GraphQLString)
				.name("consensus")
				.deprecate("Use field `reference` instead."))
			.field(field -> field
				.type(GraphQLInt)
				.name("position")
				.description("Position of the mutation."))
			.field(field -> field
				.type(GraphQLString)
				.name("displayAAs")
				.description(
					"The mutated AA(s) with possibly inserted AA(s)."))
			.field(field -> field
				.type(GraphQLString)
				.name("AAs")
				.description(
					"The mutated AA(s) with possibly inserted AA(s). Highly ambiguous mixture is not replaced to X."))
			.field(field -> field
				.type(new GraphQLList(GraphQLString))
				.name("displayAAChars")
				.description("A list of AAs."))
			.field(field -> field
				.type(new GraphQLList(GraphQLString))
				.name("AAChars")
				.description("A list of AAs. Highly ambiguous mixture is not replaced to X."))
			.field(field -> field
				.type(GraphQLString)
				.name("triplet")
				.description(
					"The mutated codon when the mutation is extracting from " +
					"an aligned sequence."))
			.field(field -> field
				.type(GraphQLString)
				.name("insertedNAs")
				.description(
					"The inserted codon(s) when the mutation is extracting from " +
					"an aligned sequence."))
			.field(field -> field
				.type(GraphQLBoolean)
				.name("isInsertion")
				.description("The mutation is an insertion or not."))
			.field(field -> field
				.type(GraphQLBoolean)
				.name("isDeletion")
				.description("The mutation is a deletion or not."))
			.field(field -> field
				.type(GraphQLBoolean)
				.name("isIndel")
				.description("The mutation is an insertion/deletion, or not."))
			.field(field -> field
				.type(GraphQLBoolean)
				.name("isAmbiguous")
				.description("The mutation is a highly ambiguous mutation or not."))
			.field(field -> field
				.type(GraphQLBoolean)
				.name("isApobecMutation")
				.description(
					"The mutation is a signature APOBEC-mediated G-to-A " +
					"hypermutation or not."))
			.field(field -> field
				.type(GraphQLBoolean)
				.name("isApobecDRM")
				.description(
					"The mutation is a drug resistance mutation (DRM) might " +
					"be caused by APOBEC-mediated G-to-A hypermutation or not."))
			.field(field -> field
				.type(GraphQLBoolean)
				.name("isUnsequenced")
				.description(
					"If the mutation is from unsequenced region."))
			.field(field -> field
				.type(GraphQLBoolean)
				.name("isDRM")
				.description(
					"If the mutation is a drug resistance mutation (DRM) or not."))
			.field(field -> field
				.type(GraphQLBoolean)
				.name("hasStop")
				.description(
					"The mutation contains stop codon or not."))
			.field(field -> field
				.type(GraphQLBoolean)
				.name("isUnusual")
				.description(
					"The mutation is a low prevalence (unusual) mutation or not."))
			.field(field -> field
				.type(GraphQLBoolean)
				.name("isSDRM")
				.description(
					"The mutation is a Surveillance Drug Resistance Mutation (SDRM) or not."))
			.field(field -> field
				.type(new GraphQLList(oMutationType.get(name)))
				.name("types")
				.description(
					"Ordered list of mutation type(s). List size can be " +
					"larger than 1 when the mutation is a mixture."))
			.field(field -> field
				.type(oMutationType.get(name))
				.name("primaryType")
				.description("Primary type of the mutation."))
			.field(field -> field
				.type(new GraphQLList(oBoundComment.get(name)))
				.name("comments")
				.description("Mutation comments."))
			.field(field -> field
				.type(GraphQLString)
				.name("text")
				.description("Formatted text of the mutation (without gene)."))
			.field(field -> field
				.type(GraphQLString)
				.name("shortText")
				.description(
					"Formatted short text of the mutation (without gene)."))
			.field(field -> field
				.type(GraphQLLong)
				.name("totalReads")
				.description(
					"Total reads at this position (only available for SequenceReadsAnalysis)"))
			.field(field -> field
				.type(new GraphQLList(oAAReads))
				.name("allAAReads")
				.description(
					"A list of read count / prevalence for each amino acid of this mutation."))
			.build()
		)
	);

}
