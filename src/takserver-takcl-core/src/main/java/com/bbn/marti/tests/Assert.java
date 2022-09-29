package com.bbn.marti.tests;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.bbn.marti.takcl.connectivity.missions.MissionModels.*;

public class Assert {

	private static final Logger logger = LoggerFactory.getLogger(Assert.class);

	public static void assertTrue(String desc, boolean value) {
		try {
			org.junit.Assert.assertTrue(desc, value);
		} catch (AssertionError e) {
			logger.error("ASSERTION FAILED: '" + desc + "'");
			throw e;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("ASSERTION PASSED. Failure Message: '" + desc + "'");
		}
	}

	public static void assertEquals(String desc, Object expected, Object actual) {
		try {
			org.junit.Assert.assertEquals(desc, expected, actual);
		} catch (AssertionError e) {
			logger.error("ASSERTION FAILED: '" + desc + "'");
			throw e;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("ASSERTION PASSED. Failure Message: '" + desc + "'");
		}
	}

	public static void assertNotEquals(String desc, Object unexpected, Object actual) {
		try {
		org.junit.Assert.assertNotEquals(desc, unexpected, actual);
		} catch (AssertionError e) {
			logger.error("ASSERTION FAILED: '" + desc + "'");
			throw e;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("ASSERTION PASSED. Failure Message: '" + desc + "'");
		}
	}

	public static void assertFalse(String desc, boolean value) {
		try {
		org.junit.Assert.assertFalse(desc, value);
		} catch (AssertionError e) {
			logger.error("ASSERTION FAILED: '" + desc + "'");
			throw e;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("ASSERTION PASSED. Failure Message: '" + desc + "'");
		}
	}

	public static void fail(String desc) {
		logger.error("ASSERTION FAILED: '" + desc + "'");
		org.junit.Assert.fail(desc);

		if (logger.isTraceEnabled()) {
			logger.trace("ASSERTION PASSED. Failure Message: '" + desc + "'");
		}
	}

	public static void assertNotNull(String desc, Object value) {
		try {
		org.junit.Assert.assertNotNull(desc, value);
		} catch (AssertionError e) {
			logger.error("ASSERTION FAILED: '" + desc + "'");
			throw e;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("ASSERTION PASSED. Failure Message: '" + desc + "'");
		}
	}

	public static class SampleObjects {
		public static final ApiSetResponse ApiSetResponse = new ApiSetResponse();
		public static final ApiSingleResponse ApiSingleResponse = new ApiSingleResponse();
		public static final ApiSetResponse<Mission> ApiSetMissionResponse = new ApiSetResponse<>();
		public static final EnterpriseSyncUploadResponse EnterpriseSyncUploadResponse = new EnterpriseSyncUploadResponse();
		public static final Mission Mission = new Mission();
		public static final MissionChange MissionChange = new MissionChange();
		public static final byte[] ByteArray = new byte[0];
		public static final SubscriptionData ReceivedSubscriptionData = new SubscriptionData();
	}

	private static <T> T assertCodeBodyTypeAndReturn(int expectedCode, @NotNull T expectedResponseType,
	                                                 @Nullable ResponseWrapper response) {
		assertNotNull("TAKCL error! Response is null!", response);
		assertEquals("Response code is not equal!", expectedCode, response.responseCode);
		Object bodyObject = response.body;
		assertNotNull("Body is null!", bodyObject);

		try {
			T result = (T) bodyObject;

			if (logger.isTraceEnabled()) {
				logger.trace("ASSERTION PASSED. Failure Message: 'Received class type \"" +
						response.body.getClass().getName() + "\" does not match expected \"" +
						expectedResponseType.getClass().getName() + "\"!");
			}
			return result;

		} catch (ClassCastException e) {
			Assert.fail("Received class type \"" + response.body.getClass().getName() + "\" does not match expected \"" +
					expectedResponseType.getClass().getName() + "\"!");
			throw e;
		}
	}

	public static byte[] getByteResponseData(int expectedCode, @Nullable ResponseWrapper rawResponse) {
		return assertCodeBodyTypeAndReturn(expectedCode, SampleObjects.ByteArray, rawResponse);
	}

	public static EnterpriseSyncUploadResponse getEnterpriseSyncUploadResponse(int expectedCode, @Nullable ResponseWrapper rawResponse) {
		return assertCodeBodyTypeAndReturn(expectedCode, SampleObjects.EnterpriseSyncUploadResponse, rawResponse);
	}

	public static void assertCallReturnCode(int expectedCode, @Nullable ResponseWrapper rawResponse) {
		Assert.assertNotNull("TAKCL error! Response is null!", rawResponse);
		Assert.assertEquals("Incorrect response code received!", expectedCode, rawResponse.responseCode);
	}

