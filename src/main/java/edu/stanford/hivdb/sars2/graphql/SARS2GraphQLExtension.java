package edu.stanford.hivdb.sars2.graphql;

import static graphql.Scalars.*;

import edu.stanford.hivdb.sars2.drdb.Antibody;
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
			AntibodySuscResultDef.antibodySuscResultListFetcher
		)
		.dataFetcher(
			coordinates("SequenceAnalysis", "antibodySuscSummary"),
			AntibodySuscResultDef.antibodySuscSummaryFetcher
		)
		.dataFetcher(
			coordinates("SequenceReadsAnalysis", "suscResultsForAntibodies"),
			AntibodySuscResultDef.antibodySuscResultListFetcher
		)
		.dataFetcher(
			coordinates("SequenceReadsAnalysis", "antibodySuscSummary"),
			AntibodySuscResultDef.antibodySuscSummaryFetcher
		)
		.dataFetcher(
			coordinates("MutationsAnalysis", "suscResultsForAntibodies"),
			AntibodySuscResultDef.antibodySuscResultListFetcher
		)
		.dataFetcher(
			coordinates("MutationsAnalysis", "antibodySuscSummary"),
			AntibodySuscResultDef.antibodySuscSummaryFetcher
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
		.build();
	}
	
	@Override
	public GraphQLObjectType.Builder extendObjectBuilder(String objectName, GraphQLObjectType.Builder builder) {
		switch (objectName) {
			case "Root":
				builder = addAntibodiesField(builder);
				break;
			case "SequenceAnalysis":
				builder = addPangolinField(builder);
				builder = addSuscResultsForAntibodiesField(builder);
				builder = addDRDBFields(builder);
				break;
			case "SequenceReadsAnalysis":
				builder = addPangolinField(builder);
				builder = addSuscResultsForAntibodiesField(builder);
				builder = addDRDBFields(builder);
				break;
			case "MutationsAnalysis":
				builder = addSuscResultsForAntibodiesField(builder);
				builder = addDRDBFields(builder);
				break;
			default:
				throw new UnsupportedOperationException();
		}
		return builder;
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
	
	private GraphQLObjectType.Builder addPangolinField(GraphQLObjectType.Builder builder) {
		return builder
			.field(field -> field
				.type(PangolinDef.oPangolin)
				.name("pangolin")
				.description("Pangolin lineage result for this sequence.")
			);
	}

	private GraphQLObjectType.Builder addSuscResultsForAntibodiesField(GraphQLObjectType.Builder builder) {
		return builder
			.field(field -> field
				.type(new GraphQLList(AntibodySuscResultDef.oAntibodySuscResult))
				.name("suscResultsForAntibodies")
				.argument(arg -> arg
					.type(GraphQLBoolean)
					.name("includeAll")
					.defaultValue(false)
					.description(
						"By default, this field only returns susceptibility results for " +
						"antibodies in clinical trials/with structural data. By changing" +
						"this argument to `true`, all results will be included."
					)
				)
				.argument(arg -> arg
					.type(new GraphQLNonNull(GraphQLString))
					.name("drdbVersion")
					.description(
						"The version of DRDB to be used by this query. A full list of DRDB versions " +
						"can be found here: https://github.com/hivdb/chiro-cms/tree/master/downloads/covid-drdb"
					)
				)
				.description("Susceptilibity results for antibodies linked to this sequence/mutation set.")
			)
			.field(field -> field
				.type(new GraphQLList(AntibodySuscResultDef.oSuscSummaryByMutationSet))
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
			);
	}
	
	
	private GraphQLObjectType.Builder addDRDBFields(GraphQLObjectType.Builder builder) {
		return builder
			.field(field -> field
				.type(new GraphQLList(DRDBDef.oConvPlasmaSuscResult))
				.name("suscResultsForConvPlasma")
				.argument(arg -> arg
					.type(new GraphQLNonNull(GraphQLString))
					.name("drdbVersion")
					.description(
						"The version of DRDB to be used by this query. A full list of DRDB versions " +
						"can be found here: https://github.com/hivdb/chiro-cms/tree/master/downloads/covid-drdb"
					)
				)
				.description("Susceptilibity results for convalescent plasma linked to this sequence/mutation set.")
			)
			.field(field -> field
				.type(new GraphQLList(DRDBDef.oImmuPlasmaSuscResult))
				.name("suscResultsForImmuPlasma")
				.argument(arg -> arg
					.type(new GraphQLNonNull(GraphQLString))
					.name("drdbVersion")
					.description(
						"The version of DRDB to be used by this query. A full list of DRDB versions " +
						"can be found here: https://github.com/hivdb/chiro-cms/tree/master/downloads/covid-drdb"
					)
				)
				.description("Susceptilibity results for vaccine-recipient plasma linked to this sequence/mutation set.")
			);
	}

}
