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

import edu.stanford.hivdb.utilities.SimpleMemoizer;
import edu.stanford.hivdb.viruses.Virus;

import static edu.stanford.hivdb.graphql.StrainDef.*;
import static edu.stanford.hivdb.graphql.ExtGraphQL.*;

public class GeneDef {

	public static SimpleMemoizer<GraphQLEnumType> enumGene = new SimpleMemoizer<>(
		name -> {
			Virus<?> virusIns = Virus.getInstance(name);
			GraphQLEnumType.Builder newEnumGene =
				GraphQLEnumType.newEnum().name("EnumGene");
			for (String gene : virusIns.getAbstractGenes()) {
				newEnumGene.value(gene, gene);
			}
			return newEnumGene.build();
		}
	);
	
	public static GraphQLCodeRegistry geneCodeRegistry = newCodeRegistry()
		.dataFetcher(
			coordinates("Gene", "reference"),
			new ExtPropertyDataFetcher<String>("refSequence")
		)
		.dataFetcher(
			coordinates("Gene", "consensus"),
			new ExtPropertyDataFetcher<String>("refSequence")
		)
		.dataFetcher(
			coordinates("Gene", "name"),
			new ExtPropertyDataFetcher<String>("abstractGene")
		)
		.dataFetcher(
			coordinates("Gene", "nameWithStrain"),
			new ExtPropertyDataFetcher<String>("name")
		)
		.dataFetcher(
			coordinates("Gene", "AASize"),
			new ExtPropertyDataFetcher<Integer>("getAASize")
		)
		.dataFetcher(
			coordinates("Gene", "NASize"),
			new ExtPropertyDataFetcher<Integer>("getNASize")
		)
		.build();
		
	public static SimpleMemoizer<GraphQLObjectType> oGene = new SimpleMemoizer<>(
		name -> (
			newObject()
			.name("Gene")
			.description("Virus genes")
			.field(field -> field
				.type(GraphQLString)
				.name("nameWithStrain")
				.description("Name of the gene (with strain name)."))
			.field(field -> field
				.type(enumGene.get(name))
				.name("name")
				.description("Name of the gene (without strain name)."))
			.field(field -> field
				.type(oStrain)
				.name("strain")
				.description("Virus strain referred by this gene."))
			.field(field -> field
				.type(GraphQLString)
				.name("refSequence")
				.description("Reference sequence of this gene."))
			.field(field -> field
				.type(GraphQLString)
				.name("reference")
				.deprecate("Use field `refSequence` instead."))
			.field(field -> field
				.type(GraphQLString)
				.name("consensus")
				.deprecate("Use field `refSequence` instead."))
			.field(field -> field
				.type(GraphQLInt)
				.name("length")
				.deprecate("Use `AASize` instead."))
			.field(field -> field
				.type(GraphQLInt)
				.name("AASize")
				.description("AA length of current gene."))
			.field(field -> field
				.type(GraphQLInt)
				.name("NASize")
				.description("NA length of current gene."))
			.field(field -> field
				.type(new GraphQLList(new GraphQLTypeReference("DrugClass")))
				.name("drugClasses")
				.description("Supported drug classes of current gene."))
			.field(field -> field
				.type(new GraphQLList(new GraphQLTypeReference("MutationType")))
				.name("mutationTypes")
				.description("Supported mutation types of current gene."))
			.build()
		)
	);

}
