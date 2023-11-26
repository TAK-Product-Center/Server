package com.bbn.marti.takcl.connectivity.missions;

import com.bbn.marti.takcl.TestExceptions;
import com.bbn.marti.tests.Assert;
import com.google.gson.*;
import org.dom4j.Document;
import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Pattern;

public class MissionModels {

	public static final MissionUserRole DEFAULT_MISSION_USER_ROLE = MissionUserRole.MISSION_OWNER;
	public static final String EXCEPTION_IGNORE_VALUE = "!#EXCEPTION_IGNORED#!";
	public static final String EXCEPTION_NOT_NULL = "!#EXCEPTION_NOT_NULL#!";

	public static final Pattern MISSION_GROUPS_PATTERN = Pattern.compile("^Mission\\.groups$");
	public static final Pattern MISSION_UIDS_PATTERN = Pattern.compile("^Mission\\.uids$");
	public static final Pattern MISSION_UID_TIMESTAMPS_PATTERN = Pattern.compile("^Mission\\.uids\\.[0-9]*\\.timestamp");
	public static final Pattern MISSIONCHANGE_SERVERTIME_PATTERN = Pattern.compile("^MissionChange\\.serverTime");
	public static final Pattern MISSIONCHANGE_TIMESTAMP_PATTERN = Pattern.compile("^MissionChange\\.timestamp");

	/**
	 * Converts a list of values to a displayable list of key-value pairs with unicode.
	 * The conversion by the JUnit xml to things like {@literal "&apos;"} was making things incredibly unreadable in raw xml...
	 * @param keyValuePairs
	 * @return
	 */
	public static String keyValueDisplayConverter(Object... keyValuePairs) {
		StringBuilder sb = new StringBuilder("(");

		for (int i = 0; i < keyValuePairs.length; i=i+2) {
			if (i >= 2) {
				sb.append(",");
			}
			sb.append(keyValuePairs[i]).append("=").append("\u2019").append(keyValuePairs[i+1]).append("\u2019");
		}
		return sb.append(")").toString();
	}

	public static class RecursiveMetadata {
		public final String parameterName;
		public final String actualHint;
		public final String expectedHint;

		public RecursiveMetadata(@NotNull String parameterName, @Nullable String expectedHint, @Nullable String actualHint) {
			this.parameterName = parameterName;
			this.expectedHint = expectedHint == null ? "?" : expectedHint;
			this.actualHint = actualHint == null ? "?" : actualHint;
		}

		public RecursiveMetadata createChild(@NotNull String parameterName, @Nullable String expectedHint, @Nullable String actualHint) {
			return new RecursiveMetadata(
					this.parameterName + "." + parameterName,
					this.expectedHint + "-" + (expectedHint == null ? "?" : expectedHint),
					this.actualHint + "-" + (actualHint == null ? "?" : actualHint));
		}

		public final String equalityFailureFormatter(@Nullable String expectedValue, @Nullable String actualValue) {
			return "FAILED COMPARISON!\n\t" +
					"Actual (" + actualHint + "): " + parameterName + "=" + actualValue + "\n\t" +
					"Expected (" + expectedHint + "): " + parameterName + "=" + expectedValue;
		}

		public final void logSuccess(@Nullable String value) {
			if (logger.isTraceEnabled()) {
				logger.trace("PASSED COMPARISON!  Actual (" + actualHint + "): " + parameterName + "==" +
						"Expected (" + expectedHint + "): " + parameterName + ", value=" + value);
			}
		}

		public final String failureFormatter(@NotNull String message) {
			return "FAILURE: " + keyValueDisplayConverter("actualHint", actualHint, "expectedHint", expectedHint,
					"parameterName", parameterName, "message", message);
		}

		@Override
		public String toString() {
			return ("Metadata(" + keyValueDisplayConverter("parametername", parameterName, "actualHint", actualHint,
					"expectedHint", expectedHint) + ")");
		}
	}

	public static class MissionContentDataContainerDeserializer implements JsonDeserializer<MissionContentDataContainer> {

		@Override
		public MissionContentDataContainer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			MissionContentDataContainer result;