	public static <T> Set<T> getApiSetResponseData(int expectedCode, @NotNull T expectedResponseType,
	                                               @Nullable ResponseWrapper rawResponse) {
		ApiSetResponse<T> apiSetResponse = Assert.assertCodeBodyTypeAndReturn(expectedCode, new ApiSetResponse<T>(), rawResponse);
		Set<T> dataSet = apiSetResponse.data;
		Assert.assertNotNull("The response data is null!", dataSet);
		Assert.assertTrue("The response data set is empty!", dataSet.size() > 0);
		return dataSet;
	}

	public static <T> List<T> getApiListResponseData(int expectedCode, @NotNull T expectedResponseType,
	                                                 @Nullable ResponseWrapper rawResponse) {
		ApiListResponse<T> apiListResponse = Assert.assertCodeBodyTypeAndReturn(expectedCode, new ApiListResponse<>(), rawResponse);
		List<T> dataSet = apiListResponse.data;
		Assert.assertNotNull("The response data is null!", dataSet);
		Assert.assertTrue("The response data set is empty!", dataSet.size() > 0);
		return dataSet;
	}

	public static <T> Set<T> getApiSetVerificationData(int expectedCode, @NotNull T expectedResponseType,
	                                                   @Nullable ResponseWrapper rawResponse) {
		ApiSetResponse<T> apiSetResponse = Assert.assertCodeBodyTypeAndReturn(expectedCode, new ApiSetResponse<T>(), rawResponse);
		Set<T> dataSet = apiSetResponse.data;
		Assert.assertNotNull("The validation response data is null!", dataSet);
		Assert.assertTrue("The response validation  data set is empty!", dataSet.size() > 0);
		return dataSet;
	}

	public static <T> T getSingleApiSetResponseData(int expectedCode, @NotNull T expectedResponseType,
	                                                @Nullable ResponseWrapper rawResponse) {
		Set<T> dataSet = getApiSetResponseData(expectedCode, expectedResponseType, rawResponse);
		Assert.assertEquals("The returned data contains more than one object!", 1, dataSet.size());
		return dataSet.stream().findFirst().get();
	}

	public static <T> T getSingleApiSetVerificationData(int expectedCode, @NotNull T expectedResponseType,
	                                                    @Nullable ResponseWrapper rawResponse) {
		Set<T> dataSet = getApiSetResponseData(expectedCode, expectedResponseType, rawResponse);
		Assert.assertEquals("The data fetched to validate the operation contains more than one object!", 1, dataSet.size());
		return dataSet.stream().findFirst().get();
	}

	public static <T> T getApiSingleResponseData(int expectedCode, @NotNull T expectedResponseType,
	                                             @Nullable ResponseWrapper rawResponse) {
		ApiSingleResponse<T> apiSingleResponse = Assert.assertCodeBodyTypeAndReturn(expectedCode, new ApiSingleResponse<T>(), rawResponse);
		T data = apiSingleResponse.data;
		Assert.assertNotNull("The response data is null!", data);
		return data;
	}


	public static void assertNull(String desc, Object value) {
		try {
		org.junit.Assert.assertNull(desc, value);
		} catch (AssertionError e) {
			logger.error("ASSERTION FAILED: '" + desc + "'");
			throw e;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("ASSERTION PASSED. Failure Message: '" + desc + "'");
		}
	}

	public static void assertRecursiveFailure(@NotNull RecursiveMetadata metadata, @Nullable String expectedValue, @Nullable String actualValue) {
		String desc = metadata.equalityFailureFormatter(expectedValue, actualValue);
		logger.error("ASSERTION FAILED: '" + desc + "'");
		org.junit.Assert.fail(desc);
	}

	public static void assertRecursiveFailure(@NotNull RecursiveMetadata metadata, @NotNull String message) {
		String desc = metadata.failureFormatter(message);
		logger.error("ASSERTION FAILED: '" + desc + "'");
		org.junit.Assert.fail(desc);
	}

	public static void assertRecursiveException(@NotNull RecursiveMetadata metadata, @NotNull Exception exception) {
		String desc = metadata.failureFormatter(exception.getMessage());
		logger.error("ASSERTION FAILED: '" + desc + "'");
		org.junit.Assert.fail(desc);
	}

	public static void assertEmpty(String desc, Set value) {
		try {
			org.junit.Assert.assertEquals(Collections.emptySet(), value);
		} catch (AssertionError e) {
			logger.error("ASSERTION FAILED: '" + desc + "'");
			throw e;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("ASSERTION PASSED. Failure Message: '" + desc + "'");
		}
	}

	public static void assertArrayEquals(String message, byte[] expecteds, byte[] actuals) {
		try {
			org.junit.Assert.assertArrayEquals(message, expecteds, actuals);
		} catch (AssertionError e) {
			logger.error("ASSERTION FAILED: '" + message + "'");
			throw e;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("ASSERTION PASSED. Failure Message: '" + message + "'");
		}
	}
}
