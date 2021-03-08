package edu.stanford.hivdb.sars2;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.gson.reflect.TypeToken;

import edu.stanford.hivdb.sequences.Sequence;
import edu.stanford.hivdb.utilities.AssertUtils;
import edu.stanford.hivdb.utilities.FastaUtils;
import edu.stanford.hivdb.utilities.Json;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

public class PangolinLambda {
	
	private static final String FUNCTION_NAME;
	private static final String FUNCTION_QUAL;
	private static final String FUNCTION_REGION;
	private static final String S3_REGION;
	protected static final String S3_BUCKET;
	private static final LambdaAsyncClient client;
	protected static final S3Client s3Client;
	private static final S3Presigner s3Presigner;
	private CompletableFuture<InvokeResponse> asyncResponse;
	private String runHash;
	private boolean loaded = false;
	private String latestVersion;
	private String version;
	private String reportTimestamp;
	
	private String taxon;
	private String lineage;
	private Double probability;
	private String status;
	private String note;
	
	static {
		Map<String, String> envs = System.getenv();
		FUNCTION_NAME = envs.getOrDefault("PANGOLIN_LAMBDA_FUNCTION_NAME", "pangolin-runner");
		FUNCTION_QUAL = envs.getOrDefault("PANGOLIN_LAMBDA_FUNCTION_VERSION", "$LATEST");
		FUNCTION_REGION = envs.getOrDefault("PANGOLIN_LAMBDA_REGION", "us-west-2");
		S3_REGION = envs.getOrDefault("PANGOLIN_LAMBDA_S3_REGION", "us-west-2");
		S3_BUCKET = envs.getOrDefault("PANGOLIN_LAMBDA_S3_BUCKET", "pangolin-assets.hivdb.org");
		client = (
			LambdaAsyncClient.builder()
			.region(Region.of(FUNCTION_REGION))
			.build()
		);
		s3Client = (
			S3Client.builder()
			.region(Region.of(S3_REGION))
			.build()
		);
		s3Presigner = (
			S3Presigner.builder()
			.region(Region.of(S3_REGION))
			.build()
		);
	}
	
	public PangolinLambda(Sequence seq) {
		String fastaText = FastaUtils.writeString(seq, /* useSHA512Name */true);
		runHash = DigestUtils.sha512Hex(fastaText);
		tryLoadRecentCache();
		if (!loaded) {
			InvokeRequest requests = (
				InvokeRequest.builder()
				.functionName(FUNCTION_NAME)
				.qualifier(FUNCTION_QUAL)
				.payload(SdkBytes.fromString(
					Json.dumps(Map.of(
						"body", fastaText
					)),
					StandardCharsets.UTF_8
				))
				.build()
			);
			asyncResponse = client.invoke(requests);
		}
	}
	
	private void tryLoadRecentCache() {
		GetObjectRequest latestVerRequest = (
			GetObjectRequest.builder()
			.bucket(S3_BUCKET)
			.key("latest_version")
			.build()
		);
		latestVersion = (
			s3Client.getObjectAsBytes(latestVerRequest)
			.asUtf8String()
			.trim()
		);
		GetObjectRequest payloadRequest = (
			GetObjectRequest.builder()
			.bucket(S3_BUCKET)
			.key(String.format("reports/%s.json", runHash))
			.build()
		);
		try {
			String payload = (
				s3Client.getObjectAsBytes(payloadRequest)
				.asUtf8String()
			);
			populatePayload(payload, true);
		}
		catch (NoSuchKeyException exc) {
			return;
		}
	}
	
	private void populatePayload(String payload, boolean checkVersion) {
		Map<String, ?> results = Json.loads(payload, new TypeToken<Map<String, ?>>() {});
		String remoteRunHash = (String) results.get("runHash");
		AssertUtils.isTrue(
			runHash.equals(remoteRunHash),
			"Mismatched run hash string: expected %s, but received %s",
			runHash, remoteRunHash
		);
		version = (String) results.get("version");
		if (checkVersion && !version.equals(latestVersion)) {
			return;
		}
		reportTimestamp = (String) results.get("reportTimestamp");
		List<?> reports = (List<?>) results.get("reports");
		Map<?, ?> report = (Map<?, ?>) reports.get(0);
		taxon = (String) report.get("taxon");
		lineage = (String) report.get("lineage");
		probability = (Double) report.get("probability");
		status = (String) report.get("status");
		note = (String) report.get("note");
		loaded = true;
	}
	
	public void join() {
		if (loaded) {
			return;
		}
		InvokeResponse response = asyncResponse.join();
		String payload = response.payload().asUtf8String();
		Map<String, ?> mapPayload = Json.loads(payload, new TypeToken<Map<String, ?>>() {});
		populatePayload((String) mapPayload.get("body"), false);
	}
	
	public String getRunHash() {
		return runHash;
	}
	
	public String getAsyncResultsURI() {
		
		// see: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-presign.html
		
		// Generate the presigned URL
		GetObjectRequest request = (
			GetObjectRequest.builder()
			.bucket(S3_BUCKET)
			.key(String.format("reports/%s.json", runHash))
			.build()
		);
		GetObjectPresignRequest presignRequest = (
			GetObjectPresignRequest.builder()
			.signatureDuration(Duration.ofHours(1))
			.getObjectRequest(request)
			.build()
		);
		
		PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
		
		URL url = presignedRequest.url();
		return url.toString();
	}
	
	public String getVersion() {
		return version;
	}
	
	public String getReportTimestamp() {
		return reportTimestamp;
	}
	
	public Boolean getLoaded() {
		return loaded;
	}
	
	public String getTaxon() {
		return taxon;
	}
	
	public String getLineage() {
		return lineage;
	}
	
	public Double getProbability() {
		return probability;
	}
	
	public String getStatus() {
		return status;
	}
	
	public String getNote() {
		return note;
	}
	
}
