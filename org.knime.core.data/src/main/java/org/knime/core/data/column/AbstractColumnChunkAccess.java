package org.knime.core.data.column;

public abstract class AbstractColumnChunkAccess<C extends ColumnChunk> implements ColumnChunkAccess<C> {

	protected int m_index;
	protected C m_data;

	@Override
	public void load(C data) {
		m_data = data;
	}

	@Override
	public void fwd() {
		m_index++;
	}

	@Override
	public void reset() {
		m_index = -1;
	}
}