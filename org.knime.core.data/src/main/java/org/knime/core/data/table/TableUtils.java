package org.knime.core.data.table;

import org.knime.core.data.column.ColumnType;
import org.knime.core.data.row.RowBatchReaderConfig;
import org.knime.core.data.row.RowBatchUtils;
import org.knime.core.data.row.RowReadCursor;
import org.knime.core.data.row.RowWriteCursor;
import org.knime.core.data.table.store.TableReadStore;
import org.knime.core.data.table.store.TableStore;

public class TableUtils {

	public static WriteTable createWriteTable(TableStore store) {
		return new WriteTable() {

			private final ColumnType<?, ?>[] m_types = store.getColumnTypes();
			private final RowWriteCursor m_cursor = new RowWriteCursor(store.createFactory(), store.getWriter(),
					RowBatchUtils.createAccess(m_types));

			@Override
			public int getNumColumns() {
				return m_types.length;
			}

			@Override
			public RowWriteCursor getCursor() {
				return m_cursor;
			}
		};
	}

	public static ReadTable createReadTable(TableReadStore store) {
		final ColumnType<?, ?>[] types = store.getColumnTypes();
		return new ReadTable() {

			@Override
			public long getNumColumns() {
				return types.length;
			}

			// TODO pass config here? config not constant over table I guess...
			@Override
			public RowReadCursor newCursor() {
				return new RowReadCursor(store.createReader(new RowBatchReaderConfig() {

					@Override
					public int[] getColumnIndices() {
						return null;
					}
				}), RowBatchUtils.createAccess(types));
			}
		};
	}

	public static ReadTable createReadTable(TableReadStore store, RowBatchReaderConfig config) {
		final ColumnType<?, ?>[] types = store.getColumnTypes();
		return new ReadTable() {

			@Override
			public long getNumColumns() {
				return types.length;
			}

			// TODO pass config here? config not constant over table I guess...
			@Override
			public RowReadCursor newCursor() {
				return new RowReadCursor(store.createReader(config), RowBatchUtils.createAccess(types));
			}
		};
	}
}