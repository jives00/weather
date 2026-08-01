package com.weather.app

import com.weather.app.domain.model.Units
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {

    @Test
    fun `imperial unit has correct symbols`() {
        val units = Units.IMPERIAL
        assertEquals("°F", units.temperatureSymbol)
        assertEquals("mph", units.windSpeedUnit)
        assertEquals("in", units.precipitationUnit)
        assertEquals("fahrenheit", units.apiTemperatureUnit)
        assertEquals("mph", units.apiWindSpeedUnit)
        assertEquals("inch", units.apiPrecipitationUnit)
    }

    @Test
    fun `metric unit has correct symbols`() {
        val units = Units.METRIC
        assertEquals("°C", units.temperatureSymbol)
        assertEquals("km/h", units.windSpeedUnit)
        assertEquals("mm", units.precipitationUnit)
        assertEquals("celsius", units.apiTemperatureUnit)
        assertEquals("kmh", units.apiWindSpeedUnit)
        assertEquals("mm", units.apiPrecipitationUnit)
    }

    @Test
    fun `fromMm converts thresholds to inches for imperial`() {
        assertEquals(0.05 / 25.4, Units.IMPERIAL.fromMm(0.05), 1e-9)
        assertEquals(0.5 / 25.4, Units.IMPERIAL.fromMm(0.5), 1e-9)
    }

    @Test
    fun `fromMm leaves thresholds untouched for metric`() {
        assertEquals(0.05, Units.METRIC.fromMm(0.05), 1e-9)
        assertEquals(0.5, Units.METRIC.fromMm(0.5), 1e-9)
    }

    @Test
    fun `pressure unit is hPa for both`() {
        assertEquals("hPa", Units.IMPERIAL.pressureUnit)
        assertEquals("hPa", Units.METRIC.pressureUnit)
    }
}
