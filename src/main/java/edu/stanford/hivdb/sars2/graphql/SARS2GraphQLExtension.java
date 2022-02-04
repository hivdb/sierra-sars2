package edu.stanford.hivdb.sars2.graphql;

import static graphql.Scalars.*;

import edu.stanford.hivdb.sars2.SARS2;
import edu.stanford.hivdb.sars2.drdb.Antibody;
import edu.stanford.hivdb.sars2.drdb.DRDB;
import edu.stanford.hivdb.viruses.VirusGraphQLExtension;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import static graphql.schema.FieldCoordinates.coordinates;

import java.util.Collection;
import java.util.stream.Collectors;

public class SARS2GraphQLExtension implements VirusGraphQLExtension {
	
	private static SARS2GraphQLExtension singleton = new SARS2GraphQLExtension();
	
	public static SARS2GraphQLExtension getInstance() { return singleton; }
	
	private SARS2GraphQLExtension() {}

	private static DataFetcher<Collection<Antibody>> antibodiesDataFetcher = env -> {
		String drdbVersion = env.getArgument("drdbVersion");
		Boolean fetchAll = env.getArgument("fetchAll");
		Collection<Antibody> antibodies = Antibody.getAllInstances(drdbVersion);
		if (!fetchAll) {
			antibodies = (
				antibodies.stream()
				.filter(ab -> ab.getVisibility())
				.collect(Collectors.toList())
			);
		}
		return antibodies;
	};
	
	private static DataFetcher<String> drdbLastUpdateDataFetcher = env -> {
		String drdbVersion = env.getArgument("drdbVersion");
		return DRDB.getInstance(drdbVersion).queryLastUpdate();
	};

