/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator.packaging;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** The deployment kit bundled with this system (module folder kit/, on the classpath under kit/): relative path -> content. */
public final class KitResources {

	private KitResources() {
	}

	public static Map<String, byte[]> load() throws IOException {
		final Map<String, byte[]> out = new TreeMap<>();
		for (final Resource r : new PathMatchingResourcePatternResolver().getResources("classpath*:kit/**")) {
			if (!r.isReadable()) {
				continue;
			}
			final String url = r.getURL().toString();
			final int at = url.lastIndexOf("/kit/");
			if (at < 0 || url.endsWith("/")) {
				continue;
			}
			final String path = url.substring(at + "/kit/".length());
			if (path.isEmpty() || path.contains("..") || path.endsWith(".DS_Store")) {
				continue;
			}
			try (InputStream in = r.getInputStream()) {
				out.put(path, in.readAllBytes());
			}
		}
		if (out.isEmpty()) {
			throw new IOException("the deployment kit is missing from the classpath (kit/)");
		}
		return out;
	}
}
