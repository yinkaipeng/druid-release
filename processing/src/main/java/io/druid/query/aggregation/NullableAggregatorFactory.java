/*
 * Licensed to Metamarkets Group Inc. (Metamarkets) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Metamarkets licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.druid.query.aggregation;


import io.druid.common.config.NullHandling;
import io.druid.guice.annotations.ExtensionPoint;
import io.druid.segment.ColumnSelectorFactory;
import io.druid.segment.ColumnValueSelector;

/**
 * Abstract class with functionality to wrap {@link Aggregator}, {@link BufferAggregator} and {@link AggregateCombiner}
 * to support nullable aggregations for SQL compatibility. Implementations of {@link AggregatorFactory} which need to
 * Support Nullable Aggregations are encouraged to extend this class.
 */
@ExtensionPoint
public abstract class NullableAggregatorFactory extends AggregatorFactory
{
  @Override
  public final Aggregator factorize(ColumnSelectorFactory metricFactory)
  {
    ColumnValueSelector selector = selector(metricFactory);
    Aggregator aggregator = factorize(metricFactory, selector);
    return NullHandling.replaceWithDefault() ? aggregator : new NullableAggregator(aggregator, selector);
  }

  @Override
  public final BufferAggregator factorizeBuffered(ColumnSelectorFactory metricFactory)
  {
    ColumnValueSelector selector = selector(metricFactory);
    BufferAggregator aggregator = factorizeBuffered(metricFactory, selector);
    return NullHandling.replaceWithDefault() ? aggregator : new NullableBufferAggregator(aggregator, selector);
  }

  @Override
  public final AggregateCombiner makeAggregateCombiner()
  {
    AggregateCombiner combiner = makeAggregateCombiner2();
    return NullHandling.replaceWithDefault() ? combiner : new NullableAggregateCombiner(combiner);
  }

  @Override
  public final int getMaxIntermediateSize()
  {
    return getMaxIntermediateSize2() + (NullHandling.replaceWithDefault() ? 0 : Byte.BYTES);
  }

  // ---- ABSTRACT METHODS BELOW ------

  /**
   * Creates a {@link ColumnValueSelector} for the aggregated column.
   *
   * @see ColumnValueSelector
   */
  protected abstract ColumnValueSelector selector(ColumnSelectorFactory metricFactory);

  /**
   * Creates an {@link Aggregator} to aggregate values from several rows, by using the provided selector.
   * @param metricFactory metricFactory
   * @param selector {@link ColumnValueSelector} for the column to aggregate.
   *
   * @see Aggregator
   */
  protected abstract Aggregator factorize(ColumnSelectorFactory metricFactory, ColumnValueSelector selector);

  /**
   * Creates an {@link BufferAggregator} to aggregate values from several rows into a ByteBuffer.
   * @param metricFactory metricFactory
   * @param selector {@link ColumnValueSelector} for the column to aggregate.
   *
   * @see BufferAggregator
   */
  protected abstract BufferAggregator factorizeBuffered(
      ColumnSelectorFactory metricFactory,
      ColumnValueSelector selector
  );

  /**
   * Creates an {@link AggregateCombiner} to fold rollup aggregation results from serveral "rows" of different indexes during
   * index merging. AggregateCombiner implements the same logic as {@link #combine}, with the difference that it uses
   * {@link ColumnValueSelector} and it's subinterfaces to get inputs and implements {@code
   * ColumnValueSelector} to provide output.
   *
   * @see AggregateCombiner
   * @see io.druid.segment.IndexMerger
   */
  @SuppressWarnings("unused") // Going to be used when https://github.com/druid-io/druid/projects/2 is complete
  protected abstract AggregateCombiner makeAggregateCombiner2();

  /**
   * Returns the maximum size that this aggregator will require in bytes for intermediate storage of results.
   *
   * @return the maximum number of bytes that an aggregator of this type will require for intermediate result storage.
   */
  protected abstract int getMaxIntermediateSize2();

}
