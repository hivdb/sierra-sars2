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

import edu.stanford.hivdb.utilities.SimpleMemoizer;

public class UnsequencedRegionsDef {

	public static GraphQLObjectType oUnsequencedRegion = newObject()
		.name("UnsequencedRegion")
		.description("An unsequenced region")
		.field(field -> field
			.type(GraphQLLong)
			.name("posStart")
			.description("Where the unsequenced region started")
		)
		.field(field -> field
			.type(GraphQLLong)
			.name("posEnd")
			.description("Where the unsequenced region ended")
		)
		.field(field -> field
			.type(GraphQLLong)
			.name("size")
			.description("Amino acid size of the unsequenced region")
		)
		.build();
	
	public static SimpleMemoizer<GraphQLObjectType> oUnsequencedRegions = new SimpleMemoizer<>(
		name -> (
			newObject()
			.name("UnsequencedRegions")
			.description("A set of unsequenced regions")
			.field(field -> field
				.type(GeneDef.oGene.get(name))
				.name("gene")
				.description("The object contains regions for this gene.")
			)
			.field(field -> field
				.type(new GraphQLList(oUnsequencedRegion))
				.name("regions")
				.description("The individual unsequenced regions.")
			)
			.field(field -> field
				.type(GraphQLLong)
				.name("size")
				.description("Total size of all unsequenced regions.")
			)
			.build()
		)
	);

}
