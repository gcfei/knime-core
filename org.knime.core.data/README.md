### DESIGN REQUIREMENTS (besides interface-driven, composition over inheritence, modularity)
- Enable (extensible) shared memory for python / java
- API for proxy-types
- wide-table support
- backwards-compatibility
- ultra-lightweight processing. Any processing on KNIME side should be much faster than any TableIO.
- Predicate push-down and Filter-API

## TODOs

### API design of org.knime.data.store
- [ ] MB: Test Life-cycle management (close() vs. destroy() vs. ... finishWriting() 'can I read before I've serialized the entire table')
- [ ] MB: Thread-safety & Cache (multi-read, multi-write?, cache, ref counting  -> check how does arrow do it?).
- [X] CD: MultiVecValue & Custom data types (e.g. Date&Time, Text, Struct, PNG Images). 
- [X] CD: Fix arrow writer/reader
- [ ] CD: NumericDomain (ordered list, per chunk, accessible before loading actual data)
- [ ] CD: Benchmark and support wide-data
- [ ] Nominal domain and fixed dictionary Encoding for Strings (encoding decision through spec by user)
- [ ] Implement remaining basic types Long, Int, Float,...
- [ ] Missing value cause
- [ ] Complex data types (DataCell)
--------------
- [ ] FileStoreCell, BlobCell
- [ ] More types: DictType for categorical variables, NativeStructs?, Collections, ...
- [ ] Exception Handling & Logging
- [ ] Concept for pre-buffering in native types or completely skip arrow (performance loss with non-primitives)
- [ ] Avoid data-copying in case of RowFilter (with efficient copy -> chunk-wise copy or so...). Create new table which only fulfills condition.
- [ ] Support for DataCellserializers. Forseeable problem: avoid constant serialization and deserialization into byte[]
- [ ] Native -> Arrow layering?
- [ ] Parquet Backend (either adapt Marc's implementation to be KNIME independent or https://github.com/apache/arrow/pull/5719 or https://github.com/apache/parquet-mr/tree/master/parquet-arrow).
- [ ] Add TableFilter API (limit number of read rows from disc / limit number of read columns from disc)
- [ ] Try to access some data from python (Davin/Marcel -> shared memory, shared jni)
- [ ] Domain Calculation
- [ ] Pre-fetching / pre-writing (async)
- [ ] Serialization - how do I restore state of a store or rather entire table (use-case: (i) knime has stored store or (ii) store created without prior writing).
- [ ] DuplicateChecker for RowId

### KNIME Integration
- [ ] Support for FileStores / BlobStores
- [ ] CollectionCells
- [ ] Off heap memory management

### Nice-to-haves
- [ ] Test idea: with intermediate buffers
- [ ] Wide-table support (e.g. automatically wrap K-consecutive columns for doubles into a double[] behind the scenes.
- [ ] Use framework for streaming (NB: Nearly support read while write already today).
- [ ] user facing API improvements (see tests)