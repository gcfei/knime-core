package org.knime.core.data.preproc;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.column.ColumnChunk;
import org.knime.core.data.column.ColumnType;
import org.knime.core.data.domain.Domain;
import org.knime.core.data.domain.DomainCalculator;
import org.knime.core.data.row.RowBatch;
import org.knime.core.data.row.RowBatchFactory;
import org.knime.core.data.row.RowBatchReader;
import org.knime.core.data.row.RowBatchReaderConfig;
import org.knime.core.data.row.RowBatchWriter;
import org.knime.core.data.table.store.TableStore;
import org.knime.core.data.type.DoubleDomainCalculator;
import org.knime.core.data.type.DoubleType;
import org.knime.core.data.type.StringDomainCalculator;

// TODO generalize to arbitrary pre-processors with mergable results
public class PreprocessingTableStore implements TableStore {

	private final TableStore m_delegate;
	private final ColumnType<?, ?>[] m_columnTypes;
	private final int[] m_enabled;
	private final Map<Integer, Domain> m_results;

	private Map<ColumnType<?, ?>, DomainCalculator<?, ?>> m_calculators = new HashMap<>();
	// TODO make extensible!!
	// TODO only put 'active' domains in the calculator map (e.g. in constructor)
	{
		m_calculators.put(DoubleType.INSTANCE, new DoubleDomainCalculator());
		// TODO make threshold configurable
		m_calculators.put(DoubleType.INSTANCE, new StringDomainCalculator(120));
	}

	public PreprocessingTableStore(TableStore delegate, PreProcessingConfig config) {
		m_delegate = delegate;
		m_columnTypes = delegate.getColumnTypes();
		m_enabled = config.getDomainEnabledIndices();
		m_results = new HashMap<Integer, Domain>();
	}

	@Override
	public RowBatchWriter getWriter() {
		final RowBatchWriter writer = m_delegate.getWriter();
		return new RowBatchWriter() {

			@Override
			public void close() throws Exception {
				// TODO here we have to synchronize
				writer.close();
			}

			@Override
			public void write(RowBatch record) {
				// TODO parallelize over columns or batches or both or selectable?
				final ColumnChunk[] recordData = record.getRecordData();
				for (int i = 0; i < m_enabled.length; i++) {
					@SuppressWarnings("unchecked")
					DomainCalculator<ColumnChunk, Domain> domainCalculator = (DomainCalculator<ColumnChunk, Domain>) m_calculators
							.get(m_columnTypes[m_enabled[i]]);

					// TODO store and aggregate the results somewhere...
					Domain result = domainCalculator.apply(recordData[m_enabled[i]]);
					Domain stored = m_results.get(m_enabled[i]);
					if (stored == null) {
						m_results.put(m_enabled[i], result);
					} else {
						// TODO needs to be synchronized 
						m_results.put(m_enabled[i], domainCalculator.merge(result, stored));
					}
				}

				/*
				 * TODO directly write column chunks to cache instead of record batch (Introduce
				 * 'ColumnStoreInterface' and make a single 'instance of' check in constructor)?
				 * Before we do that we need to investigate the performance benefits of that.
				 */
				writer.write(record);
			}

		};
	}

	@Override
	public RowBatchReader createReader(RowBatchReaderConfig config) {
		return m_delegate.createReader(config);
	}

	@Override
	public void close() throws Exception {
		m_delegate.close();
	}

	@Override
	public ColumnType<?, ?>[] getColumnTypes() {
		return m_delegate.getColumnTypes();
	}

	@Override
	public RowBatchFactory createFactory() {
		return m_delegate.createFactory();
	}

}