			try {
				result = new MissionContentDataContainer();
				JsonObject jsonObject = (JsonObject) json;

				Class<?> clazz = MissionModels.class.getClassLoader().loadClass(typeOfT.getTypeName());
				for (Field field : clazz.getFields()) {
					String fieldName = field.getName();
					Class<?> fieldType = field.getType();
					if (fieldName.equals("data")) {
						JsonElement dataElement = jsonObject.get("data");
						if (dataElement.isJsonNull()) {
							field.set(result, null);
						} else if (dataElement.isJsonObject()) {
							JsonObject dataObject = (JsonObject) dataElement;
							Resource mcd = context.deserialize(dataObject, Resource.class);
							field.set(result, mcd);

						} else if (dataElement.isJsonPrimitive()) {
							JsonPrimitive dataPrimitive = (JsonPrimitive) dataElement;
							if (dataPrimitive.isString()) {
								field.set(result, dataPrimitive.getAsString());
							} else {
								throw new RuntimeException("Primitive type " + dataPrimitive + " not currently supported!");
							}
						}

					} else {
						field.set(result, context.deserialize(jsonObject.get(fieldName), fieldType));
					}

				}
			} catch (ClassNotFoundException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			return result;
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface OmitFromEqualsAssertion {
	}

	private static final Logger logger = LoggerFactory.getLogger(MissionModels.class);

	public static final Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(MissionContentDataContainer.class, new MissionContentDataContainerDeserializer())
			.create();

	public static abstract class AssertableObject<T extends AssertableObject<T>> implements Comparable<T> {

		@OmitFromEqualsAssertion
		private String _comparisonHint;

		private static boolean assertStringsEqual(@NotNull RecursiveMetadata metadata, @Nullable String expectedValue, @Nullable String actualValue) {
			if ((expectedValue != null && !expectedValue.equals(actualValue)) ||
					(actualValue != null && !actualValue.equals(expectedValue))) {
				Assert.assertRecursiveFailure(metadata, expectedValue, actualValue);
				return false;
			}
			metadata.logSuccess(actualValue);
			return true;
		}

		private static boolean isPrimitiveOrNull(@Nullable Object obj) {
			return obj == null ||
					obj instanceof Integer ||
					obj instanceof Float ||
					obj instanceof Double ||
					obj instanceof Byte ||
					obj instanceof Character ||
					obj instanceof Boolean ||
					obj instanceof Long;
		}

		private static boolean isPrimitiveType(@NotNull Type type) throws ClassNotFoundException {
			String typeName = type.getTypeName();
			if (typeName.equals("int") ||
					typeName.equals("float") ||
					typeName.equals("double") ||
					typeName.equals("byte") ||
					typeName.equals("char") ||
					typeName.equals("boolean") ||
					typeName.equals("long")) {
				return true;
			} else {
				Class<?> typeClass = MissionModels.class.getClassLoader().loadClass(typeName);
				return (typeClass == Integer.class ||
						typeClass == Float.class ||
						typeClass == Double.class ||
						typeClass == Byte.class ||
						typeClass == Character.class ||
						typeClass == Long.class);
			}
		}

		private static boolean assertPrimitivesEqual(@NotNull RecursiveMetadata metadata, @Nullable Object expectedValue, @Nullable Object actualValue) {
			Assert.assertTrue("The provided expected value is not a primitive value!", isPrimitiveOrNull(expectedValue));
			Assert.assertTrue("The provided actual value is not a primitive value!", isPrimitiveOrNull(actualValue));

			if ((expectedValue != null && !expectedValue.equals(actualValue)) ||
					(actualValue != null && !actualValue.equals(expectedValue))) {
				String actualStr = actualValue == null ? null : actualValue.toString();
				String expectedStr = expectedValue == null ? null : expectedValue.toString();
				Assert.assertRecursiveFailure(metadata, expectedStr, actualStr);
				return false;
			}
			metadata.logSuccess(actualValue == null ? null : actualValue.toString());

			return true;
		}


		private static boolean assertEnumsEqual(@NotNull RecursiveMetadata metadata, @Nullable Object expectedValue, @Nullable Object actualValue) {
			Assert.assertTrue("The provided expected value is not an enum!", expectedValue instanceof Enum);
			Assert.assertTrue("The provided actual value is not an enum!", actualValue instanceof Enum);

			if ((expectedValue != null && expectedValue != actualValue) ||
					(actualValue != null && actualValue != actualValue)) {
				String actualStr = actualValue == null ? null : actualValue.toString();
				String expectedStr = expectedValue == null ? null : expectedValue.toString();
				Assert.assertRecursiveFailure(metadata, expectedStr, actualStr);
				return false;
			}
			metadata.logSuccess(actualValue == null ? null : actualValue.toString());

			return true;
		}

		private static boolean assertListsEqual(@NotNull RecursiveMetadata metadata, @Nullable List expectedObjectList,
		                                       @Nullable List actualObjectList, Map<Pattern, Object> expectationOverrides) {

			if (expectedObjectList == null) {
				if (actualObjectList == null) {
					metadata.logSuccess(null);
					return true;
				} else {
					Assert.assertRecursiveFailure(metadata, null, actualObjectList.toString());
					return false;
				}
			} else {
				if (actualObjectList == null) {
					Assert.assertRecursiveFailure(metadata, expectedObjectList.toString(), null);
					return false;
				}

				if (expectedObjectList.size() != actualObjectList.size()) {
					System.err.println("ParamName=" + metadata.parameterName);
					Assert.assertRecursiveFailure(metadata, "List.size=" + expectedObjectList.size(), "List.size=" + actualObjectList.size());
					return false;
				}

				if (expectedObjectList.size() == 0) {
					metadata.logSuccess("[]");
					return true;
				}

				Iterator expectedIterator = expectedObjectList.iterator();
				Iterator actualIterator = actualObjectList.iterator();

				int iterationCount = 0;
				while (expectedIterator.hasNext()) {
					Object expectedObject = expectedIterator.next();
					Object actualObject = actualIterator.next();

					if (expectedObject instanceof AssertableObject && actualObject instanceof AssertableObject) {
						AssertableObject expectedAssertableObject = (AssertableObject) expectedObject;
						AssertableObject actualAssertableObject = (AssertableObject) actualObject;
						actualAssertableObject.getThis().assertMatchesExpectation(expectedAssertableObject.getThis(),
								metadata.createChild(Integer.toString(iterationCount), null, null), expectationOverrides);
					} else {
						if (!actualObject.equals(expectedObject)) {
							Assert.assertRecursiveFailure(metadata.createChild(Integer.toString(iterationCount), null, null),
									actualObject.toString(), expectedObject.toString());
							return false;
						}
					}
					iterationCount++;
				}
				if (logger.isTraceEnabled()) {
					HashSet<String> displayValues = new HashSet<>();
					expectedIterator = expectedObjectList.iterator();

					while (expectedIterator.hasNext()) {
						Object obj = expectedIterator.next();
						if (AssertableObject.class.isAssignableFrom(obj.getClass())) {
							displayValues.add(((AssertableObject) obj).getUniqueStableName());
						} else {
							displayValues.add(obj.toString());
						}
					}
					metadata.logSuccess(String.join(",", displayValues));
				}
				return true;
			}
		}

		private static boolean assertSetsEqual(@NotNull RecursiveMetadata metadata, @Nullable TreeSet expectedObjectSet,
		                                       @Nullable TreeSet actualObjectSet, Map<Pattern, Object> expectationOverrides) {

			if (expectedObjectSet == null) {
				if (actualObjectSet == null) {
					metadata.logSuccess(null);
					return true;
				} else {
					Assert.assertRecursiveFailure(metadata, null, actualObjectSet.toString());
					return false;
				}
			} else {
				if (actualObjectSet == null) {
					Assert.assertRecursiveFailure(metadata, expectedObjectSet.toString(), null);
					return false;
				}

				if (expectedObjectSet.size() != actualObjectSet.size()) {
					System.err.println("ParamName=" + metadata.parameterName);
					Assert.assertRecursiveFailure(metadata, "Set.size=" + expectedObjectSet.size(), "Set.size=" + actualObjectSet.size());
					return false;
				}

				if (expectedObjectSet.size() == 0) {
					metadata.logSuccess("[]");
					return true;
				}

				Iterator expectedIterator = expectedObjectSet.iterator();
				Iterator actualIterator = actualObjectSet.iterator();

				int iterationCount = 0;
				while (expectedIterator.hasNext()) {
					Object expectedObject = expectedIterator.next();
					Object actualObject = actualIterator.next();

					if (expectedObject instanceof AssertableObject && actualObject instanceof AssertableObject) {
						AssertableObject expectedAssertableObject = (AssertableObject) expectedObject;
						AssertableObject actualAssertableObject = (AssertableObject) actualObject;
						actualAssertableObject.getThis().assertMatchesExpectation(expectedAssertableObject.getThis(),
								metadata.createChild(Integer.toString(iterationCount), null, null), expectationOverrides);
					} else {
						if (!actualObject.equals(expectedObject)) {
							Assert.assertRecursiveFailure(metadata.createChild(Integer.toString(iterationCount), null, null),
									actualObject.toString(), expectedObject.toString());
							return false;
						}
					}
					iterationCount++;
				}
				if (logger.isTraceEnabled()) {
					HashSet<String> displayValues = new HashSet<>();
					expectedIterator = expectedObjectSet.iterator();

					while (expectedIterator.hasNext()) {
						Object obj = expectedIterator.next();
						if (AssertableObject.class.isAssignableFrom(obj.getClass())) {
							displayValues.add(((AssertableObject) obj).getUniqueStableName());
						} else {
							displayValues.add(obj.toString());
						}
					}
					metadata.logSuccess(String.join(",", displayValues));
				}
				return true;
			}
		}

		public final boolean assertMatchesExpectation(@NotNull T desiredObject, @Nullable Map<Pattern, Object> expectationOverrides) {
			return assertMatchesExpectation(desiredObject, null, expectationOverrides);

		}

		private static Object determineResultantValue(@NotNull RecursiveMetadata metadata, @Nullable Object expectedValue, Map<Pattern, Object> expectationOverrides) {
			String parameterName = metadata.parameterName;
			if (expectationOverrides != null) {
				for (Pattern pattern : expectationOverrides.keySet()) {
					if (pattern.matcher(parameterName).matches()) {
						if (logger.isTraceEnabled()) {
							logger.trace("Overriding value for " + parameterName + "!");
						}
						return expectationOverrides.get(pattern);
					}
				}
			}
			return expectedValue;
		}

		private boolean assertMatchesExpectation(@NotNull T desiredObject, @Nullable RecursiveMetadata metadata,
		                                         @Nullable Map<Pattern, Object> expectationOverrides) {
			if (metadata == null) {
				metadata = new RecursiveMetadata(this.getClass().getSimpleName(), desiredObject.getComparisonHint(), this.getComparisonHint());
			}

			AssertableObject actualObject = this;

			boolean result = true;

			Set<Field> fields = new HashSet<>(Arrays.asList(actualObject.getClass().getDeclaredFields()));
			fields.addAll(Arrays.asList(actualObject.getClass().getFields()));


			for (Field field : fields) {
				String fieldName = field.getName();
				RecursiveMetadata fieldMetadata = metadata.createChild(fieldName, null, null);

				try {
					Object expectedFieldObject = determineResultantValue(fieldMetadata, field.get(desiredObject), expectationOverrides);
					Object actualFieldObject = field.get(this);

					if (EXCEPTION_IGNORE_VALUE.equals(expectedFieldObject)) {
						if (logger.isTraceEnabled()) {
							logger.trace("Ignoring value for " + fieldMetadata.parameterName + "!");
						}
						continue;
					}

					if (EXCEPTION_NOT_NULL.equals(expectedFieldObject)) {
						if(actualFieldObject == null) {
							Assert.assertRecursiveFailure(fieldMetadata, "The expected value cannot be null!");
						}
						continue;
					}

					if (expectedFieldObject == null && actualFieldObject != null) {
						Assert.assertRecursiveFailure(fieldMetadata, null, actualFieldObject.toString());

					} else if (expectedFieldObject != null && actualFieldObject == null) {
						Assert.assertRecursiveFailure(fieldMetadata, expectedFieldObject.toString(), null);

					} else if (!field.isAnnotationPresent(OmitFromEqualsAssertion.class)) {
						Type fieldType = field.getGenericType();

						if (fieldType instanceof ParameterizedType) {

							ParameterizedType ptype = (ParameterizedType) fieldType;

							Type[] typeArguments = ptype.getActualTypeArguments();
							if (typeArguments.length != 1) {
								throw new RuntimeException("Type argument lengths other than one are not currently supported for comparison!");
							}

							Type typeArgument = typeArguments[0];
							Type rawType = ptype.getRawType();

							if (rawType == TreeSet.class) {
								if (typeArgument == String.class) {
									result = result && assertSetsEqual(fieldMetadata, (TreeSet) expectedFieldObject,
											(TreeSet) actualFieldObject, expectationOverrides);

								} else if (AssertableObject.class.isAssignableFrom(MissionModels.class.getClassLoader().loadClass(typeArgument.getTypeName()))) {
									result = result && assertSetsEqual(fieldMetadata, (TreeSet) expectedFieldObject,
											(TreeSet) actualFieldObject, expectationOverrides);

								} else {
									throw new RuntimeException("Generic type " + typeArgument + " not currently supported for comparison!");
								}

							} else if(rawType instanceof Class && ((Class<?>)rawType).isAssignableFrom(List.class)) {
								if (typeArgument == String.class) {
									result = result && assertListsEqual(fieldMetadata, (List) expectedFieldObject,
											(List) actualFieldObject, expectationOverrides);

								} else if (AssertableObject.class.isAssignableFrom(MissionModels.class.getClassLoader().loadClass(typeArgument.getTypeName()))) {
									result = result && assertListsEqual(fieldMetadata, (List) expectedFieldObject,
											(List) actualFieldObject, expectationOverrides);

								} else {
									throw new RuntimeException("Generic type " + typeArgument + " not currently supported for comparison!");
								}

							} else {
								throw new RuntimeException("Type " + rawType + " not currently supported for comparison!");
							}
						} else {
							if (fieldType == String.class || (
									fieldType == Object.class && actualFieldObject != null && actualFieldObject instanceof String)) {
								result = result && assertStringsEqual(fieldMetadata, (String) expectedFieldObject, (String) actualFieldObject);

							} else if (isPrimitiveType(fieldType)) {
								result = result && assertPrimitivesEqual(fieldMetadata, expectedFieldObject, actualFieldObject);

							} else if ((AssertableObject.class.isAssignableFrom(MissionModels.class.getClassLoader().loadClass(fieldType.getTypeName()))) ||
									(actualFieldObject != null && AssertableObject.class.isAssignableFrom(actualFieldObject.getClass()))) {

								if (actualFieldObject == null) {
									if (expectedFieldObject != null) {
										result = false;
										Assert.assertRecursiveFailure(fieldMetadata, expectedFieldObject.toString(), null);
									}

								} else {
									result = result && ((AssertableObject) actualFieldObject).assertMatchesExpectation(
											(AssertableObject) expectedFieldObject, fieldMetadata, expectationOverrides);
								}

							} else if(fieldType instanceof Class && ((Class<?>)fieldType).isEnum()) {
								assertEnumsEqual(fieldMetadata, expectedFieldObject, actualFieldObject);

							} else {

								Assert.assertRecursiveFailure(fieldMetadata, "Unsupported type " + fieldType + "!");
							}
						}

					}
				} catch (Exception e) {
					Assert.assertRecursiveException(fieldMetadata, e);
					throw new RuntimeException(e);
				}
			}
			return result;
		}

		/**
		 * Used to set a hint regarding the origin of a piece of data to be used when debugging
		 * @param hint The hint
		 * @return This object
		 */
		public final AssertableObject<T> setComparisonHint(@NotNull String hint) {
			_comparisonHint = hint;
			return this;
		}

		public final String getComparisonHint() {
			return _comparisonHint;
		}

		/**
		 * The intent of this is to be able to compare POJOs constructed or received from different server APIs and
		 * determine if they are equivalent, and suitable for comparision of the details. Ideally it should be user-readable.
		 */
		public abstract String getUniqueStableName();

		public abstract T getThis();
	}

	public static class EnterpriseSyncUploadResponse {
		public String UID;
		public String SubmissionDateTime;
		public String MIMEType;
		public String SubmissionUser;
		public String PrimaryKey;
		public String Hash;
		public String Name;
	}

	public static class ApiSingleResponse<T> {
		// Successful response values
		public String version;
		public String type;
		public T data;
		public String nodeId;

		// Error response values
		public String status;
		public Integer code;
		public String message;

		public ApiSingleResponse() {
		}

		public T getData() {
			return data;
		}
	}

	public static class ResponseWrapper<T> {
		public final Integer responseCode;
		public final String httpErrorBody;
		public final T body;

		private Collection<Mission> getMissions() {
			Set<Mission> missions = new HashSet<>();
			if (body != null) {
				if (body instanceof ApiSingleResponse &&
						((ApiSingleResponse) body).data != null &&
						((ApiSingleResponse) body).data instanceof Mission) {
					missions.add((Mission) ((ApiSingleResponse) body).data);

				} else if (body instanceof ApiSetResponse &&
						((ApiSetResponse) body).data != null &&
						((ApiSetResponse) body).data.size() > 0) {

					for (Object obj : ((ApiSetResponse) body).data) {
						if (obj instanceof Mission) {
							missions.add((Mission) obj);
						}
					}

				} else if (body instanceof ApiListResponse &&
						((ApiListResponse) body).data != null &&
						((ApiListResponse) body).data.size() > 0) {

					for (Object obj : ((ApiListResponse) body).data) {
						if (obj instanceof Mission) {
							missions.add((Mission) obj);
						}
					}
				}
			}
			return missions;
		}

		public void setMissionComparisonHintIfPossible(@NotNull String comparisonHint) {
			for (Mission mission : getMissions()) {
				mission.setComparisonHint(comparisonHint);
			}
		}

		public void overrideMissionGroupsIfPossible(@NotNull TreeSet<String> groups) {
			for (Mission mission : getMissions()) {
				mission.overrideGroups(new TreeSet<>(groups));
			}
		}

		public void overrideMissionGroupsIfPossible(@NotNull ResponseWrapper source) {
			Mission sourceMission = null;

			if (source.body != null) {
				if (source.body instanceof ApiSingleResponse &&
						((ApiSingleResponse) source.body).data != null &&
						((ApiSingleResponse) source.body).data instanceof Mission) {
					sourceMission = (Mission) ((ApiSingleResponse) source.body).data;

				} else if (source.body instanceof ApiSetResponse &&
						((ApiSetResponse) source.body).data != null &&
						((ApiSetResponse) source.body).data.size() > 0) {
					Object obj = ((ApiSetResponse) source.body).data.stream().findFirst();
					if (obj instanceof Mission) {
						sourceMission = (Mission) obj;
					}
				}
			}

			if (sourceMission != null) {
				this.overrideMissionGroupsIfPossible(sourceMission.getGroups());
			}
		}

		public ResponseWrapper(int responseCode, @Nullable T body, @Nullable String httpErrorBody) {
			this.responseCode = responseCode;
			this.body = body;
			this.httpErrorBody = httpErrorBody;
		}
	}

	public static class ApiSetResponse<T> {

		// Successful response values
		public String version;
		public String type;
		public final Set<T> data = new HashSet<>();
		public String nodeId;

		// Error response values
		public String status;
		public Integer code;
		public String message;

		public ApiSetResponse() {
		}

		public TreeSet<Mission> getMissions() {
			return (TreeSet<Mission>) data;
		}

		public TreeSet<MissionChange> getMissionChanges() {
			return (TreeSet<MissionChange>) data;
		}

		public Set<T> getData() {
			return data;
		}
	}

	public static class ApiListResponse<T> {

		// Successful response values
		public String version;
		public String type;
		public final LinkedList<T> data = new LinkedList<>();
		public String nodeId;

		// Error response values
		public String status;
		public Integer code;
		public String message;

		public ApiListResponse() {
		}

		public List<Mission> getMissions() {
			return (List<Mission>) data;
		}

		public List<MissionChange> getMissionChanges() {
			return (List<MissionChange>) data;
		}

		public List<T> getData() {
			return data;
		}
	}

	public static class Coordinates extends AssertableObject<Coordinates> {
		public float lat;
		public float lon;

		@Override
		public Coordinates getThis() {
			return this;
		}

		@Override
		public String getUniqueStableName() {
			return "(" + lat + "," + lon + ")";
		}

		@Override
		public int compareTo(@NotNull Coordinates o) {
			return getUniqueStableName().compareTo(o.getUniqueStableName());
		}

		public String toString() {
			return getUniqueStableName();
		}
	}

	public enum MissionUserPermission {
		MISSION_READ,           // Can read all mission data
		MISSION_WRITE,          // Can read and write mission data
		MISSION_DELETE,         // Can read, write, and delete mission data
		MISSION_SET_ROLE,       // Can set user roles
		MISSION_SET_PASSWORD,   // Can set the mission password
		MISSION_UPDATE_GROUPS,  // Can update the mission groups
		MISSION_MANAGE_FEEDS,   // TODO: Add tests?
		MISSION_MANAGE_LAYERS	// TODO: Add tests?
	}

	// Server Admin: Default owner role for everything.
	public enum MissionUserRole {
		MISSION_OWNER(MissionUserPermission.MISSION_READ,
				MissionUserPermission.MISSION_WRITE,
				MissionUserPermission.MISSION_DELETE,
				MissionUserPermission.MISSION_SET_ROLE,
				MissionUserPermission.MISSION_SET_PASSWORD,
				MissionUserPermission.MISSION_UPDATE_GROUPS),
		MISSION_SUBSCRIBER(MissionUserPermission.MISSION_READ, MissionUserPermission.MISSION_WRITE),
		MISSION_READONLY_SUBSCRIBER(MissionUserPermission.MISSION_READ);

		private final Set<MissionUserPermission> missionUserPermissions;

		MissionUserRole(@NotNull MissionUserPermission... permissions) {
			this.missionUserPermissions = new HashSet<>(Arrays.asList(permissions));
		}

		public boolean hasPermission(MissionUserPermission permission) {
			return missionUserPermissions.contains(permission);
		}
	}

	public static class MissionRole {
		public MissionUserRole type;
		public final TreeSet<MissionUserPermission> permissions = new TreeSet<>();
	}


	public static class Resource extends AssertableObject<Resource> {
		protected String filename;
		protected TreeSet<String> keywords;
		protected String mimeType;
		protected String contentType;
		protected String name;
		protected String submissionTime;
		protected String submitter;
		protected String uid;
		protected String creatorUid;
		protected String hash;
		protected Long size;
		protected String tool;
		protected Double latitude;
		protected Double longitude;
		protected Double altitude;

		public String getContentHash() {
			return hash;
		}

		@Override
		public String getUniqueStableName() {
			return hash + contentType + submitter + uid;
		}

		@Override
		public Resource getThis() {
			return this;
		}

		@Override
		public int compareTo(@NotNull Resource o) {
			if (hash != null) {
				return this.hash.compareTo(o.hash);
			} else {
				return this.getUniqueStableName().compareTo(o.getUniqueStableName());
			}
		}

		public Resource clone() {
			return gson.fromJson(gson.toJson(this), Resource.class);
		}
	}

	public static class MissionContentDataDetails extends AssertableObject<MissionContentDataDetails> {
		public String type;
		public String callsign;
		public Coordinates location;

		@Override
		public String getUniqueStableName() {
			return callsign + type + location;
		}

		@Override
		public MissionContentDataDetails getThis() {
			return this;
		}

		public String toString() {
			return getUniqueStableName();
		}

		@Override
		public int compareTo(@NotNull MissionContentDataDetails o) {
			return this.getUniqueStableName().compareTo(o.getUniqueStableName());
		}
	}

	public static class MissionContentDataContainer extends AssertableObject<MissionContentDataContainer> {
		public Object data;
		public String timestamp;
		public String creatorUid;
		public MissionContentDataDetails details;

		public String getDataAsString() {
			return (String) data;
		}

		public Resource getDataAsMissionContent() {
			Resource result = null;
			if (data != null) {
				if (data instanceof Resource) {
					result = (Resource) data;
				} else {
					Assert.fail("The data property should be MissionContentData but it is " + data.getClass().getName() + "!");
				}
			}
			return result;
		}

		@Override
		public String toString() {
			return gson.toJson(this);
		}

		@Override
		public String getUniqueStableName() {
			// TODO: Make more unique
			return data.toString();
		}

		@Override
		public MissionContentDataContainer getThis() {
			return this;
		}

		@Override
		public int compareTo(@NotNull MissionContentDataContainer o) {
			return getUniqueStableName().compareTo(o.getUniqueStableName());
		}

		public static MissionContentDataContainer fromSentCotDocument(Document document) {
//			try {
//				XMLWriter xmlWriter = new XMLWriter(new OutputStreamWriter(System.err), OutputFormat.createPrettyPrint());
//				System.err.println("SENT COT XML:");
//				xmlWriter.write(document);
//				xmlWriter.flush();
//				System.err.println("\n\n\n");
//			} catch (Exception e) {
//				throw new RuntimeException(e);
//			}
			MissionContentDataContainer container = new MissionContentDataContainer();
			Element event = document.getRootElement();
			Element point = event.element("point");

			container.timestamp = event.attributeValue("time");
			container.creatorUid = event.attributeValue("uid");
			container.details = new MissionContentDataDetails();
			container.data = container.creatorUid;
			container.details.callsign = event.element("detail").element("contact").attributeValue("callsign");
			container.details.type = event.attributeValue("type");
			container.details.location = new Coordinates();
			container.details.location.lat = Float.parseFloat(point.attributeValue("lat"));
			container.details.location.lon = Float.parseFloat(point.attributeValue("lon"));

//			System.err.println("SENT COT CONTAINER:\n" + new GsonBuilder().setPrettyPrinting().create().toJson(container) + "\n\n\n");
			return container;
		}

		public MissionChange toMissionChangeAddition(@NotNull String missionName) {
			Resource missionContentData = (Resource) data;
			MissionChange result = new MissionChange();

			result.missionName = missionName;
			result.timestamp = timestamp;
			result.type = MissionChangeType.ADD_CONTENT;
			result.creatorUid = creatorUid;
			result.contentResource = missionContentData.clone();

			return result;
		}

		public MissionChange toMissionChangeRemoval(@NotNull String missionName) {
			MissionChange result = new MissionChange();

			result.missionName = missionName;
			result.type = MissionChangeType.ADD_CONTENT;
			result.creatorUid = creatorUid;

			if (data != null && data instanceof Resource) {
				Resource resource = new Resource();
				result.contentResource = resource;

				Resource mcd = (Resource) data;

				resource.mimeType = mcd.mimeType;
				resource.name = mcd.name;
				resource.submissionTime = mcd.submissionTime;
				resource.submitter = mcd.submitter;
				resource.uid = mcd.uid;
				resource.creatorUid = mcd.creatorUid;
				resource.hash = mcd.hash;
				resource.size = mcd.size;
				resource.keywords = mcd.keywords;
			}

			if (details != null && details instanceof MissionContentDataDetails) {
				UidDetails missionChangeDetails = new UidDetails();
				result.details = missionChangeDetails;
				missionChangeDetails.callsign = details.callsign;
				missionChangeDetails.type = details.type;
				missionChangeDetails.location = new Coordinates();
				missionChangeDetails.location.lat = details.location.lat;
				missionChangeDetails.location.lon = details.location.lon;
			}
			return result;
		}
	}

	public static class SubscriptionRoleData extends AssertableObject<SubscriptionRoleData> {

		public String clientUid;
		public MissionRole role;

		@Override
		public String getUniqueStableName() {
			return clientUid + "(" + role + ")";
		}

		@Override
		public SubscriptionRoleData getThis() {
			return this;
		}

		@Override
		public int compareTo(@NotNull SubscriptionRoleData o) {
			return this.getUniqueStableName().compareTo(o.getUniqueStableName());
		}
	}

	public static class SubscriptionData extends AssertableObject<SubscriptionData> {

		protected String token;
		protected String clientUid;
		protected String createTime;
		protected MissionRole role;

		public SubscriptionData cloneWithNewRole(@NotNull MissionUserRole newRole) {
			SubscriptionData result = new SubscriptionData();
			result.token = token;
			result.clientUid = clientUid;
			result.createTime = createTime;
			result.role = new MissionRole();
			result.role.type = newRole;
			return result;
		}

		public String getToken() {
			return token;
		}

		public String getClientUid() {
			return clientUid;
		}

		public String getCreateTime() {
			// TODO Missions: Validate proper creation times for everything
			return createTime;
		}

		@Override
		public String getUniqueStableName() {
			return "subscription-" + clientUid;
		}

		@Override
		public SubscriptionData getThis() {
			return this;
		}

		@Override
		public int compareTo(@NotNull SubscriptionData o) {
			return this.getUniqueStableName().compareTo(o.getUniqueStableName());
		}

		public MissionUserRole getRole() {
			return role.type;
		}
	}

	public static class Mission extends AssertableObject<Mission> {
		protected String name;
		protected String description;
		protected String chatRoom;
		protected String tool;
		protected final TreeSet<String> keywords = new TreeSet<>();
		protected String creatorUid;
		protected String createTime;
//		protected JsonElement externalData;

		protected TreeSet<String> groups = new TreeSet<>();
		//		protected MissionRole ownerRole;
		protected TreeSet<MissionContentDataContainer> uids;
		protected TreeSet<MissionContentDataContainer> contents;
		protected boolean passwordProtected;

		public TreeSet<MissionContentDataContainer> getUids() {
			return uids;
		}

		public final TreeSet<String> getKeywords() {
			return new TreeSet<>(keywords);
		}

		@Override
		public String toString() {
			return gson.toJson(this);
		}

		public Mission setName(@NotNull String name) {
			this.name = name;
			return this;
		}

		public String getCreateTime() {
			return createTime;
		}

		public boolean isPasswordProtected() {
			return passwordProtected;
		}

		public void setPasswordProtected(boolean isPasswordProtected) {
			passwordProtected = isPasswordProtected;
		}

		public String getCreatorUid() {
			return creatorUid;
		}

		@Override
		public String getUniqueStableName() {
			return name;
		}

		@Override
		public Mission getThis() {
			return this;
		}

		public TreeSet<MissionContentDataContainer> getContents() {
			return contents;
		}

		public Mission setDescription(@NotNull String description) {
			this.description = description;
			return this;
		}

		public Mission setChatRoom(@NotNull String chatRoom) {
			this.chatRoom = chatRoom;
			return this;
		}

		public synchronized Mission setKeywords(@NotNull String... keywords) {
			this.keywords.clear();
			this.keywords.addAll(Arrays.asList(keywords));
			return this;
		}

		public synchronized Mission clearKeywords() {
			this.keywords.clear();
			return this;
		}

		public Mission setTool(@NotNull String tool) {
			this.tool = tool;
			return this;
		}

		public synchronized void addUidData(@NotNull MissionContentDataContainer uidData) {
			// TODO Missions: Affirm what is the primary key on the server
			// TODO Missions: Deconflict non-cot-tracks?
			if (uidData.data instanceof String) {
				Optional<MissionContentDataContainer> result = uids.stream().filter(x -> x.getDataAsString().equals(uidData.getDataAsString())).findFirst();
				result.ifPresent(missionContentDataContainer -> uids.remove(missionContentDataContainer));
			}
			uids.add(uidData);
		}

		public TreeSet<String> getGroups() {
			return new TreeSet<>(this.groups);
		}

		public void overrideGroups(TreeSet<String> groups) {
			if (!TestExceptions.MISSION_IGNORE_GROUPS_MISSING_IN_ADD_REMOVE_RESPONSES) {
				throw new RuntimeException("This should only be done as an override for a specific bug!!");
			}
			this.groups = new TreeSet<>(groups);
		}

		public void update(@NotNull Mission mission) {
			// TODO Missions: Implement
		}

		@Override
		public int compareTo(@NotNull Mission o) {
			return name.compareTo(o.name);
		}
	}

	public enum MissionChangeType {
		CREATE_MISSION,
		DELETE_MISSION,
		ADD_CONTENT,
		REMOVE_CONTENT
	}

	// TODO Missions: Is this MissionContentDataDetails?
	public static class UidDetails extends AssertableObject<UidDetails> {

		public UidDetails() {

		}

		public String type;
		public String callsign;
		public String title;
		public String iconsetPath;
		public String color;
		public List<String> attachments;
		public String name;
		public String category;
		public Coordinates location;

		@Override
		public String getUniqueStableName() {
			return "UidDetails(" + keyValueDisplayConverter("type", type, "callsign", callsign, "title", title, "name", name) + ")";
		}

		public String toString() {
			return getUniqueStableName();
		}

		@Override
		public UidDetails getThis() {
			return this;
		}

		@Override
		public int compareTo(@NotNull UidDetails o) {
			return 0;
		}
	}

	public static class LogEntry extends AssertableObject<LogEntry> {
		protected String id;
		protected String content;
		protected String creatorUid;
		protected String entryUid;
		protected Set<String> missionNames;
		protected String servertime;
		protected String dtg;
		protected String created;
		protected Set<String> contentHashes;
		protected Set<String> keywords;

		@Override
		public String getUniqueStableName() {
			return "LogEntry(" + keyValueDisplayConverter("entryUid", entryUid) + ")";
		}

		@Override
		public LogEntry getThis() {
			return this;
		}

		@Override
		public int compareTo(@NotNull LogEntry o) {
			return this.getUniqueStableName().compareTo(o.getUniqueStableName());
		}
	}

	public static class MissionChange extends AssertableObject<MissionChange> {
		public Resource contentResource;
		public String contentUid;
		public String creatorUid;
		public UidDetails details;
		// externalData?
		public LogEntry logEntry;
		public String missionName;
		public String serverTime;
		public String timestamp;
		public MissionChangeType type;

		@Override
		public String getUniqueStableName() {
			String name = type + missionName + creatorUid;

			if (contentResource != null) {
				name = name + contentResource.hash;
			}

			if (details != null) {
				name = name + details.callsign + timestamp;
			}

			if (logEntry != null) {
				name = name + logEntry.entryUid;
			}

			return name;
		}

		@Override
		public String toString() {
			return "(" + keyValueDisplayConverter("missionName", missionName, "type", type, "contentResource", contentResource) + ")";
		}

		@Override
		public MissionChange getThis() {
			return this;
		}

		@Override
		public int compareTo(@NotNull MissionChange o) {
			return this.getUniqueStableName().compareTo(o.getUniqueStableName());
		}

		public static MissionChange fromSentCotDocument(Document document) {
			MissionChange missionChange = new MissionChange();
			Element xmlEvent = document.getRootElement();
			Element xmlPoint = xmlEvent.element("point");
			Element xmlDetail = xmlEvent.element("detail");
			Element xmlContact = xmlDetail.element("contact");
			Element xmlDest = xmlDetail.element("marti").element("dest");

			missionChange.missionName = xmlDest.attributeValue("mission");
			missionChange.type = MissionChangeType.ADD_CONTENT;
			missionChange.contentUid = xmlEvent.attributeValue("uid");
			missionChange.creatorUid = xmlEvent.attributeValue("uid");
			missionChange.details = new UidDetails();
			missionChange.details.type = xmlEvent.attributeValue("type");
			missionChange.details.callsign = xmlContact.attributeValue("callsign");
			missionChange.details.location = new Coordinates();
			missionChange.details.location.lat = Float.parseFloat(xmlPoint.attributeValue("lat"));
			missionChange.details.location.lon = Float.parseFloat(xmlPoint.attributeValue("lon"));

			return missionChange;
		}

		public MissionChange obsoleteContentResourceAndProduceDeletionStatement(@Nullable String creatorUid) {
			MissionChange result = new MissionChange();
			result.contentResource = contentResource;
			contentResource = null;

			result.missionName = missionName;
			result.creatorUid = creatorUid;
			result.type = MissionChangeType.REMOVE_CONTENT;

			return result;
		}
	}

	public static class PutMissionContents {
		private TreeSet<String> hashes; // = new HashSet<>();
		private TreeSet<String> uids; // = new HashSet<>();

		public PutMissionContents() {
		}

		public synchronized PutMissionContents addHashes(String... hashes) {
			if (this.hashes == null) {
				this.hashes = new TreeSet<>();
			}
			this.hashes.addAll(Arrays.asList(hashes));
			return this;
		}

		public synchronized PutMissionContents addUids(String... uids) {
			if (this.uids == null) {
				this.uids = new TreeSet<>();
			}
			this.uids.addAll(Arrays.asList(uids));
			return this;
		}
	}
}
