package org.knime.core.data.type;

import java.util.Collections;
import java.util.Set;

import org.knime.core.data.domain.Domain;

public class StringDomain implements Domain {

	private Set<String> m_values;

	public StringDomain(Set<String> values, long numMissing, long numNonMissing) {
		m_values = values;
	}

	@Override
	public long getNumMissing() {
		return 0;
	}

	@Override
	public long getNumNonMissing() {
		return 0;
	}

	public Set<String> getValues() {
		return m_values != null ? Collections.unmodifiableSet(m_values) : null;
	}

	public boolean hasValues() {
		return m_values != null;
	}

}
