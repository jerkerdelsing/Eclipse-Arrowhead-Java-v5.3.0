/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.graphdbmanager.graphdb;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.graphdbmanager.GraphDBManagerConstants;
import jakarta.annotation.PreDestroy;

// Historian packs in an Ontotext GraphDB repository, reached over the RDF4J HTTP protocol. Every pack is a
// gdm:SenMLPack resource in the historian named graph; query values are bound, never concatenated into SPARQL.
@Component
public class GraphDBHistorianStore implements HistorianStore {

	public static final String NAMESPACE = "https://arrowhead.eu/ns/graphdbmanager#";
	public static final String HISTORIAN_GRAPH = "urn:arrowhead:graphdbmanager:historian";

	private static final String PREFIXES = "PREFIX gdm: <" + NAMESPACE + ">\n";
	private static final String FROM = "FROM <" + HISTORIAN_GRAPH + ">\n";
	private static final String QUERY_SYSTEMS = PREFIXES + "SELECT DISTINCT ?system " + FROM
			+ "WHERE { ?pack a gdm:SenMLPack ; gdm:systemName ?system } ORDER BY ?system";
	private static final String QUERY_SERVICES = PREFIXES + "SELECT DISTINCT ?service " + FROM
			+ "WHERE { ?pack a gdm:SenMLPack ; gdm:systemName ?system ; gdm:serviceName ?service } ORDER BY ?service";
	private static final String QUERY_RANGE = PREFIXES + "SELECT ?pack ?baseTime ?minTime ?maxTime ?payload " + FROM
			+ "WHERE { ?pack a gdm:SenMLPack ; gdm:systemName ?system ; gdm:serviceName ?service ; gdm:baseTime ?baseTime ;"
			+ " gdm:minTime ?minTime ; gdm:maxTime ?maxTime ; gdm:payload ?payload ."
			+ " FILTER(?maxTime >= ?from && ?minTime <= ?to) } ORDER BY DESC(?baseTime) LIMIT %d";

	private final Logger logger = LogManager.getLogger(getClass());
	private final HTTPRepository repository;
	private final ValueFactory vf;
	private final IRI graph;

	public GraphDBHistorianStore(
			@Value(GraphDBManagerConstants.$GRAPHDB_URL_WD) final String serverUrl,
			@Value(GraphDBManagerConstants.$GRAPHDB_REPOSITORY_WD) final String repositoryId,
			@Value(GraphDBManagerConstants.$GRAPHDB_USERNAME_WD) final String username,
			@Value(GraphDBManagerConstants.$GRAPHDB_PASSWORD_WD) final String password) {
		repository = new HTTPRepository(serverUrl, repositoryId);
		if (!Utilities.isEmpty(username)) {
			repository.setUsernameAndPassword(username, password);
		}
		repository.init();
		vf = repository.getValueFactory();
		graph = vf.createIRI(HISTORIAN_GRAPH);
		logger.info("Historian store: GraphDB repository {} at {}", repositoryId, serverUrl);
	}

	@PreDestroy
	public void close() {
		repository.shutDown();
	}

	@Override
	public void save(final HistorianPack pack) {
		final IRI packIri = vf.createIRI("urn:uuid:" + pack.id());
		try (RepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.add(packIri, RDF.TYPE, term("SenMLPack"), graph);
			connection.add(packIri, term("systemName"), vf.createLiteral(pack.systemName()), graph);
			connection.add(packIri, term("serviceName"), vf.createLiteral(pack.serviceName()), graph);
			connection.add(packIri, term("baseTime"), vf.createLiteral(pack.baseTime()), graph);
			connection.add(packIri, term("minTime"), vf.createLiteral(pack.minTime()), graph);
			connection.add(packIri, term("maxTime"), vf.createLiteral(pack.maxTime()), graph);
			connection.add(packIri, term("payload"), vf.createLiteral(pack.payload()), graph);
			connection.add(packIri, term("createdAt"), vf.createLiteral(Date.from(Instant.now())), graph);
			int index = 0;
			for (final HistorianPack.Measurement measurement : pack.measurements()) {
				final Resource node = vf.createIRI("urn:uuid:" + pack.id() + "#m" + index++);
				connection.add(packIri, term("measurement"), node, graph);
				connection.add(node, RDF.TYPE, term("Measurement"), graph);
				connection.add(node, term("time"), vf.createLiteral(measurement.time()), graph);
				if (measurement.name() != null) {
					connection.add(node, term("name"), vf.createLiteral(measurement.name()), graph);
				}
				if (measurement.unit() != null) {
					connection.add(node, term("unit"), vf.createLiteral(measurement.unit()), graph);
				}
				if (measurement.value() != null) {
					connection.add(node, term("value"), vf.createLiteral(measurement.value()), graph);
				}
				if (measurement.stringValue() != null) {
					connection.add(node, term("stringValue"), vf.createLiteral(measurement.stringValue()), graph);
				}
				if (measurement.booleanValue() != null) {
					connection.add(node, term("booleanValue"), vf.createLiteral(measurement.booleanValue()), graph);
				}
			}
			connection.commit();
		}
	}

	@Override
	public List<String> findDistinctSystemNames() {
		try (RepositoryConnection connection = repository.getConnection()) {
			return strings(connection.prepareTupleQuery(QueryLanguage.SPARQL, QUERY_SYSTEMS), "system");
		}
	}

	@Override
	public List<String> findDistinctServiceNames(final String systemName) {
		try (RepositoryConnection connection = repository.getConnection()) {
			final TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, QUERY_SERVICES);
			query.setBinding("system", vf.createLiteral(systemName));
			return strings(query, "service");
		}
	}

	@Override
	public List<HistorianPack> findInTimeRange(final String systemName, final String serviceName, final double from, final double to, final int limit) {
		try (RepositoryConnection connection = repository.getConnection()) {
			final TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, QUERY_RANGE.formatted(limit));
			query.setBinding("system", vf.createLiteral(systemName));
			query.setBinding("service", vf.createLiteral(serviceName));
			query.setBinding("from", vf.createLiteral(from));
			query.setBinding("to", vf.createLiteral(to));
			final List<HistorianPack> result = new ArrayList<>();
			try (TupleQueryResult rows = query.evaluate()) {
				for (final BindingSet row : rows) {
					final String packIri = row.getValue("pack").stringValue();
					result.add(new HistorianPack(
							UUID.fromString(packIri.substring("urn:uuid:".length())),
							systemName,
							serviceName,
							Double.parseDouble(row.getValue("baseTime").stringValue()),
							Double.parseDouble(row.getValue("minTime").stringValue()),
							Double.parseDouble(row.getValue("maxTime").stringValue()),
							row.getValue("payload").stringValue(),
							List.of()));
				}
			}
			return result;
		}
	}

	private IRI term(final String localName) {
		return vf.createIRI(NAMESPACE, localName);
	}

	private List<String> strings(final TupleQuery query, final String variable) {
		final List<String> result = new ArrayList<>();
		try (TupleQueryResult rows = query.evaluate()) {
			for (final BindingSet row : rows) {
				result.add(row.getValue(variable).stringValue());
			}
		}
		return result;
	}
}
