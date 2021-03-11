package edu.stanford.hivdb.sars2.graphql;

import edu.stanford.hivdb.viruses.VirusGraphQLExtension;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLList;
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
		.dataFetcher(
			coordinates("SequenceAnalysis", "suscResultsForAntibodies"),
			DRDBDef.suscResultsForAntibodiesDataFetcher
		)
		.dataFetcher(
			coordinates("SequenceReadsAnalysis", "suscResultsForAntibodies"),
			DRDBDef.suscResultsForAntibodiesDataFetcher
		)
		.dataFetcher(
			coordinates("MutationsAnalysis", "suscResultsForAntibodies"),
			DRDBDef.suscResultsForAntibodiesDataFetcher
		)
		.dataFetcher(
			coordinates("SequenceAnalysis", "suscResultsForConvPlasma"),
			DRDBDef.suscResultsForConvPlasmaDataFetcher
		)
		.dataFetcher(
			coordinates("SequenceReadsAnalysis", "suscResultsForConvPlasma"),
			DRDBDef.suscResultsForConvPlasmaDataFetcher
		)
		.dataFetcher(
			coordinates("MutationsAnalysis", "suscResultsForConvPlasma"),
			DRDBDef.suscResultsForConvPlasmaDataFetcher
		)
		.dataFetcher(
			coordinates("SequenceAnalysis", "suscResultsForImmuPlasma"),
			DRDBDef.suscResultsForImmuPlasmaDataFetcher
		)
		.dataFetcher(
			coordinates("SequenceReadsAnalysis", "suscResultsForImmuPlasma"),
			DRDBDef.suscResultsForImmuPlasmaDataFetcher
		)
		.dataFetcher(
			coordinates("MutationsAnalysis", "suscResultsForImmuPlasma"),
			DRDBDef.suscResultsForImmuPlasmaDataFetcher
		)
		.build();
	}
	
	@Override
	public GraphQLObjectType.Builder extendObjectBuilder(String objectName, GraphQLObjectType.Builder builder) {
		switch (objectName) {
			case "SequenceAnalysis":
				builder = addPangolinField(builder);
				builder = addDRDBFields(builder);
				break;
			case "SequenceReadsAnalysis":
				builder = addPangolinField(builder);
				builder = addDRDBFields(builder);
				break;
			case "MutationsAnalysis":
				builder = addDRDBFields(builder);
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
	
	private GraphQLObjectType.Builder addDRDBFields(GraphQLObjectType.Builder builder) {
		return builder
			.field(field -> field
				.type(new GraphQLList(DRDBDef.oAntibodySuscResult))
				.name("suscResultsForAntibodies")
				.description("Susceptilibity results for antibodies linked to this sequence/mutation set.")
			)
			.field(field -> field
				.type(new GraphQLList(DRDBDef.oConvPlasmaSuscResult))
				.name("suscResultsForConvPlasma")
				.description("Susceptilibity results for convalescent plasma linked to this sequence/mutation set.")
			)
			.field(field -> field
				.type(new GraphQLList(DRDBDef.oImmuPlasmaSuscResult))
				.name("suscResultsForImmuPlasma")
				.description("Susceptilibity results for vaccine-recipient plasma linked to this sequence/mutation set.")
			);
	}

}
