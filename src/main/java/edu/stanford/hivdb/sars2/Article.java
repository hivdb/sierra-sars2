package edu.stanford.hivdb.sars2;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Article {

	private final static Map<String, Article> singletons;
	
	static {
		SARS2 sars2 = SARS2.getInstance();
		List<Map<String, Object>> allRefs = sars2.getDRDBObj().queryAllArticles();
		Map<String, Article> localSingletons = (
			allRefs.stream()
			.map(Article::new)
			.collect(Collectors.toMap(
				ref -> ref.getRefName(),
				ref -> ref,
				(ref1, ref2) -> ref1,
				LinkedHashMap::new
			))
		);
		singletons = Collections.unmodifiableMap(localSingletons);
	}
	
	public static Article getInstance(String refName) {
		return singletons.get(refName);
	}
	
	public static Collection<Article> getAllInstances() {
		return singletons.values();
	}
	
	private final String refName;
	private final String doi;
	private final String url;
	private final String firstAuthor;
	private final Integer year;
	
	private Article(Map<String, Object> refData) {
		refName = (String) refData.get("refName");
		doi = (String) refData.get("doi");
		url = (String) refData.get("url");
		firstAuthor = (String) refData.get("firstAuthor");
		year = (Integer) refData.get("year");
	}
	
	public String name() { return refName; }
	public String getRefName() { return refName; }
	
	public String getDOI() { return doi; }
	public String getURL() { return url; }
	public String getFirstAuthor() { return firstAuthor; }
	public Integer getYear() { return year; }
	
	@Override
	public String toString() {
		return refName;
	}
	
}
