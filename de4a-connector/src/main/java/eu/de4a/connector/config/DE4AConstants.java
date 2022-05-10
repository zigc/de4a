package eu.de4a.connector.config;

import javax.annotation.concurrent.Immutable;

@Immutable
public final class DE4AConstants {
  public static final String XPATH_REQUEST_ID="//*[local-name()='RequestId']/text()";

	//Processes identifiers
	public static final String PROCESS_ID_REQUEST = "request";
	public static final String PROCESS_ID_RESPONSE = "response";
	public static final String PROCESS_ID_NOTIFICATION = "notification";

	// DE4A Document types Values
	public static final String MULTI_ITEM_TYPE = "MultiItem";

	private DE4AConstants (){}
}