	private static DataFetcher<Boolean> purgeCacheDataFetcher = env -> {
		return SARS2.purgeCache();
	};

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
			coordinates("SequenceAnalysis", "mutationComments"),
			SARS2MutationCommentDef.boundMutationCommentsFetcher
		)
		.dataFetcher(
			coordinates("MutationsAnalysis", "mutationComments"),
			SARS2MutationCommentDef.boundMutationCommentsFetcher
		)
		.dataFetcher(
			coordinates("SequenceReadsAnalysis", "mutationComments"),
			SARS2MutationCommentDef.boundMutationCommentsFetcher
		)
		.dataFetcher(
			coordinates("SequenceAnalysis", "antibodySuscSummary"),
			SuscResultDef.antibodySuscSummaryFetcher
		)
		.dataFetcher(
			coordinates("SequenceReadsAnalysis", "antibodySuscSummary"),
			SuscResultDef.antibodySuscSummaryFetcher
		)
		.dataFetcher(
			coordinates("MutationsAnalysis", "antibodySuscSummary"),
			SuscResultDef.antibodySuscSummaryFetcher
		)
		.dataFetcher(
			coordinates("SequenceAnalysis", "convPlasmaSuscSummary"),
			SuscResultDef.convPlasmaSuscSummaryFetcher
		)
		.dataFetcher(
			coordinates("SequenceReadsAnalysis", "convPlasmaSuscSummary"),
			SuscResultDef.convPlasmaSuscSummaryFetcher
		)
		.dataFetcher(
			coordinates("MutationsAnalysis", "convPlasmaSuscSummary"),
			SuscResultDef.convPlasmaSuscSummaryFetcher
		)
		.dataFetcher(
			coordinates("SequenceAnalysis", "vaccPlasmaSuscSummary"),
			SuscResultDef.vaccPlasmaSuscSummaryFetcher
		)
		.dataFetcher(
			coordinates("SequenceReadsAnalysis", "vaccPlasmaSuscSummary"),
			SuscResultDef.vaccPlasmaSuscSummaryFetcher
		)
		.dataFetcher(
			coordinates("MutationsAnalysis", "vaccPlasmaSuscSummary"),
			SuscResultDef.vaccPlasmaSuscSummaryFetcher
		)
		.dataFetchers(AntibodyDef.antibodyCodeRegistry)
		.dataFetchers(ArticleDef.articleCodeRegistry)
		.dataFetcher(
			coordinates("Root", "antibodies"),
			antibodiesDataFetcher
		)
		.dataFetcher(
			coordinates("Viewer", "antibodies"),
			antibodiesDataFetcher
		)
		.dataFetcher(
			coordinates("Root", "drdbLastUpdate"),
			drdbLastUpdateDataFetcher		
		)
		.dataFetcher(
			coordinates("Viewer", "drdbLastUpdate"),
			drdbLastUpdateDataFetcher		
		)
		.dataFetcher(
			coordinates("Root", "purgeCache"),
			purgeCacheDataFetcher
		)
		.build();
	}
	
	@Override
	public GraphQLObjectType.Builder extendObjectBuilder(String objectName, GraphQLObjectType.Builder builder) {
		switch (objectName) {
			case "Root":
				builder = addDRDBLastUpdateField(builder);
				builder = addAntibodiesField(builder);
				builder = addPurgeCacheField(builder);
				break;
			case "SequenceAnalysis":
				builder = addPangolinField(builder);
				builder = addSuscResultsFields(builder);
				builder = addMutationCommentsField(builder);
				break;
			case "SequenceReadsAnalysis":
				builder = addPangolinField(builder);
				builder = addSuscResultsFields(builder);
				builder = addMutationCommentsField(builder);
				break;
			case "MutationsAnalysis":
				builder = addSuscResultsFields(builder);
				builder = addMutationCommentsField(builder);
				break;
			default:
				throw new UnsupportedOperationException();
		}
		return builder;
	}
	
	private GraphQLObjectType.Builder addDRDBLastUpdateField(GraphQLObjectType.Builder builder) {
		return builder
			.field(field -> field
				.type(GraphQLString)
				.name("drdbLastUpdate")
				.argument(arg -> arg
					.type(new GraphQLNonNull(GraphQLString))
					.name("drdbVersion")
					.description(
						"The version of DRDB to be used by this query. A full list of DRDB versions " +
						"can be found here: https://github.com/hivdb/chiro-cms/tree/master/downloads/covid-drdb"
					)
				)
				.description("The last update time of Covid-DRDB.")
			);
	}
	
	private GraphQLObjectType.Builder addAntibodiesField(GraphQLObjectType.Builder builder) {
		return builder
			.field(field -> field
				.type(new GraphQLList(AntibodyDef.oAntibody))
				.name("antibodies")
				.argument(arg -> arg
					.name("fetchAll")
					.type(GraphQLBoolean)
					.defaultValue(false)
					.description("Also fetch antibodies that are not available in trial/treatment.")
				)
				.argument(arg -> arg
					.type(new GraphQLNonNull(GraphQLString))
					.name("drdbVersion")
					.description(
						"The version of DRDB to be used by this query. A full list of DRDB versions " +
						"can be found here: https://github.com/hivdb/chiro-cms/tree/master/downloads/covid-drdb"
					)
				)
				.description("List of all antibodies.")
			);
	}
	
	private GraphQLObjectType.Builder addPurgeCacheField(GraphQLObjectType.Builder builder) {
		return builder
			.field(field -> field
				.type(GraphQLBoolean)
				.name("purgeCache")
				.description("Purge server-side cached objects.")
			);
	}
	
	
	private GraphQLObjectType.Builder addPangolinField(GraphQLObjectType.Builder builder) {
		return builder
			.field(field -> field
				.type(PangolinDef.oPangolin)
				.name("pangolin")
				.argument(arg -> arg
					.name("syncFetch")
					.type(GraphQLBoolean)
					.defaultValue(false)
					.description(
						"True to await for the Pangolin software to return a PANGO lineage (slow)"
					)
				)
				.description("Pangolin lineage result for this sequence.")
			);
	}
	
	private GraphQLObjectType.Builder addMutationCommentsField(GraphQLObjectType.Builder builder) {
		return builder
			.field(field -> field
				.type(new GraphQLList(SARS2MutationCommentDef.oSARS2MutComment))
				.name("mutationComments")
				.argument(arg -> arg
					.type(new GraphQLNonNull(GraphQLString))
					.name("cmtVersion")
					.description(
						"The version of mutation comments to be used by this query. A full list of `cmtVersion`s " +
						"can be found here: https://github.com/hivdb/chiro-cms/tree/master/downloads/mutation-comments"
					)
				)
				.description("SARS-CoV-2 Mutation comments.")
			);
	}

	private GraphQLObjectType.Builder addSuscResultsFields(GraphQLObjectType.Builder builder) {
		return builder
			.field(field -> field
				.type(SuscResultDef.oSuscSummary)
				.name("antibodySuscSummary")
				.argument(arg -> arg
					.type(new GraphQLNonNull(GraphQLString))
					.name("drdbVersion")
					.description(
						"The version of DRDB to be used by this query. A full list of DRDB versions " +
						"can be found here: https://github.com/hivdb/chiro-cms/tree/master/downloads/covid-drdb"
					)
				)
				.description("Susceptibility summary for antibodies linked to this sequence/mutation set.")
			)
			.field(field -> field
				.type(SuscResultDef.oSuscSummary)
				.name("convPlasmaSuscSummary")
				.argument(arg -> arg
					.type(new GraphQLNonNull(GraphQLString))
					.name("drdbVersion")
					.description(
						"The version of DRDB to be used by this query. A full list of DRDB versions " +
						"can be found here: https://github.com/hivdb/chiro-cms/tree/master/downloads/covid-drdb"
					)
				)
				.description("Susceptibility summary for convalescent plasma linked to this sequence/mutation set.")
			)
			.field(field -> field
				.type(SuscResultDef.oSuscSummary)
				.name("vaccPlasmaSuscSummary")
				.argument(arg -> arg
					.type(new GraphQLNonNull(GraphQLString))
					.name("drdbVersion")
					.description(
						"The version of DRDB to be used by this query. A full list of DRDB versions " +
						"can be found here: https://github.com/hivdb/chiro-cms/tree/master/downloads/covid-drdb"
					)
				)
				.description("Susceptibility summary for vaccine-recipient plasma linked to this sequence/mutation set.")
			);
	}
	
}
