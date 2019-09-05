package dev.natewells.weather

import groovy.json.JsonSlurper
import wslite.rest.ContentType
import wslite.rest.RESTClient

class ForecastService {

    def grailsApplication
    RESTClient client  = new RESTClient()

    HourForecast getForecastForHour( Double latitude, Double longitude, Date hour ) {
        HourForecast forecast = new HourForecast()
        String baseUrl = grailsApplication.config.gameForecast.nws.baseUrl
        client.url = baseUrl

        // Step 1: Call the points API to find out what URL to retrieve the hourly forecast from
        try {
            def pointResponse = client.get(
                    path: "/points/${latitude.round(4)},${longitude.round(4)}",
                    headers: [
                            'user-agent': "dev.natewells.GameForecast-grails2.5.1"
                    ],
                    accept: ContentType.JSON,
                    connectTimeout: 2000,
                    readTimeout: 10000
            )

            log.info("Response from NWS with a status code of ${pointResponse.statusCode}")
            log.debug("  Response payload:\n${pointResponse.contentAsString}")

            if (pointResponse.statusCode == 200) {
                // parse the response JSON and grab the URL to the hourly forecast product.
                def jsonPointResponse = new JsonSlurper().parseText(pointResponse.contentAsString)
                String forecastPath = jsonPointResponse.properties.forecastHourly?.replace(baseUrl, '')

                if ( forecastPath ) {
                    // now make the second request to get the hourly forecast.
                    try {
                        def forecastResponse = client.get(
                                path: forecastPath,
                                headers: [
                                        'user-agent': "dev.natewells.GameForecast-grails2.5.1"
                                ],
                                accept: ContentType.JSON,
                                connectTimeout: 2000,
                                readTimeout: 10000
                        )

                        log.info("Response from NWS with a status code of ${forecastResponse.statusCode}")
                        log.debug("  Response payload:\n${forecastResponse.contentAsString}")

                        if (forecastResponse.statusCode == 200) {
                            def jsonForecastResponse = new JsonSlurper().parseText(forecastResponse.contentAsString)

                            // Find the forecast period that includes the requested time.
                            def forecastPeriod = jsonForecastResponse.properties.periods.find {
                                Date.parse("yyyy-MM-dd'T'HH:mm:ssXXX", it.startTime) <= hour &&
                                        hour < Date.parse("yyyy-MM-dd'T'HH:mm:ssXXX", it.endTime)
                            }
                            if (forecastPeriod) {
                                forecast.temperature = forecastPeriod.temperature
                                forecast.windSpeed = forecastPeriod.windSpeed
                                forecast.windDirection = forecastPeriod.windDirection
                                forecast.weatherDescription = forecastPeriod.shortForecast
                            } else {
                                forecast.errorMessage = "Forecast not available."
                            }
                        } else {
                            forecast.errorMessage = "Forecast request failed."
                        }
                    } catch( Exception e ){
                        log.error(e)
                        forecast.errorMessage = "Error encountered retrieving forecast details."
                    }
                } else {
                    forecast.errorMessage = "Forecast details not available."
                }

            } else {
                forecast.errorMessage = 'Invalid location.'
            }
        } catch( Exception e ){
            log.error(e)
            forecast.errorMessage = "Error encountered retrieving forecast."
        }
        forecast
    }
}
