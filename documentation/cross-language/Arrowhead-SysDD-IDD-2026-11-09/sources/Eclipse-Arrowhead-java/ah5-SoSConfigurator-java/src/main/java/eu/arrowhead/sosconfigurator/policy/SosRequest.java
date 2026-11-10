package eu.arrowhead.sosconfigurator.policy;

import java.util.List;

/**
 * What the operator asks for: the local clouds of the SoS and the inter-cloud service exchanges between them.
 *
 * @param name              name of the SoS (shown in plans and ColonyOS labels)
 * @param clouds            member clouds; a member without exchanges is released (no neighbours, no inter-cloud grants)
 * @param exchanges         consumer system of one member uses a service of a provider in another member
 * @param verifyDiscovery   also run a global service discovery from each consumer cloud over the relay
 */
public record SosRequest(String name, List<String> clouds, List<Exchange> exchanges, boolean verifyDiscovery) {

	public record Exchange(String consumerCloud, String consumer, String providerCloud, String provider, String service,
			List<String> operations) {
	}

	public List<Exchange> exchangesOrEmpty() {
		return exchanges == null ? List.of() : exchanges;
	}

	public List<String> cloudsOrEmpty() {
		return clouds == null ? List.of() : clouds;
	}
}
