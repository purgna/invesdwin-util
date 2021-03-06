package de.invesdwin.util.math.decimal;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import de.invesdwin.util.collections.Lists;

@Immutable
class DecimalAggregate<E extends ADecimal<E>> implements IDecimalAggregate<E> {

    private E converter;
    private final List<? extends E> values;

    public DecimalAggregate(final List<? extends E> values) {
        this.values = values;
    }

    private E getConverter() {
        if (converter == null) {
            for (final E scaledValue : values) {
                if (scaledValue != null) {
                    converter = scaledValue;
                    break;
                }
            }
        }
        return converter;

    }

    /**
     * All growth rates separately
     */
    @Override
    public IDecimalAggregate<E> growthRates() {
        final List<E> growthRates = new ArrayList<E>();
        E previousValue = (E) null;
        for (final E value : values) {
            if (previousValue != null) {
                growthRates.add(previousValue.growthRate(value));
            }
            previousValue = value;
        }
        return new DecimalAggregate<E>(growthRates);
    }

    /**
     * The average of all growthRates.
     */
    @Override
    public E growthRate() {
        return growthRates().avg();
    }

    /**
     * The growthRate of the growthRates.
     */
    @Override
    public E growthRatesTrend() {
        return growthRates().growthRate();
    }

    @Override
    public IDecimalAggregate<E> reverse() {
        return new DecimalAggregate<E>(Lists.reverse(values));
    }

    /**
     * Returns a weighted average where the first value has the least weight and the last value has the highest weight.
     */
    @Override
    public E avgWeightedAsc() {
        return reverse().avgWeightedDesc();
    }

    /**
     * Returns a weighted average where the first value has the highest weight and the last value has the least weight.
     */
    @Override
    public E avgWeightedDesc() {
        int sumOfWeights = 0;
        Decimal sumOfWeightedValues = Decimal.ZERO;
        for (int i = 0, weight = values.size(); i < values.size(); i++, weight--) {
            final Decimal weightedValue = values.get(i).getDefaultValue().multiply(weight);
            sumOfWeights += weight;
            sumOfWeightedValues = sumOfWeightedValues.add(weightedValue);
        }
        return getConverter().fromDefaultValue(sumOfWeightedValues.divide(sumOfWeights));
    }

    @Override
    public E sum() {
        Decimal sum = Decimal.ZERO;
        for (final E value : values) {
            if (value != null) {
                sum = sum.add(value.getDefaultValue());
            }
        }
        return getConverter().fromDefaultValue(sum);
    }

    /**
     * x_quer = (x_1 + x_2 + ... + x_n) / n
     * 
     * @see <a href="http://de.wikipedia.org/wiki/Arithmetisches_Mittel">Source</a>
     */
    @Override
    public E avg() {
        return sum().divide(values.size());
    }

    /**
     * Product = x_1 * x_2 * ... * x_n
     * 
     * @see <a href="http://de.wikipedia.org/wiki/Arithmetisches_Mittel">Source</a>
     */
    @Override
    public E product() {
        Decimal product = Decimal.ONE;
        for (final E value : values) {
            product = product.multiply(value.getDefaultValue());
        }
        return getConverter().fromDefaultValue(product);
    }

    /**
     * x_quer = (x_1 * x_2 * ... * x_n)^1/n
     * 
     * @see <a href="http://de.wikipedia.org/wiki/Geometrisches_Mittel">Source</a>
     * @see <a href="http://www.ee.ucl.ac.uk/~mflanaga/java/Stat.html#geom2">Source with BigDecimal</a>
     */
    @Override
    public E geomAvg() {
        double logSum = 0;
        for (int i = 0; i < values.size(); i++) {
            logSum += Math.log(values.get(i).doubleValue());
        }
        final double result = Math.exp(logSum / values.size());
        return getConverter().fromDefaultValue(new Decimal(result));
    }

    @Override
    public E max() {
        E highest = (E) null;
        for (final E value : values) {
            if (highest == null) {
                highest = value;
            } else if (value == null) {
                continue;
            } else if (highest.compareTo(value) < 0) {
                highest = value;
            }
        }
        return highest;
    }

    @Override
    public E min() {
        E lowest = (E) null;
        for (final E value : values) {
            if (lowest == null) {
                lowest = value;
            } else if (value == null) {
                continue;
            } else if (value.compareTo(lowest) < 0) {
                lowest = value;
            }
        }
        return lowest;
    }

    /**
     * s = (1/(n-1) * sum((x_i - x_quer)^2))^1/2
     */
    @Override
    public E standardDeviation() {
        final E avg = avg();
        Decimal sum = Decimal.ZERO;
        for (final E value : values) {
            sum = sum.add(value.subtract(avg).getDefaultValue().pow(2));
        }
        return getConverter().fromDefaultValue(sum.sqrt().divide(values.size() - 1));
    }

    /**
     * s^2 = 1/(n-1) * sum((x_i - x_quer)^2)
     */
    @Override
    public E variance() {
        final E avg = avg();
        Decimal sum = Decimal.ZERO;
        for (final E value : values) {
            sum = sum.add(value.subtract(avg).getDefaultValue().pow(2));
        }
        return getConverter().fromDefaultValue(sum.divide(values.size() - 1));
    }

    /**
     * s^2 = 1/(n) * sum((x_i - x_quer)^2)
     * 
     * <a href="http://de.wikipedia.org/wiki/Stichprobenvarianz">Source</a>
     */
    @Override
    public E sampleVariance() {
        final E avg = avg();
        Decimal sum = Decimal.ZERO;
        for (final E value : values) {
            sum = sum.add(value.subtract(avg).getDefaultValue().pow(2));
        }
        return getConverter().fromDefaultValue(sum.divide(values.size()));
    }

    @Override
    public List<? extends E> values() {
        return Collections.unmodifiableList(values);
    }

    @Override
    public IDecimalAggregate<E> round() {
        return round(Decimal.DEFAULT_ROUNDING_SCALE);
    }

    @Override
    public IDecimalAggregate<E> round(final RoundingMode roundingMode) {
        return round(Decimal.DEFAULT_ROUNDING_SCALE, roundingMode);
    }

    @Override
    public IDecimalAggregate<E> round(final int scale) {
        return round(scale, Decimal.DEFAULT_ROUNDING_MODE);
    }

    @Override
    public IDecimalAggregate<E> round(final int scale, final RoundingMode roundingMode) {
        final List<E> rounded = new ArrayList<E>(values.size());
        for (final E value : values) {
            rounded.add(value.round(scale, roundingMode));
        }
        return new DecimalAggregate<E>(rounded);
    }

    @Override
    public IDecimalAggregate<E> roundToStep(final E step) {
        return roundToStep(step, Decimal.DEFAULT_ROUNDING_MODE);
    }

    @Override
    public IDecimalAggregate<E> roundToStep(final E step, final RoundingMode roundingMode) {
        final List<E> rounded = new ArrayList<E>(values.size());
        for (final E value : values) {
            rounded.add(value.roundToStep(step, roundingMode));
        }
        return new DecimalAggregate<E>(rounded);
    }

    @Override
    public String toString() {
        return values.toString();
    }

}
