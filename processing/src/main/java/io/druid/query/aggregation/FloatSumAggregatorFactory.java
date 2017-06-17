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

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.druid.java.util.common.StringUtils;
import io.druid.math.expr.ExprMacroTable;
import io.druid.segment.ColumnSelectorFactory;
import io.druid.segment.FloatColumnSelector;
import io.druid.segment.NullHandlingHelper;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 */
public class FloatSumAggregatorFactory extends SimpleFloatAggregatorFactory
{
  @JsonCreator
  public FloatSumAggregatorFactory(
      @JsonProperty("name") String name,
      @JsonProperty("fieldName") String fieldName,
      @JsonProperty("expression") String expression,
      @JacksonInject ExprMacroTable macroTable
  )
  {
    super(macroTable, name, fieldName, expression);
  }

  public FloatSumAggregatorFactory(String name, String fieldName)
  {
    this(name, fieldName, null, ExprMacroTable.nil());
  }

  @Override
  public Aggregator factorize(ColumnSelectorFactory metricFactory)
  {
    FloatColumnSelector floatColumnSelector = getFloatColumnSelector(metricFactory, 0.0f);
    return NullHandlingHelper.getNullableAggregator(new FloatSumAggregator(floatColumnSelector), floatColumnSelector);
  }

  @Override
  public BufferAggregator factorizeBuffered(ColumnSelectorFactory metricFactory)
  {
    FloatColumnSelector floatColumnSelector = getFloatColumnSelector(metricFactory, 0.0f);
    return NullHandlingHelper.getNullableAggregator(
        new FloatSumBufferAggregator(floatColumnSelector),
        floatColumnSelector
    );
  }

  @Override
  @Nullable
  public Object combine(@Nullable Object lhs, @Nullable Object rhs)
  {
    if (rhs == null) {
      return lhs;
    }
    if (lhs == null) {
      return rhs;
    }
    return FloatSumAggregator.combineValues(lhs, rhs);
  }

  @Override
  public AggregateCombiner makeAggregateCombiner()
  {
    return NullHandlingHelper.getNullableCombiner(new DoubleSumAggregateCombiner());
  }

  @Override
  public AggregatorFactory getCombiningFactory()
  {
    return new FloatSumAggregatorFactory(name, name, null, macroTable);
  }


  @Override
  public List<AggregatorFactory> getRequiredColumns()
  {
    return Arrays.asList(new FloatSumAggregatorFactory(fieldName, fieldName, expression, macroTable));
  }

  @JsonProperty
  public String getFieldName()
  {
    return fieldName;
  }

  @JsonProperty
  public String getExpression()
  {
    return expression;
  }

  @Override
  @JsonProperty
  public String getName()
  {
    return name;
  }

  @Override
  public byte[] getCacheKey()
  {
    byte[] fieldNameBytes = StringUtils.toUtf8WithNullToEmpty(fieldName);
    byte[] expressionBytes = StringUtils.toUtf8WithNullToEmpty(expression);

    return ByteBuffer.allocate(2 + fieldNameBytes.length + expressionBytes.length)
                     .put(AggregatorUtil.FLOAT_SUM_CACHE_TYPE_ID)
                     .put(fieldNameBytes)
                     .put(AggregatorUtil.STRING_SEPARATOR)
                     .put(expressionBytes)
                     .array();
  }

  @Override
  public String toString()
  {
    return "FloatSumAggregatorFactory{" +
           "fieldName='" + fieldName + '\'' +
           ", expression='" + expression + '\'' +
           ", name='" + name + '\'' +
           '}';
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    FloatSumAggregatorFactory that = (FloatSumAggregatorFactory) o;

    if (!Objects.equals(fieldName, that.fieldName)) {
      return false;
    }
    if (!Objects.equals(expression, that.expression)) {
      return false;
    }
    if (!Objects.equals(name, that.name)) {
      return false;
    }
    return true;
  }
}