package dev.natewells.weather

import groovy.transform.ToString

@ToString( includePackage = false )
class HourForecast {
    String errorMessage = ''
    Integer temperature
    String windSpeed
    String windDirection
    String weatherDescription
}
