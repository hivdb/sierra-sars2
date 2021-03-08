package edu.stanford.hivdb.sars2;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import com.google.gson.reflect.TypeToken;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import edu.stanford.hivdb.sequences.Sequence;
import edu.stanford.hivdb.utilities.Json;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

public class PangolinLambdaTest {

	@Test
	public void testIssueSeq() throws UnirestException {
		// clean up cache
		DeleteObjectRequest request = (
			DeleteObjectRequest.builder()
			.bucket(PangolinLambda.S3_BUCKET)
			.key("reports/a872bc8b5d1091ee91995e8d18156853a9cbed1a92e910b76d51d9b62fa6c514cc91106260404f1836c3f948d6110a4320762315091f933eb028863402165d9e.json")
			.build()
		);
		PangolinLambda.s3Client.deleteObject(request);
		
		// round one: no cache
		Sequence seq = new Sequence("issueSeq", "ACGTACGT");
		PangolinLambda pango = new PangolinLambda(seq);
		assertFalse(pango.getLoaded());
		String urlString = pango.getAsyncResultsURI();
		pango.join();
		assertTrue(pango.getLoaded());
		assertEquals(DigestUtils.sha512Hex("ACGTACGT"), pango.getTaxon());
		assertEquals("None", pango.getLineage());
		
		HttpResponse<InputStream> resp = Unirest.get(urlString).asBinary();
		Map<?, ?> payload = Json.loads(new InputStreamReader(resp.getBody()), new TypeToken<Map<?, ?>>() {});
		assertEquals(pango.getRunHash(), (String) payload.get("runHash"));
		assertEquals(pango.getReportTimestamp(), (String) payload.get("reportTimestamp"));
		
		// round two: cached
		pango = new PangolinLambda(seq);
		assertTrue(pango.getLoaded());
		assertEquals(DigestUtils.sha512Hex("ACGTACGT"), pango.getTaxon());
		assertEquals("None", pango.getLineage());
	}
	
}
