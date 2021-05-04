package edu.stanford.hivdb.sars2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.gson.reflect.TypeToken;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import edu.stanford.hivdb.mutations.Mutation;
import edu.stanford.hivdb.mutations.MutationSet;
import edu.stanford.hivdb.utilities.Json;
import edu.stanford.hivdb.viruses.Gene;

public class SARS2MutationComment {

	public final static SARS2 sars2 = SARS2.getInstance();
	public final static String URL_PREFIX = "https://s3-us-west-2.amazonaws.com/cms.hivdb.org/chiro-prod/downloads/mutation-comments/";
	public static Map<String, List<SARS2MutationComment>> singletons = new HashMap<>();
	
	public static class SARS2BoundMutationComment {
		private final MutationSet<SARS2> triggeredMutations;
		private final SARS2MutationComment commentObj;
		
		private SARS2BoundMutationComment(MutationSet<SARS2> triggeredMutations, SARS2MutationComment commentObj) {
			this.triggeredMutations = triggeredMutations;
			this.commentObj = commentObj;
		}
		
		public SortedSet<Gene<SARS2>> getTriggeredGenes() {
			return (
				triggeredMutations.stream()
				.map(mut -> mut.getGene())
				.collect(Collectors.toCollection(TreeSet::new))
			);
		}

		public MutationSet<SARS2> getTriggeredMutations() { return triggeredMutations; }
		public String getComment() { return commentObj.getComment(); }
		public String getVersion() { return commentObj.getVersion(); }
	}
	
	public static List<SARS2BoundMutationComment> query(String cmtVersion, Collection<Mutation<SARS2>> mutations) {
		List<SARS2MutationComment> instances = getInstances(cmtVersion);
		List<SARS2BoundMutationComment> triggeredCmts = new ArrayList<>();
		for (SARS2MutationComment cmtObj : instances) {
			MutationSet<SARS2> triggered = cmtObj.getMutations().intersectsWith(mutations);
			if (!triggered.isEmpty()) {
				triggeredCmts.add(new SARS2BoundMutationComment(triggered, cmtObj));
			}
		}
		return triggeredCmts;
	}
	
	private static List<SARS2MutationComment> getInstances(String cmtVersion) {
		if (!singletons.containsKey(cmtVersion)) {
			HttpResponse<String> response;
			Map<String, Object> results;
			try {
				response = Unirest.get(URL_PREFIX + cmtVersion + ".json")
					.asString();
				results = Json.loads(response.getBody(), new TypeToken<Map<String, Object>>() {});
			} catch (UnirestException e) {
				throw new RuntimeException(e);
			}
			cmtVersion = (String) results.get("version");
			List<?> payload = (List<?>) results.get("payload");
			List<SARS2MutationComment> commentObjs = new ArrayList<>();
			for (Object cmt : payload) {
				Map<?, ?> cmtMap = (Map<?, ?>) cmt;
				String mutations = (String) cmtMap.get("mutations");
				String comment = (String) cmtMap.get("comment");
				MutationSet<SARS2> mutationSet = sars2.newMutationSet(mutations);
				commentObjs.add(new SARS2MutationComment(mutationSet, comment, cmtVersion));
			}
			singletons.put(cmtVersion, Collections.unmodifiableList(commentObjs));
		}
		return singletons.get(cmtVersion);
	}
	
	private final MutationSet<SARS2> mutations;
	private final String comment;
	private final String version;
	
	private SARS2MutationComment(MutationSet<SARS2> mutations, String comment, String version) {
		this.mutations = mutations;
		this.comment = comment;
		this.version = version;
	}
	
	public MutationSet<SARS2> getMutations() { return mutations; }
	public String getComment() { return comment; }
	public String getVersion() { return version; }
	
}
