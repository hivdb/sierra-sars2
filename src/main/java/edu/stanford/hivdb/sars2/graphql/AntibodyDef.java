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

import static edu.stanford.hivdb.graphql.ExtGraphQL.ExtPropertyDataFetcher;

import static graphql.schema.FieldCoordinates.coordinates;

public class AntibodyDef {

	public static GraphQLCodeRegistry antibodyCodeRegistry = GraphQLCodeRegistry.newCodeRegistry()
		.dataFetcher(
			coordinates("AntibodyObject", "PDB"),
			new ExtPropertyDataFetcher<String>("getPDB")
		)
		.build();

	public static GraphQLObjectType oAntibody = newObject()
		.name("AntibodyObject")
		.description("Antibody object")
		.field(field -> field
			.type(GraphQLString)
			.name("name")
			.description("Antibody name."))
		.field(field -> field
			.type(GraphQLString)
			.name("abbrName")
			.description("Antibody abbreviation name."))
		.field(field -> field
			.type(GraphQLString)
			.name("availability")
			.description("Antibody availability."))
		.field(field -> field
			.type(GraphQLInt)
			.name("priority")
			.description("Antibody sorting order."))
		.field(field -> field
			.type(GraphQLBoolean)
			.name("visibility")
			.description("Antibody visibility for summary tables."))
		.field(field -> field
			.type(GraphQLString)
			.name("target")
			.description("Antibody target by structure."))
		.field(field -> field
			.type(GraphQLString)
			.name("antibodyClass")
			.description("Antibody class by structure."))
		.field(field -> field
			.type(new GraphQLList(GraphQLString))
			.name("synonyms")
			.description("Antibody synonym names."))
		.build();

}
