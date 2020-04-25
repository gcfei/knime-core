package org.knime.core.data.arrow;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.complex.StructVector;
import org.apache.arrow.vector.types.pojo.ArrowType.Struct;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.knime.core.data.ChunkFactory;
import org.knime.core.data.column.ColumnChunk;
import org.knime.core.data.column.ColumnType;
import org.knime.core.data.column.struct.StructType;
import org.knime.core.data.row.DefaultRowBatch;
import org.knime.core.data.row.RowBatch;
import org.knime.core.data.row.RowBatchFactory;
import org.knime.core.data.type.DoubleType;
import org.knime.core.data.type.StringType;

public class ArrowRowBatchFactory implements RowBatchFactory {

	private final BufferAllocator m_allocator;
	private final ColumnType<?, ?>[] m_types;
	private final int m_chunkSize;
	private final ChunkFactory<FieldVectorChunk<?>>[] m_factories;

	// TODO move chunk size into create for dynamic chunk sizes
	public ArrowRowBatchFactory(ColumnType<?, ?>[] types, BufferAllocator allocator, int chunkSize) {
		m_types = types;
		m_chunkSize = chunkSize;
		m_allocator = allocator;
		m_factories = createColumns(m_types, false);
	}

	@Override
	public RowBatch create() {
		final ColumnChunk[] chunks = new ColumnChunk[m_factories.length];
		for (int i = 0; i < m_factories.length; i++) {
			chunks[i] = m_factories[i].create();
		}
		return new DefaultRowBatch(chunks);
	}

	@Override
	public ColumnType<?, ?>[] getColumnTypes() {
		return m_types;
	}

	// TODO vector naming (e.g. with TableSchema class...?)
	// creates nested structs recursively.
	private ChunkFactory<FieldVectorChunk<?>>[] createColumns(final ColumnType<?, ?>[] types, boolean hasParent) {
		@SuppressWarnings("unchecked")
		final ChunkFactory<FieldVectorChunk<?>>[] factories = new ChunkFactory[m_types.length];
		for (int i = 0; i < factories.length; i++) {
			if (types[i] instanceof DoubleType) {
				factories[i] = () -> allocateNew(new Float8VectorChunk(m_allocator), hasParent);
			} else if (m_types[i] instanceof StringType) {
				factories[i] = () -> allocateNew(new VarCharVectorChunk(m_allocator), hasParent);
			} else if (m_types[i] instanceof StructType) {
				final ChunkFactory<FieldVectorChunk<?>>[] childFactories = createColumns(
						((StructType) m_types[i]).getColumnTypes(), true);
				factories[i] = () -> {
					final ArrowStructVector structVector = new ArrowStructVector("StructVector", m_allocator);
					final FieldVectorChunk<?>[] childColumns = new FieldVectorChunk[childFactories.length];
					for (int j = 0; j < childFactories.length; j++) {
						childColumns[j] = childFactories[j].create();
						structVector.putVectorInternal("Child", childColumns[j].get());
					}
					return allocateNew(new StructVectorChunk(structVector, childColumns), hasParent);
				};
			}
		}
		return factories;

	}

	private <C extends FieldVectorChunk<?>> C allocateNew(C chunk, boolean hasParent) {
		if (!hasParent) {
			chunk.allocateNew(m_chunkSize);
		}
		return chunk;
	}

	// NB: just to access 'putVector' directly. Performance!
	private class ArrowStructVector extends StructVector {
		public ArrowStructVector(String name, BufferAllocator alloc) {
			super("Struct", alloc, FieldType.nullable(Struct.INSTANCE), null);
		}

		void putVectorInternal(String name, FieldVector vector) {
			putVector(name, vector);
		}
	}

}
