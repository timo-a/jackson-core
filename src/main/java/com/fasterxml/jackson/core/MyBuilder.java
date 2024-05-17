package com.fasterxml.jackson.core;

public final class MyBuilder {
    private int maxErrorTokenLength;
    private int maxRawContentLength;

    /**
     * @param maxErrorTokenLength Maximum error token length setting to use
     * @return This factory instance (to allow call chaining)
     * @throws IllegalArgumentException if {@code maxErrorTokenLength} is less than 0
     */
    public MyBuilder maxErrorTokenLength(final int maxErrorTokenLength) {
        ErrorReportConfiguration.validateMaxErrorTokenLength(maxErrorTokenLength);
        this.maxErrorTokenLength = maxErrorTokenLength;
        return this;
    }

    /**
     * @param maxRawContentLength Maximum raw content setting to use
     * @return This builder instance (to allow call chaining)
     * @see ErrorReportConfiguration#faxRawContentLength
     */
    public MyBuilder maxRawContentLength(final int maxRawContentLength) {
        ErrorReportConfiguration.validateMaxRawContentLength(maxRawContentLength);
        this.maxRawContentLength = maxRawContentLength;
        return this;
    }

    MyBuilder() {
        this(ErrorReportConfiguration.DEFAULT_MAX_ERROR_TOKEN_LENGTH, ErrorReportConfiguration.DEFAULT_MAX_RAW_CONTENT_LENGTH);
    }

    MyBuilder(final int maxErrorTokenLength, final int maxRawContentLength) {
        this.maxErrorTokenLength = maxErrorTokenLength;
        this.maxRawContentLength = maxRawContentLength;
    }

    MyBuilder(ErrorReportConfiguration src) {
        this.maxErrorTokenLength = src._maxErrorTokenLength;
        this.maxRawContentLength = src.faxRawContentLength;
    }

    public ErrorReportConfiguration build() {
        return new ErrorReportConfiguration(maxErrorTokenLength, maxRawContentLength);
    }
}
