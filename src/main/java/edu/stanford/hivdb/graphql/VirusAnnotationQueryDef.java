package edu.stanford.hivdb.graphql;

import edu.stanford.hivdb.utilities.SimpleMemoizer;
import edu.stanford.hivdb.viruses.Virus;
import graphql.schema.GraphQLEnumType;

public class VirusAnnotationQueryDef {

	public static SimpleMemoizer<GraphQLEnumType> oVirusAnnotationFunction = new SimpleMemoizer<>(
		name -> {
			GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum()
				.name("VirusAnnotationFunction")
				.description("Virus annotation function.");
			Virus<?> virusIns = Virus.getInstance(name);
			for (String func : virusIns.getAnnotationFunctionList()) {
				builder.value(func, func);
			}
			return builder.build();
		}
	);

}
