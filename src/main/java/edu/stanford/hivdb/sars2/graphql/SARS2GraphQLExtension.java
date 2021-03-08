package edu.stanford.hivdb.sars2.graphql;

import edu.stanford.hivdb.viruses.VirusGraphQLExtension;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLObjectType;
import static graphql.schema.FieldCoordinates.coordinates;

public class SARS2GraphQLExtension implements VirusGraphQLExtension {
	
	private static SARS2GraphQLExtension singleton = new SARS2GraphQLExtension();
	
	public static SARS2GraphQLExtension getInstance() { return singleton; }
	
	private SARS2GraphQLExtension() {}

	@Override
	public GraphQLCodeRegistry getExtendedCodonRegistry() {
		return GraphQLCodeRegistry.newCodeRegistry()
		.dataFetcher(
			coordinates("SequenceAnalysis", "pangolin"),
			PangolinDef.pangolinDataFetcher
		)
		.dataFetcher(
			coordinates("SequenceReadsAnalysis", "pangolin"),
			PangolinDef.pangolinDataFetcher
		)
		.build();
	}
	
	@Override
	public GraphQLObjectType.Builder extendObjectBuilder(String objectName, GraphQLObjectType.Builder builder) {
		switch (objectName) {
			case "SequenceAnalysis":
				builder = addPangolinField(builder);
				break;
			case "SequenceReadsAnalysis":
				builder = addPangolinField(builder);
				break;
			case "MutationsAnalysis":
				break;
			default:
				throw new UnsupportedOperationException();
		}
		return builder;
	}
	
	private GraphQLObjectType.Builder addPangolinField(GraphQLObjectType.Builder builder) {
		return builder
			.field(field -> field
				.type(PangolinDef.oPangolin)
				.name("pangolin")
				.description("Pangolin lineage result for this sequence.")
			);
	}

}
