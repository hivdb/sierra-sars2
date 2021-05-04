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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.stanford.hivdb.graphql.GeneDef;
import edu.stanford.hivdb.graphql.MutationSetDef;
import edu.stanford.hivdb.graphql.UnsequencedRegionsDef;
import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.sars2.SARS2;
import edu.stanford.hivdb.sars2.SARS2MutationComment;
import edu.stanford.hivdb.sars2.SARS2MutationComment.SARS2BoundMutationComment;
import edu.stanford.hivdb.sequences.UnsequencedRegions;
import edu.stanford.hivdb.viruses.Gene;


public class SARS2MutationCommentDef {
	
	public static DataFetcher<List<SARS2BoundMutationComment>> boundMutationCommentsFetcher = env -> {
		SARS2 sars2 = SARS2.getInstance(); 
		String cmtVersion = env.getArgument("cmtVersion");
		Object src = env.getSource();
		MutationSet<SARS2> mutations = MutationSetDef.getMutationSetFromSource(src);
		Set<Mutation<SARS2>> filteredMuts = new LinkedHashSet<>();
		for (Gene<SARS2> gene : sars2.getMainStrain().getGenes()) {
			UnsequencedRegions<SARS2> unseqRegions = UnsequencedRegionsDef.getUnsequencedRegionsFromSource(src, gene);
			filteredMuts.addAll(
				mutations.getGeneMutationsNoSplit(gene)
				.filterByNoSplit(mut -> !mut.isUnsequenced(unseqRegions))
			);
		}
		return SARS2MutationComment.query(cmtVersion, filteredMuts);
	};

	public static GraphQLObjectType oSARS2MutComment = newObject()
		.name("SARS2BoundMutationComment")
		.description("SARS-CoV-2 mutation comments")
		.field(field -> MutationSetDef.newMutationSet("SARS2", field, "triggeredMutations")
			.description("Mutations matched this comment."))
		.field(field -> field
			.type(new GraphQLList(GeneDef.oGene.get("SARS2")))
			.name("triggeredGenes")
			.description("Distinct genes of muatations matched this comment."))
		.field(field -> field
			.type(GraphQLString)
			.name("version")
			.description("Comment version."))
		.field(field -> field
			.type(GraphQLString)
			.name("comment")
			.description("Comment text."))
		.build();

}
