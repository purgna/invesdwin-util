package de.invesdwin.util.math.decimal.internal.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.math3.dfp.Dfp;

import de.invesdwin.util.math.BigDecimals;
import de.invesdwin.util.math.decimal.ADecimal;
import de.invesdwin.util.math.decimal.Decimal;

@ThreadSafe
public class BigDecimalDecimalImpl extends ADecimalImpl<BigDecimalDecimalImpl, BigDecimal> {

    public BigDecimalDecimalImpl(final BigDecimal value, final BigDecimal defaultRoundedValue) {
        super(value, defaultRoundedValue);
    }

    @Override
    public boolean isZero() {
        return getDefaultRoundedValue().compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public boolean isPositive() {
        return getDefaultRoundedValue().compareTo(BigDecimal.ZERO) >= 0;
    }

    @Override
    public String internalToString() {
        return getDefaultRoundedValue().toPlainString();
    }

    @Override
    protected int internalCompareTo(final ADecimal<?> defaultRoundedOther) {
        return getDefaultRoundedValue().compareTo(BigDecimalDecimalImplFactory.toBigDecimal(defaultRoundedOther));
    }

    @Override
    public int intValue() {
        return getValue().intValue();
    }

    @Override
    public long longValue() {
        return getValue().longValue();
    }

    @Override
    public float floatValue() {
        return getValue().floatValue();
    }

    @Override
    public double doubleValue() {
        return getValue().doubleValue();
    }

    @Override
    public byte byteValue() {
        return getValue().byteValue();
    }

    @Override
    public short shortValue() {
        return getValue().shortValue();
    }

    @Override
    public BigDecimalDecimalImpl abs() {
        return newValueCopy(getValue().abs());
    }

    @Override
    public BigDecimalDecimalImpl scaleByPowerOfTen(final int n) {
        return newValueCopy(getValue().scaleByPowerOfTen(n));
    }

    @Override
    public BigDecimalDecimalImpl root(final int n) {
        final double doubleValue = getValue().doubleValue();
        return newValueCopy(new DoubleDecimalImpl(doubleValue, doubleValue).root(n).bigDecimalValue());
    }

    @Override
    public BigDecimalDecimalImpl sqrt() {
        final double doubleValue = getValue().doubleValue();
        return newValueCopy(new DoubleDecimalImpl(doubleValue, doubleValue).sqrt().bigDecimalValue());
    }

    @Override
    public BigDecimalDecimalImpl pow(final int exponent) {
        return newValueCopy(getValue().pow(exponent, BigDecimals.DEFAULT_MATH_CONTEXT));
    }

    @Override
    public BigDecimalDecimalImpl subtract(final ADecimal<?> subtrahend) {
        return newValueCopy(getValue().subtract(subtrahend.bigDecimalValue(), BigDecimals.DEFAULT_MATH_CONTEXT));
    }

    @Override
    public BigDecimalDecimalImpl add(final ADecimal<?> augend) {
        return newValueCopy(getValue().add(augend.bigDecimalValue(), BigDecimals.DEFAULT_MATH_CONTEXT));
    }

    @Override
    public BigDecimalDecimalImpl multiply(final Number multiplicant) {
        return newValueCopy(
                getValue().multiply(BigDecimalDecimalImplFactory.toBigDecimal(multiplicant),
                        BigDecimals.DEFAULT_MATH_CONTEXT)).round(Decimal.DEFAULT_ROUNDING_SCALE,
                Decimal.DEFAULT_ROUNDING_MODE);
    }

    @Override
    public BigDecimalDecimalImpl multiply(final ADecimal<?> multiplicant) {
        return newValueCopy(getValue().multiply(multiplicant.bigDecimalValue(), BigDecimals.DEFAULT_MATH_CONTEXT));
    }

    @Override
    public BigDecimalDecimalImpl divide(final Number divisor) {
        return newValueCopy(getValue().divide(BigDecimalDecimalImplFactory.toBigDecimal(divisor),
                BigDecimals.DEFAULT_MATH_CONTEXT));
    }

    @Override
    public BigDecimalDecimalImpl divide(final ADecimal<?> divisor) {
        return newValueCopy(getValue().divide(divisor.bigDecimalValue(), BigDecimals.DEFAULT_MATH_CONTEXT));
    }

    @Override
    public BigDecimalDecimalImpl remainder(final Number divisor) {
        return newValueCopy(getValue().remainder(BigDecimalDecimalImplFactory.toBigDecimal(divisor),
                BigDecimals.DEFAULT_MATH_CONTEXT));
    }

    @Override
    public BigDecimalDecimalImpl remainder(final ADecimal<?> divisor) {
        return newValueCopy(getValue().remainder(divisor.bigDecimalValue(), BigDecimals.DEFAULT_MATH_CONTEXT));
    }

    @Override
    public BigDecimal bigDecimalValue() {
        return getValue();
    }

    @Override
    public BigInteger bigIntegerValue() {
        return getValue().toBigInteger();
    }

    @Override
    protected BigDecimal internalRound(final BigDecimal value, final int scale, final RoundingMode roundingMode) {
        return value.setScale(scale, roundingMode);
    }

    @Override
    public Dfp dfpValue() {
        return DfpDecimalImplFactory.toDfp(getValue());
    }

    @Override
    public Number numberValue() {
        return getValue();
    }

    @Override
    protected BigDecimal getZero() {
        return BigDecimal.ZERO;
    }

    @Override
    protected BigDecimalDecimalImpl newValueCopy(final BigDecimal value, final BigDecimal defaultRoundedValue) {
        return new BigDecimalDecimalImpl(value, defaultRoundedValue);
    }

    @Override
    protected BigDecimalDecimalImpl getGenericThis() {
        return this;
    }

}
