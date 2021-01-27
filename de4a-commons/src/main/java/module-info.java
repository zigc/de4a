module de4acommons {
	exports eu.de4a.conn.api.as4;
	exports eu.de4a.exception;
	exports eu.de4a.conn.owner;
	exports eu.de4a.conn.api.requestor;
	exports eu.de4a.conn.api.canonical;
	exports eu.de4a.conn.api.rest;
	exports eu.de4a.conn.api.smp;
	exports eu.de4a.util;
	exports eu.de4a.conn.xml;

	requires eu.toop.connector.api;
	requires java.xml;
	requires java.xml.bind;
	requires org.apache.logging.log4j;
	requires org.apache.santuario.xmlsec;
}