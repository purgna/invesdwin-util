package de.invesdwin.util.math.decimal;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import de.invesdwin.util.math.decimal.internal.impl.ADecimalImpl;
import de.invesdwin.util.math.decimal.internal.impl.DoubleDecimalImplFactory;
import de.invesdwin.util.math.decimal.internal.impl.IDecimalImplFactory;

@Immutable
public class Decimal extends ADecimal<Decimal> {

    public static final String DEFAULT_DECIMAL_FORMAT = "#,##0.##";

    public static final Decimal MINUS_THREE;
    public static final Decimal MINUS_TWO;
    public static final Decimal MINUS_ONE;
    public static final Decimal ZERO;
    public static final Decimal ONE;
    public static final Decimal TWO;
    public static final Decimal THREE;

    /**
     * Due to implementation simplifications of equals, hashCode and compareTo only one implementations cannot be mixed
     * with each other.
     */
    private static final IDecimalImplFactory<?> DECIMAL_IMPL_FACTORY;

    static {
        /*
         * double is the fastest implementation, thus defaulting to that. The other ones are still there for comparison
         * purposes.
         */
        DECIMAL_IMPL_FACTORY = new DoubleDecimalImplFactory();
        MINUS_THREE = new Decimal("-3");
        MINUS_TWO = new Decimal("-2");
        MINUS_ONE = new Decimal("-1");
        ZERO = new Decimal("0");
        ONE = new Decimal("1");
        TWO = new Decimal("2");
        THREE = new Decimal("3");
    }

    private final ADecimalImpl<?, ?> impl;

    public Decimal(final Number value) {
        this(DECIMAL_IMPL_FACTORY.valueOf(value));
    }

    public Decimal(final Double value) {
        this(DECIMAL_IMPL_FACTORY.valueOf(value));
    }

    public Decimal(final Float value) {
        this(DECIMAL_IMPL_FACTORY.valueOf(value));
    }

    public Decimal(final Long value) {
        this(DECIMAL_IMPL_FACTORY.valueOf(value));
    }

    public Decimal(final Integer value) {
        this(DECIMAL_IMPL_FACTORY.valueOf(value));
    }

    public Decimal(final Short value) {
        this(DECIMAL_IMPL_FACTORY.valueOf(value));
    }

    public Decimal(final Byte value) {
        this(DECIMAL_IMPL_FACTORY.valueOf(value));
    }

    public Decimal(final String value) {
        this(DECIMAL_IMPL_FACTORY.valueOf(value));
    }

    public Decimal(final ADecimalImpl<?, ?> impl) {
        if (impl instanceof ScaledDecimalDelegateImpl) {
            this.impl = ((ScaledDecimalDelegateImpl) impl).getDelegate();
        } else {
            this.impl = impl;
        }
    }

    @Override
    protected ADecimalImpl getImpl() {
        return impl;
    }

    @Override
    protected Decimal newValueCopy(final ADecimalImpl value) {
        return new Decimal(value);
    }

    public static Decimal nullToZero(final Decimal value) {
        if (value == null) {
            return ZERO;
        } else {
            return value;
        }
    }

    @Override
    protected Decimal getGenericThis() {
        return this;
    }

    @Override
    protected Decimal fromDefaultValue(final Decimal value) {
        return value;
    }

    @Override
    protected Decimal getDefaultValue() {
        return this;
    }

    public static IDecimalAggregate<Decimal> valueOf(final Decimal... values) {
        return valueOf(Arrays.asList(values));
    }

    public static IDecimalAggregate<Decimal> valueOf(final List<? extends Decimal> values) {
        if (values == null || values.size() == 0) {
            return DummyDecimalAggregate.getInstance();
        } else {
            return new DecimalAggregate<Decimal>(values);
        }
    }

    /**
     * Use default values of the scaled decimal instead!
     */
    @Deprecated
    public static Decimal valueOf(final AScaledDecimal<?, ?> value) {
        throw new UnsupportedOperationException();
    }

    public static Decimal valueOf(final Double value) {
        if (value == null) {
            return null;
        } else {
            return new Decimal(value);
        }
    }

    public static Decimal valueOf(final Number value) {
        if (value == null) {
            return null;
        } else if (value instanceof Decimal) {
            return (Decimal) value;
        } else {
            if (value instanceof AScaledDecimal) {
                throw new IllegalArgumentException("value [" + value + "] should not be an instance of "
                        + AScaledDecimal.class.getSimpleName());
            }
            return new Decimal(value);
        }
    }

    public static Decimal fromDefaultValue(final AScaledDecimal<?, ?> scaledDecimal) {
        if (scaledDecimal != null) {
            return scaledDecimal.getDefaultValue();
        } else {
            return null;
        }
    }

    public String toFormattedString() {
        return toFormattedString(DEFAULT_DECIMAL_FORMAT);
    }

    public String toFormattedString(final String format) {
        final DecimalFormat dc = new DecimalFormat(format);
        return dc.format(this);
    }

}
