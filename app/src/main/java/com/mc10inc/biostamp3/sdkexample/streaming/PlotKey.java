package com.mc10inc.biostamp3.sdkexample.streaming;

import com.mc10inc.biostamp3.sdk.sensing.StreamingType;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PlotKey {
    private final String serial;
    private final PlotType plotType;

    public PlotKey(String serial, PlotType plotType) {
        this.serial = serial;
        this.plotType = plotType;
    }

    public PlotType getPlotType() {
        return plotType;
    }

    public String getSerial() {
        return serial;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlotKey that = (PlotKey) o;
        return serial.equals(that.serial) &&
                plotType == that.plotType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(serial, plotType);
    }

    @NotNull
    @Override
    public String toString() {
        return String.format("%s %s", serial, plotType);
    }
}
