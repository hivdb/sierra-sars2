package edu.stanford.hivdb.graphql;

import graphql.schema.*;
import graphql.schema.GraphQLFieldDefinition.Builder;

import static graphql.Scalars.*;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

import static edu.stanford.hivdb.graphql.SequenceReadsHistogramDef.enumAggregationOption;
import static edu.stanford.hivdb.seqreads.SequenceReadsHistogram.AggregationOption;
import static edu.stanford.hivdb.seqreads.SequenceReadsHistogramByCodonReads.WithSequenceReadsHistogramByCodonReads;

import edu.stanford.hivdb.seqreads.SequenceReadsHistogramByCodonReads;


public class SequenceReadsHistogramByCodonReadsDef {

	public static GraphQLObjectType oSeqReadsHistogramByCodonReadsBin;

	public static DataFetcher<SequenceReadsHistogramByCodonReads<?>> seqReadsHistogramByCodonReadsDataFetcher = env -> {
		WithSequenceReadsHistogramByCodonReads<?> seqReads = env.getSource();
		List<Double> codonReadsCutoffs = env.getArgument("codonReadsCutoffs");
		AggregationOption aggBy = env.getArgument("aggregatesBy");
		return seqReads.getHistogramByCodonReads(
			codonReadsCutoffs.toArray(new Long[codonReadsCutoffs.size()]),
			aggBy);
	};
	
	static {		
		oSeqReadsHistogramByCodonReadsBin = newObject()
			.name("SequenceReadsHistogramByCodonReadsBin")
			.description("A single bin data of the histogram.")
			.field(field -> field
				.name("cutoff")
				.type(GraphQLLong)
				.description("Codon count cutoff (minimal) of this bin."))
			.field(field -> field
				.name("count")
				.type(GraphQLInt)
				.description("Total count (Y axis) of this bin."))
			.build();
	}

	public static GraphQLObjectType oSeqReadsHistogramByCodonReads = newObject()
		.name("SequenceReadsHistogramByCodonReads")
		.description("Histogram data for sequence reads.")
		.field(field -> field
			.type(new GraphQLList(oSeqReadsHistogramByCodonReadsBin))
			.name("usualSites")
			.description("Usual sites histogram data."))
		.field(field -> field
			.type(new GraphQLList(oSeqReadsHistogramByCodonReadsBin))
			.name("drmSites")
			.description("Sites with drug resistance mutations histogram data."))
		.field(field -> field
			.type(new GraphQLList(oSeqReadsHistogramByCodonReadsBin))
			.name("unusualSites")
			.description("Unusual sites histogram data."))
		.field(field -> field
			.type(new GraphQLList(oSeqReadsHistogramByCodonReadsBin))
			.name("unusualApobecSites")
			.description("Unusual & APOBEC sites histogram data."))
		.field(field -> field
			.type(new GraphQLList(oSeqReadsHistogramByCodonReadsBin))
			.name("unusualNonApobecSites")
			.description("Unusual & Non-APOBEC sites histogram data."))
		.field(field -> field
			.type(new GraphQLList(oSeqReadsHistogramByCodonReadsBin))
			.name("apobecSites")
			.description("APOBEC sites histogram data."))
		.field(field -> field
			.type(new GraphQLList(oSeqReadsHistogramByCodonReadsBin))
			.name("apobecDrmSites")
			.description("APOBEC DRM sites histogram data."))
		.field(field -> field
			.type(new GraphQLList(oSeqReadsHistogramByCodonReadsBin))
			.name("stopCodonSites")
			.description("Stop codon sites histogram data."))
		.field(field -> field
			.type(GraphQLInt)
			.name("numPositions")
			.description("Total number of positions."))
		.build();

	public static UnaryOperator<Builder> oSeqReadsHistogramByCodonReadsBuilder = field -> field
		.type(oSeqReadsHistogramByCodonReads)
		.name("histogramByCodonReads")
		.description("Histogram data for sequence reads.")
		.argument(arg -> arg
		 	.name("codonReadsCutoffs")
		 	.type(new GraphQLList(GraphQLLong))
		 	.defaultValue(Arrays.asList(16, 32, 64, 128, 256, 512, 1024, 2048))
		 	.description("Codon count cutoffs wanted in this histogram."))
		.argument(arg -> arg
			.name("aggregatesBy")
			.type(enumAggregationOption)
			.defaultValue(AggregationOption.Position)
			.description("Aggregation option."));
}
