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


public class VariantDef {

	public static GraphQLObjectType oVariant = newObject()
		.name("VariantObject")
		.description("Variant object")
		.field(field -> field
			.type(GraphQLString)
			.name("name")
			.description("Variant name."))
		.field(field -> field
			.type(GraphQLBoolean)
			.name("asWildtype")
			.description("Should this variant be treated as a wild type"))
		.build();

}
