package org.knime.core.data.type;

import org.knime.core.data.domain.Domain;

public class DoubleDomain implements Domain {

	private final long m_numMissing;
	private final long m_numNonMissing;
	private final double m_minimum;
	private final double m_maximum;

	public DoubleDomain(long numMissing, long numNonMissing, double minimum, double maximum) {
		m_numMissing = numMissing;
		m_numNonMissing = numNonMissing;
		m_minimum = minimum;
		m_maximum = maximum;
	}

	@Override
	public long getNumMissing() {
		return m_numMissing;
	}

	@Override
	public long getNumNonMissing() {
		return m_numNonMissing;
	}

	public double getMinimum() {
		return m_minimum;
	}

	public double getMaximum() {
		return m_maximum;
	}

}
