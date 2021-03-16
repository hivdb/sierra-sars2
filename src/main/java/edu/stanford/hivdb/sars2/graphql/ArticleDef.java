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
import static graphql.schema.FieldCoordinates.coordinates;
import static edu.stanford.hivdb.graphql.ExtGraphQL.ExtPropertyDataFetcher;


public class ArticleDef {

	public static GraphQLCodeRegistry articleCodeRegistry = GraphQLCodeRegistry.newCodeRegistry()
		.dataFetcher(
			coordinates("AntibodyObject", "DOI"),
			new ExtPropertyDataFetcher<String>("getDOI")
		)
		.dataFetcher(
			coordinates("AntibodyObject", "URL"),
			new ExtPropertyDataFetcher<String>("getURL")
		)
		.build();

	public static GraphQLObjectType oArticle = newObject()
		.name("ArticleObject")
		.description("Article object")
		.field(field -> field
			.type(GraphQLString)
			.name("refName")
			.description("Reference unique identifier provided by COVDB."))
		.field(field -> field
			.type(GraphQLString)
			.name("DOI")
			.description("Article DOI."))
		.field(field -> field
			.type(GraphQLString)
			.name("URL")
			.description("Reference URL. Provided when DOI is unavailable."))
		.field(field -> field
			.type(GraphQLString)
			.name("firstAuthor")
			.description("Article first author."))
		.field(field -> field
			.type(GraphQLInt)
			.name("year")
			.description("Article publication year."))
		.build();

}
