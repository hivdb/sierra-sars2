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

import edu.stanford.hivdb.sars2.PangolinLambda;
import edu.stanford.hivdb.seqreads.SequenceReads;
import edu.stanford.hivdb.sequences.AlignedSequence;
import edu.stanford.hivdb.sequences.Sequence;

public class PangolinDef {

	public static DataFetcher<PangolinLambda> pangolinDataFetcher = env -> {
		Object seq = env.getSource();
		if (seq instanceof AlignedSequence) {
			Sequence inputSeq = ((AlignedSequence<?>) seq).getInputSequence();
			return new PangolinLambda(inputSeq);
		}
		else if (seq instanceof SequenceReads) {
			SequenceReads<?> seqReads = (SequenceReads<?>) seq;
			String concatSeq = seqReads.getConcatenatedSeqForSubtyping();
			Sequence inputSeq = new Sequence(seqReads.getName(), concatSeq);
			return new PangolinLambda(inputSeq);
		}
		else {
			throw new UnsupportedOperationException();
		}
	};

	public static GraphQLObjectType oPangolin = newObject()
		.name("PangolinObject")
		.description("Object for Pangolin Lineage report")
		.field(field -> field
			.type(GraphQLString)
			.name("runHash")
			.description("SHA-512 hashed string for the input sequence."))
		.field(field -> field
			.type(GraphQLString)
			.name("reportTimestamp")
			.description("The timestamp when this report was generated."))
		.field(field -> field
			.type(GraphQLString)
			.name("version")
			.description("The version of Pangolin / PangoLEARN for this report (usually the same as `latestVersion`)."))
		.field(field -> field
			.type(GraphQLString)
			.name("latestVersion")
			.description("The latest version of Pangolin / PangoLEARN on our server."))
		.field(field -> field
			.type(GraphQLBoolean)
			.name("loaded")
			.description("A flag indicates if the results are loaded. When this argument is false, client can instead load the results from `asyncResultsURI`."))
		.field(field -> field
			.type(GraphQLString)
			.name("asyncResultsURI")
			.description("The pangolin-runner returns results asynchronically. This URI is provided for the client to retrieve the results later."))
		.field(field -> field
			.type(GraphQLString)
			.name("taxon")
			.description("Hashed sequence name (SHA-512)."))
		.field(field -> field
			.type(GraphQLString)
			.name("lineage")
			.description("Lineage result reported by Pangolin."))
		.field(field -> field
			.type(GraphQLFloat)
			.name("probability")
			.description("Pangolin probability."))
		.field(field -> field
			.type(GraphQLString)
			.name("status")
			.description("Pangolin status."))
		.field(field -> field
			.type(GraphQLString)
			.name("note")
			.description("Pangolin note."))
		.build();

}
