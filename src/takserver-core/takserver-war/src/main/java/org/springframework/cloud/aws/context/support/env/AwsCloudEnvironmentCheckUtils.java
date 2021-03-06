/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.context.support.env;

import com.amazonaws.AmazonClientException;
import com.amazonaws.util.EC2MetadataUtils;

/**
 * @author Lukasz Luszczynski
 */
public final class AwsCloudEnvironmentCheckUtils {

	private static final String EC2_METADATA_ROOT = "/latest/meta-data";

	private static Boolean isCloudEnvironment;
	
	public static void setIsCloudEnvironment(Boolean isCloudEnvironment) {
		AwsCloudEnvironmentCheckUtils.isCloudEnvironment = isCloudEnvironment;
	}

	private AwsCloudEnvironmentCheckUtils() {
	}

	public static boolean isRunningOnCloudEnvironment() {
		
		if (isCloudEnvironment == null) {
			try {
				isCloudEnvironment = EC2MetadataUtils
						.getData(EC2_METADATA_ROOT + "/instance-id", 1) != null;
			}
			catch (AmazonClientException e) {
				isCloudEnvironment = false;
			}
		}
		return isCloudEnvironment;
	}

}
