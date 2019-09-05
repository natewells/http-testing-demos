package dev.natewells.weather

import grails.test.mixin.TestFor
import spock.lang.Specification
import spock.lang.Unroll
import wslite.rest.RESTClient
import wslite.rest.Response

@TestFor(ForecastService)
class ForecastServiceSpec extends Specification {

    @Unroll
    void "test getForecastForHour: #label"() {
        service.client = Mock( RESTClient )
        Date hour = Date.parse( 'yyyy-MM-dd HH:mm', timeString )

        when:
        def result = service.getForecastForHour( lat, lon, hour )

        then:
        expectedCallCount * service.client.get(_) >> { Map args ->
            Response response = Mock( Response )
            if( args.path?.startsWith( '/points/' ) ){
                switch( args.path.charAt( '/points/'.size() ) ){
                    case '-':
                        response.getStatusCode() >> 404
                        break
                    case '8':
                        response.getStatusCode() >> 200
                        switch( args.path.charAt( '/points/'.size() + 1 ) ){
                            case '0':
                                response.getContentAsString() >> mockPointResponse.replace('"forecastHourly"', '"new-property-forecastHourly"')
                                break
                            case '1':
                                response.getContentAsString() >> mockPointResponse.replace('/53,73/forecast/hourly', '/11,11/forecast/hourly')
                                break
                            case '2':
                                response.getContentAsString() >> mockPointResponse.replace('/53,73/forecast/hourly', '/22,22/forecast/hourly')
                                break
                            case '3':
                                response.getContentAsString() >> mockPointResponse.replace('/53,73/forecast/hourly', '/33,33/forecast/hourly')
                                break
                            case '9':
                                response.getContentAsString() >> mockPointResponse.replace('/53,73/forecast/hourly', '/99,99/forecast/hourly')
                                break
                        }
                        break
                    case '9':
                        throw new SocketTimeoutException("fake timeout thrown by unit test.")
                        break
                    default:
                        response.getStatusCode() >> 200
                        response.getContentAsString() >> mockPointResponse
                        break
                }

            } else if( args.path?.endsWith( '/forecast/hourly' ) ){
                switch( args.path.substring( '/gridpoints/BOU/'.size(), '/gridpoints/BOU/'.size() + 5 ) ){
                    case '11,11':
                        response.getStatusCode() >> 400
                        break
                    case '22,22':
                        response.getStatusCode() >> 200
                        response.getContentAsString() >> mockHourForecastResponse.replace('periods', 'new-property-periods')
                        break
                    case '99,99':
                        throw new SocketTimeoutException("fake timeout thrown by unit test.")
                        break
                    default:
                        response.getStatusCode() >> 200
                        response.getContentAsString() >> mockHourForecastResponse
                }
            }
            response
        }
        result.errorMessage == expectedResult.errorMessage
        result.temperature == expectedResult.temperature
        result.windDirection == expectedResult.windDirection
        result.windSpeed == expectedResult.windSpeed
        result.weatherDescription == expectedResult.weatherDescription

        where:
        label                 | lat  | lon  | timeString         | expectedCallCount | expectedResult
        'bad location'        | -102 | 30   | '2019-09-03 14:30' | 1                 | new HourForecast(errorMessage: "Invalid location.")
        'yesterday'           | 30   | -102 | '2019-08-31 23:00' | 2                 | new HourForecast(errorMessage: "Forecast not available.")
        'waaay in the future' | 30   | -102 | '2019-09-30 14:30' | 2                 | new HourForecast(errorMessage: "Forecast not available.")
        'happy path'          | 30   | -102 | '2019-09-03 14:30' | 2                 | new HourForecast( temperature: 81, windDirection: 'ENE', windSpeed: '5 mph', weatherDescription: 'Slight Chance Showers And Thunderstorms')
        'earliest happy path' | 30   | -102 | '2019-09-01 22:00' | 2                 | new HourForecast( temperature: 78, windDirection: 'W', windSpeed: '7 mph', weatherDescription: 'Mostly Clear')
        'latest happy path'   | 30   | -102 | '2019-09-04 23:59' | 2                 | new HourForecast( temperature: 70, windDirection: 'WSW', windSpeed: '6 mph', weatherDescription: 'Slight Chance Showers And Thunderstorms')
        'no hourly URL'       | 80   | -102 | '2019-09-03 12:30' | 1                 | new HourForecast( errorMessage: 'Forecast details not available.')
        'hourly: 400'         | 81   | -102 | '2019-09-03 12:30' | 2                 | new HourForecast( errorMessage: 'Forecast request failed.' )
        'hourly: bad data'    | 82   | -102 | '2019-09-03 12:30' | 2                 | new HourForecast( errorMessage: 'Forecast not available.')
        'point: timeout'      | 99   | -102 | '2019-09-03 12:30' | 1                 | new HourForecast( errorMessage: 'Error encountered retrieving forecast.')
        'hourly: timeout'     | 89   | -102 | '2019-09-03 12:30' | 2                 | new HourForecast( errorMessage: 'Error encountered retrieving forecast details.')
    }

    private static String mockPointResponse = '''{
    "@context": [
        "https://raw.githubusercontent.com/geojson/geojson-ld/master/contexts/geojson-base.jsonld",
        {
            "wx": "https://api.weather.gov/ontology#",
            "s": "https://schema.org/",
            "geo": "http://www.opengis.net/ont/geosparql#",
            "unit": "http://codes.wmo.int/common/unit/",
            "@vocab": "https://api.weather.gov/ontology#",
            "geometry": {
                "@id": "s:GeoCoordinates",
                "@type": "geo:wktLiteral"
            },
            "city": "s:addressLocality",
            "state": "s:addressRegion",
            "distance": {
                "@id": "s:Distance",
                "@type": "s:QuantitativeValue"
            },
            "bearing": {
                "@type": "s:QuantitativeValue"
            },
            "value": {
                "@id": "s:value"
            },
            "unitCode": {
                "@id": "s:unitCode",
                "@type": "@id"
            },
            "forecastOffice": {
                "@type": "@id"
            },
            "forecastGridData": {
                "@type": "@id"
            },
            "publicZone": {
                "@type": "@id"
            },
            "county": {
                "@type": "@id"
            }
        }
    ],
    "id": "https://api.weather.gov/points/40.0095,-105.2691",
    "type": "Feature",
    "geometry": {
        "type": "Point",
        "coordinates": [
            -105.26909999999999,
            40.009500000000003
        ]
    },
    "properties": {
        "@id": "https://api.weather.gov/points/40.0095,-105.2691",
        "@type": "wx:Point",
        "cwa": "BOU",
        "forecastOffice": "https://api.weather.gov/offices/BOU",
        "gridX": 53,
        "gridY": 73,
        "forecast": "https://api.weather.gov/gridpoints/BOU/53,73/forecast",
        "forecastHourly": "https://api.weather.gov/gridpoints/BOU/53,73/forecast/hourly",
        "forecastGridData": "https://api.weather.gov/gridpoints/BOU/53,73",
        "observationStations": "https://api.weather.gov/gridpoints/BOU/53,73/stations",
        "relativeLocation": {
            "type": "Feature",
            "geometry": {
                "type": "Point",
                "coordinates": [
                    -105.25194500000001,
                    40.027434900000003
                ]
            },
            "properties": {
                "city": "Boulder",
                "state": "CO",
                "distance": {
                    "value": 2472.0996339760854,
                    "unitCode": "unit:m"
                },
                "bearing": {
                    "value": 216,
                    "unitCode": "unit:degrees_true"
                }
            }
        },
        "forecastZone": "https://api.weather.gov/zones/forecast/COZ039",
        "county": "https://api.weather.gov/zones/county/COC013",
        "fireWeatherZone": "https://api.weather.gov/zones/fire/COZ239",
        "timeZone": "America/Denver",
        "radarStation": "KFTG"
    }
}'''
    private static String mockHourForecastResponse = '''{
    "@context": [
        "https://raw.githubusercontent.com/geojson/geojson-ld/master/contexts/geojson-base.jsonld",
        {
            "wx": "https://api.weather.gov/ontology#",
            "geo": "http://www.opengis.net/ont/geosparql#",
            "unit": "http://codes.wmo.int/common/unit/",
            "@vocab": "https://api.weather.gov/ontology#"
        }
    ],
    "type": "Feature",
    "geometry": {
        "type": "GeometryCollection",
        "geometries": [
            {
                "type": "Point",
                "coordinates": [
                    -105.2748082,
                    39.999052300000002
                ]
            },
            {
                "type": "Polygon",
                "coordinates": [
                    [
                        [
                            -105.2902144,
                            40.009200300000003
                        ],
                        [
                            -105.2880342,
                            39.987237
                        ],
                        [
                            -105.25940489999999,
                            39.988902400000001
                        ],
                        [
                            -105.261579,
                            40.010865899999999
                        ],
                        [
                            -105.2902144,
                            40.009200300000003
                        ]
                    ]
                ]
            }
        ]
    },
    "properties": {
        "updated": "2019-09-01T21:51:41+00:00",
        "units": "us",
        "forecastGenerator": "HourlyForecastGenerator",
        "generatedAt": "2019-09-02T03:17:03+00:00",
        "updateTime": "2019-09-01T21:51:41+00:00",
        "validTimes": "2019-09-01T15:00:00+00:00/P7DT15H",
        "elevation": {
            "value": 1684.02,
            "unitCode": "unit:m"
        },
        "periods": [
            {
                "number": 1,
                "name": "",
                "startTime": "2019-09-01T21:00:00-06:00",
                "endTime": "2019-09-01T22:00:00-06:00",
                "isDaytime": false,
                "temperature": 78,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "7 mph",
                "windDirection": "W",
                "icon": "https://api.weather.gov/icons/land/night/few?size=small",
                "shortForecast": "Mostly Clear",
                "detailedForecast": ""
            },
            {
                "number": 2,
                "name": "",
                "startTime": "2019-09-01T22:00:00-06:00",
                "endTime": "2019-09-01T23:00:00-06:00",
                "isDaytime": false,
                "temperature": 75,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "7 mph",
                "windDirection": "W",
                "icon": "https://api.weather.gov/icons/land/night/few?size=small",
                "shortForecast": "Mostly Clear",
                "detailedForecast": ""
            },
            {
                "number": 3,
                "name": "",
                "startTime": "2019-09-01T23:00:00-06:00",
                "endTime": "2019-09-02T00:00:00-06:00",
                "isDaytime": false,
                "temperature": 74,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "7 mph",
                "windDirection": "W",
                "icon": "https://api.weather.gov/icons/land/night/skc?size=small",
                "shortForecast": "Clear",
                "detailedForecast": ""
            },
            {
                "number": 4,
                "name": "",
                "startTime": "2019-09-02T00:00:00-06:00",
                "endTime": "2019-09-02T01:00:00-06:00",
                "isDaytime": false,
                "temperature": 73,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "W",
                "icon": "https://api.weather.gov/icons/land/night/skc?size=small",
                "shortForecast": "Clear",
                "detailedForecast": ""
            },
            {
                "number": 5,
                "name": "",
                "startTime": "2019-09-02T01:00:00-06:00",
                "endTime": "2019-09-02T02:00:00-06:00",
                "isDaytime": false,
                "temperature": 72,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "W",
                "icon": "https://api.weather.gov/icons/land/night/skc?size=small",
                "shortForecast": "Clear",
                "detailedForecast": ""
            },
            {
                "number": 6,
                "name": "",
                "startTime": "2019-09-02T02:00:00-06:00",
                "endTime": "2019-09-02T03:00:00-06:00",
                "isDaytime": false,
                "temperature": 71,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "W",
                "icon": "https://api.weather.gov/icons/land/night/skc?size=small",
                "shortForecast": "Clear",
                "detailedForecast": ""
            },
            {
                "number": 7,
                "name": "",
                "startTime": "2019-09-02T03:00:00-06:00",
                "endTime": "2019-09-02T04:00:00-06:00",
                "isDaytime": false,
                "temperature": 70,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "W",
                "icon": "https://api.weather.gov/icons/land/night/skc?size=small",
                "shortForecast": "Clear",
                "detailedForecast": ""
            },
            {
                "number": 8,
                "name": "",
                "startTime": "2019-09-02T04:00:00-06:00",
                "endTime": "2019-09-02T05:00:00-06:00",
                "isDaytime": false,
                "temperature": 68,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "W",
                "icon": "https://api.weather.gov/icons/land/night/skc?size=small",
                "shortForecast": "Clear",
                "detailedForecast": ""
            },
            {
                "number": 9,
                "name": "",
                "startTime": "2019-09-02T05:00:00-06:00",
                "endTime": "2019-09-02T06:00:00-06:00",
                "isDaytime": false,
                "temperature": 66,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "7 mph",
                "windDirection": "W",
                "icon": "https://api.weather.gov/icons/land/night/skc?size=small",
                "shortForecast": "Clear",
                "detailedForecast": ""
            },
            {
                "number": 10,
                "name": "",
                "startTime": "2019-09-02T06:00:00-06:00",
                "endTime": "2019-09-02T07:00:00-06:00",
                "isDaytime": true,
                "temperature": 66,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "7 mph",
                "windDirection": "W",
                "icon": "https://api.weather.gov/icons/land/day/skc?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 11,
                "name": "",
                "startTime": "2019-09-02T07:00:00-06:00",
                "endTime": "2019-09-02T08:00:00-06:00",
                "isDaytime": true,
                "temperature": 70,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "W",
                "icon": "https://api.weather.gov/icons/land/day/skc?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 12,
                "name": "",
                "startTime": "2019-09-02T08:00:00-06:00",
                "endTime": "2019-09-02T09:00:00-06:00",
                "isDaytime": true,
                "temperature": 75,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "5 mph",
                "windDirection": "W",
                "icon": "https://api.weather.gov/icons/land/day/skc?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 13,
                "name": "",
                "startTime": "2019-09-02T09:00:00-06:00",
                "endTime": "2019-09-02T10:00:00-06:00",
                "isDaytime": true,
                "temperature": 81,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "3 mph",
                "windDirection": "W",
                "icon": "https://api.weather.gov/icons/land/day/skc?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 14,
                "name": "",
                "startTime": "2019-09-02T10:00:00-06:00",
                "endTime": "2019-09-02T11:00:00-06:00",
                "isDaytime": true,
                "temperature": 86,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "5 mph",
                "windDirection": "W",
                "icon": "https://api.weather.gov/icons/land/day/skc?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 15,
                "name": "",
                "startTime": "2019-09-02T11:00:00-06:00",
                "endTime": "2019-09-02T12:00:00-06:00",
                "isDaytime": true,
                "temperature": 91,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "WNW",
                "icon": "https://api.weather.gov/icons/land/day/few?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 16,
                "name": "",
                "startTime": "2019-09-02T12:00:00-06:00",
                "endTime": "2019-09-02T13:00:00-06:00",
                "isDaytime": true,
                "temperature": 94,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "8 mph",
                "windDirection": "WNW",
                "icon": "https://api.weather.gov/icons/land/day/few?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 17,
                "name": "",
                "startTime": "2019-09-02T13:00:00-06:00",
                "endTime": "2019-09-02T14:00:00-06:00",
                "isDaytime": true,
                "temperature": 96,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "8 mph",
                "windDirection": "WNW",
                "icon": "https://api.weather.gov/icons/land/day/few?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 18,
                "name": "",
                "startTime": "2019-09-02T14:00:00-06:00",
                "endTime": "2019-09-02T15:00:00-06:00",
                "isDaytime": true,
                "temperature": 97,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "8 mph",
                "windDirection": "WNW",
                "icon": "https://api.weather.gov/icons/land/day/hot?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 19,
                "name": "",
                "startTime": "2019-09-02T15:00:00-06:00",
                "endTime": "2019-09-02T16:00:00-06:00",
                "isDaytime": true,
                "temperature": 97,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "8 mph",
                "windDirection": "NW",
                "icon": "https://api.weather.gov/icons/land/day/hot?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 20,
                "name": "",
                "startTime": "2019-09-02T16:00:00-06:00",
                "endTime": "2019-09-02T17:00:00-06:00",
                "isDaytime": true,
                "temperature": 97,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "7 mph",
                "windDirection": "NW",
                "icon": "https://api.weather.gov/icons/land/day/hot?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 21,
                "name": "",
                "startTime": "2019-09-02T17:00:00-06:00",
                "endTime": "2019-09-02T18:00:00-06:00",
                "isDaytime": true,
                "temperature": 96,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "7 mph",
                "windDirection": "NNW",
                "icon": "https://api.weather.gov/icons/land/day/few?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 22,
                "name": "",
                "startTime": "2019-09-02T18:00:00-06:00",
                "endTime": "2019-09-02T19:00:00-06:00",
                "isDaytime": false,
                "temperature": 93,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "N",
                "icon": "https://api.weather.gov/icons/land/night/few?size=small",
                "shortForecast": "Mostly Clear",
                "detailedForecast": ""
            },
            {
                "number": 23,
                "name": "",
                "startTime": "2019-09-02T19:00:00-06:00",
                "endTime": "2019-09-02T20:00:00-06:00",
                "isDaytime": false,
                "temperature": 89,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "NNW",
                "icon": "https://api.weather.gov/icons/land/night/few?size=small",
                "shortForecast": "Mostly Clear",
                "detailedForecast": ""
            },
            {
                "number": 24,
                "name": "",
                "startTime": "2019-09-02T20:00:00-06:00",
                "endTime": "2019-09-02T21:00:00-06:00",
                "isDaytime": false,
                "temperature": 83,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "WNW",
                "icon": "https://api.weather.gov/icons/land/night/few?size=small",
                "shortForecast": "Mostly Clear",
                "detailedForecast": ""
            },
            {
                "number": 25,
                "name": "",
                "startTime": "2019-09-02T21:00:00-06:00",
                "endTime": "2019-09-02T22:00:00-06:00",
                "isDaytime": false,
                "temperature": 79,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "W",
                "icon": "https://api.weather.gov/icons/land/night/few?size=small",
                "shortForecast": "Mostly Clear",
                "detailedForecast": ""
            },
            {
                "number": 26,
                "name": "",
                "startTime": "2019-09-02T22:00:00-06:00",
                "endTime": "2019-09-02T23:00:00-06:00",
                "isDaytime": false,
                "temperature": 76,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "W",
                "icon": "https://api.weather.gov/icons/land/night/few?size=small",
                "shortForecast": "Mostly Clear",
                "detailedForecast": ""
            },
            {
                "number": 27,
                "name": "",
                "startTime": "2019-09-02T23:00:00-06:00",
                "endTime": "2019-09-03T00:00:00-06:00",
                "isDaytime": false,
                "temperature": 75,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "W",
                "icon": "https://api.weather.gov/icons/land/night/few?size=small",
                "shortForecast": "Mostly Clear",
                "detailedForecast": ""
            },
            {
                "number": 28,
                "name": "",
                "startTime": "2019-09-03T00:00:00-06:00",
                "endTime": "2019-09-03T01:00:00-06:00",
                "isDaytime": false,
                "temperature": 74,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "W",
                "icon": "https://api.weather.gov/icons/land/night/few?size=small",
                "shortForecast": "Mostly Clear",
                "detailedForecast": ""
            },
            {
                "number": 29,
                "name": "",
                "startTime": "2019-09-03T01:00:00-06:00",
                "endTime": "2019-09-03T02:00:00-06:00",
                "isDaytime": false,
                "temperature": 73,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "W",
                "icon": "https://api.weather.gov/icons/land/night/few?size=small",
                "shortForecast": "Mostly Clear",
                "detailedForecast": ""
            },
            {
                "number": 30,
                "name": "",
                "startTime": "2019-09-03T02:00:00-06:00",
                "endTime": "2019-09-03T03:00:00-06:00",
                "isDaytime": false,
                "temperature": 71,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "7 mph",
                "windDirection": "WNW",
                "icon": "https://api.weather.gov/icons/land/night/few?size=small",
                "shortForecast": "Mostly Clear",
                "detailedForecast": ""
            },
            {
                "number": 31,
                "name": "",
                "startTime": "2019-09-03T03:00:00-06:00",
                "endTime": "2019-09-03T04:00:00-06:00",
                "isDaytime": false,
                "temperature": 69,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "7 mph",
                "windDirection": "WNW",
                "icon": "https://api.weather.gov/icons/land/night/few?size=small",
                "shortForecast": "Mostly Clear",
                "detailedForecast": ""
            },
            {
                "number": 32,
                "name": "",
                "startTime": "2019-09-03T04:00:00-06:00",
                "endTime": "2019-09-03T05:00:00-06:00",
                "isDaytime": false,
                "temperature": 66,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "7 mph",
                "windDirection": "WNW",
                "icon": "https://api.weather.gov/icons/land/night/few?size=small",
                "shortForecast": "Mostly Clear",
                "detailedForecast": ""
            },
            {
                "number": 33,
                "name": "",
                "startTime": "2019-09-03T05:00:00-06:00",
                "endTime": "2019-09-03T06:00:00-06:00",
                "isDaytime": false,
                "temperature": 64,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "8 mph",
                "windDirection": "NW",
                "icon": "https://api.weather.gov/icons/land/night/few?size=small",
                "shortForecast": "Mostly Clear",
                "detailedForecast": ""
            },
            {
                "number": 34,
                "name": "",
                "startTime": "2019-09-03T06:00:00-06:00",
                "endTime": "2019-09-03T07:00:00-06:00",
                "isDaytime": true,
                "temperature": 63,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "8 mph",
                "windDirection": "NW",
                "icon": "https://api.weather.gov/icons/land/day/few?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 35,
                "name": "",
                "startTime": "2019-09-03T07:00:00-06:00",
                "endTime": "2019-09-03T08:00:00-06:00",
                "isDaytime": true,
                "temperature": 64,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "8 mph",
                "windDirection": "NNW",
                "icon": "https://api.weather.gov/icons/land/day/few?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 36,
                "name": "",
                "startTime": "2019-09-03T08:00:00-06:00",
                "endTime": "2019-09-03T09:00:00-06:00",
                "isDaytime": true,
                "temperature": 67,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "8 mph",
                "windDirection": "N",
                "icon": "https://api.weather.gov/icons/land/day/sct?size=small",
                "shortForecast": "Mostly Sunny",
                "detailedForecast": ""
            },
            {
                "number": 37,
                "name": "",
                "startTime": "2019-09-03T09:00:00-06:00",
                "endTime": "2019-09-03T10:00:00-06:00",
                "isDaytime": true,
                "temperature": 71,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "7 mph",
                "windDirection": "N",
                "icon": "https://api.weather.gov/icons/land/day/sct?size=small",
                "shortForecast": "Mostly Sunny",
                "detailedForecast": ""
            },
            {
                "number": 38,
                "name": "",
                "startTime": "2019-09-03T10:00:00-06:00",
                "endTime": "2019-09-03T11:00:00-06:00",
                "isDaytime": true,
                "temperature": 74,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "NNE",
                "icon": "https://api.weather.gov/icons/land/day/sct?size=small",
                "shortForecast": "Mostly Sunny",
                "detailedForecast": ""
            },
            {
                "number": 39,
                "name": "",
                "startTime": "2019-09-03T11:00:00-06:00",
                "endTime": "2019-09-03T12:00:00-06:00",
                "isDaytime": true,
                "temperature": 76,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "5 mph",
                "windDirection": "NE",
                "icon": "https://api.weather.gov/icons/land/day/sct?size=small",
                "shortForecast": "Mostly Sunny",
                "detailedForecast": ""
            },
            {
                "number": 40,
                "name": "",
                "startTime": "2019-09-03T12:00:00-06:00",
                "endTime": "2019-09-03T13:00:00-06:00",
                "isDaytime": true,
                "temperature": 78,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "5 mph",
                "windDirection": "NE",
                "icon": "https://api.weather.gov/icons/land/day/tsra_hi?size=small",
                "shortForecast": "Slight Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 41,
                "name": "",
                "startTime": "2019-09-03T13:00:00-06:00",
                "endTime": "2019-09-03T14:00:00-06:00",
                "isDaytime": true,
                "temperature": 81,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "5 mph",
                "windDirection": "ENE",
                "icon": "https://api.weather.gov/icons/land/day/tsra_hi?size=small",
                "shortForecast": "Slight Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 42,
                "name": "",
                "startTime": "2019-09-03T14:00:00-06:00",
                "endTime": "2019-09-03T15:00:00-06:00",
                "isDaytime": true,
                "temperature": 83,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "ENE",
                "icon": "https://api.weather.gov/icons/land/day/tsra_hi?size=small",
                "shortForecast": "Slight Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 43,
                "name": "",
                "startTime": "2019-09-03T15:00:00-06:00",
                "endTime": "2019-09-03T16:00:00-06:00",
                "isDaytime": true,
                "temperature": 85,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "E",
                "icon": "https://api.weather.gov/icons/land/day/tsra_hi?size=small",
                "shortForecast": "Slight Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 44,
                "name": "",
                "startTime": "2019-09-03T16:00:00-06:00",
                "endTime": "2019-09-03T17:00:00-06:00",
                "isDaytime": true,
                "temperature": 85,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "E",
                "icon": "https://api.weather.gov/icons/land/day/tsra_hi?size=small",
                "shortForecast": "Slight Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 45,
                "name": "",
                "startTime": "2019-09-03T17:00:00-06:00",
                "endTime": "2019-09-03T18:00:00-06:00",
                "isDaytime": true,
                "temperature": 84,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "E",
                "icon": "https://api.weather.gov/icons/land/day/tsra_hi?size=small",
                "shortForecast": "Slight Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 46,
                "name": "",
                "startTime": "2019-09-03T18:00:00-06:00",
                "endTime": "2019-09-03T19:00:00-06:00",
                "isDaytime": false,
                "temperature": 81,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "E",
                "icon": "https://api.weather.gov/icons/land/night/tsra_hi?size=small",
                "shortForecast": "Slight Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 47,
                "name": "",
                "startTime": "2019-09-03T19:00:00-06:00",
                "endTime": "2019-09-03T20:00:00-06:00",
                "isDaytime": false,
                "temperature": 78,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "5 mph",
                "windDirection": "ESE",
                "icon": "https://api.weather.gov/icons/land/night/tsra_hi?size=small",
                "shortForecast": "Slight Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 48,
                "name": "",
                "startTime": "2019-09-03T20:00:00-06:00",
                "endTime": "2019-09-03T21:00:00-06:00",
                "isDaytime": false,
                "temperature": 74,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "5 mph",
                "windDirection": "ESE",
                "icon": "https://api.weather.gov/icons/land/night/tsra_hi?size=small",
                "shortForecast": "Slight Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 49,
                "name": "",
                "startTime": "2019-09-03T21:00:00-06:00",
                "endTime": "2019-09-03T22:00:00-06:00",
                "isDaytime": false,
                "temperature": 70,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "3 mph",
                "windDirection": "SE",
                "icon": "https://api.weather.gov/icons/land/night/rain_showers?size=small",
                "shortForecast": "Slight Chance Rain Showers",
                "detailedForecast": ""
            },
            {
                "number": 50,
                "name": "",
                "startTime": "2019-09-03T22:00:00-06:00",
                "endTime": "2019-09-03T23:00:00-06:00",
                "isDaytime": false,
                "temperature": 68,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "3 mph",
                "windDirection": "SE",
                "icon": "https://api.weather.gov/icons/land/night/rain_showers?size=small",
                "shortForecast": "Slight Chance Rain Showers",
                "detailedForecast": ""
            },
            {
                "number": 51,
                "name": "",
                "startTime": "2019-09-03T23:00:00-06:00",
                "endTime": "2019-09-04T00:00:00-06:00",
                "isDaytime": false,
                "temperature": 66,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "2 mph",
                "windDirection": "SE",
                "icon": "https://api.weather.gov/icons/land/night/rain_showers?size=small",
                "shortForecast": "Slight Chance Rain Showers",
                "detailedForecast": ""
            },
            {
                "number": 52,
                "name": "",
                "startTime": "2019-09-04T00:00:00-06:00",
                "endTime": "2019-09-04T01:00:00-06:00",
                "isDaytime": false,
                "temperature": 65,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "2 mph",
                "windDirection": "SE",
                "icon": "https://api.weather.gov/icons/land/night/sct?size=small",
                "shortForecast": "Partly Cloudy",
                "detailedForecast": ""
            },
            {
                "number": 53,
                "name": "",
                "startTime": "2019-09-04T01:00:00-06:00",
                "endTime": "2019-09-04T02:00:00-06:00",
                "isDaytime": false,
                "temperature": 65,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "2 mph",
                "windDirection": "SSE",
                "icon": "https://api.weather.gov/icons/land/night/sct?size=small",
                "shortForecast": "Partly Cloudy",
                "detailedForecast": ""
            },
            {
                "number": 54,
                "name": "",
                "startTime": "2019-09-04T02:00:00-06:00",
                "endTime": "2019-09-04T03:00:00-06:00",
                "isDaytime": false,
                "temperature": 65,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "2 mph",
                "windDirection": "SSW",
                "icon": "https://api.weather.gov/icons/land/night/sct?size=small",
                "shortForecast": "Partly Cloudy",
                "detailedForecast": ""
            },
            {
                "number": 55,
                "name": "",
                "startTime": "2019-09-04T03:00:00-06:00",
                "endTime": "2019-09-04T04:00:00-06:00",
                "isDaytime": false,
                "temperature": 64,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "2 mph",
                "windDirection": "SW",
                "icon": "https://api.weather.gov/icons/land/night/sct?size=small",
                "shortForecast": "Partly Cloudy",
                "detailedForecast": ""
            },
            {
                "number": 56,
                "name": "",
                "startTime": "2019-09-04T04:00:00-06:00",
                "endTime": "2019-09-04T05:00:00-06:00",
                "isDaytime": false,
                "temperature": 62,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "2 mph",
                "windDirection": "SW",
                "icon": "https://api.weather.gov/icons/land/night/sct?size=small",
                "shortForecast": "Partly Cloudy",
                "detailedForecast": ""
            },
            {
                "number": 57,
                "name": "",
                "startTime": "2019-09-04T05:00:00-06:00",
                "endTime": "2019-09-04T06:00:00-06:00",
                "isDaytime": false,
                "temperature": 60,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "2 mph",
                "windDirection": "WSW",
                "icon": "https://api.weather.gov/icons/land/night/few?size=small",
                "shortForecast": "Mostly Clear",
                "detailedForecast": ""
            },
            {
                "number": 58,
                "name": "",
                "startTime": "2019-09-04T06:00:00-06:00",
                "endTime": "2019-09-04T07:00:00-06:00",
                "isDaytime": true,
                "temperature": 59,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "2 mph",
                "windDirection": "WSW",
                "icon": "https://api.weather.gov/icons/land/day/few?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 59,
                "name": "",
                "startTime": "2019-09-04T07:00:00-06:00",
                "endTime": "2019-09-04T08:00:00-06:00",
                "isDaytime": true,
                "temperature": 61,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "2 mph",
                "windDirection": "SW",
                "icon": "https://api.weather.gov/icons/land/day/few?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 60,
                "name": "",
                "startTime": "2019-09-04T08:00:00-06:00",
                "endTime": "2019-09-04T09:00:00-06:00",
                "isDaytime": true,
                "temperature": 65,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "2 mph",
                "windDirection": "SW",
                "icon": "https://api.weather.gov/icons/land/day/few?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 61,
                "name": "",
                "startTime": "2019-09-04T09:00:00-06:00",
                "endTime": "2019-09-04T10:00:00-06:00",
                "isDaytime": true,
                "temperature": 71,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "2 mph",
                "windDirection": "SSW",
                "icon": "https://api.weather.gov/icons/land/day/few?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 62,
                "name": "",
                "startTime": "2019-09-04T10:00:00-06:00",
                "endTime": "2019-09-04T11:00:00-06:00",
                "isDaytime": true,
                "temperature": 76,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "2 mph",
                "windDirection": "S",
                "icon": "https://api.weather.gov/icons/land/day/few?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 63,
                "name": "",
                "startTime": "2019-09-04T11:00:00-06:00",
                "endTime": "2019-09-04T12:00:00-06:00",
                "isDaytime": true,
                "temperature": 81,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "2 mph",
                "windDirection": "ESE",
                "icon": "https://api.weather.gov/icons/land/day/few?size=small",
                "shortForecast": "Sunny",
                "detailedForecast": ""
            },
            {
                "number": 64,
                "name": "",
                "startTime": "2019-09-04T12:00:00-06:00",
                "endTime": "2019-09-04T13:00:00-06:00",
                "isDaytime": true,
                "temperature": 85,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "2 mph",
                "windDirection": "E",
                "icon": "https://api.weather.gov/icons/land/day/tsra_hi?size=small",
                "shortForecast": "Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 65,
                "name": "",
                "startTime": "2019-09-04T13:00:00-06:00",
                "endTime": "2019-09-04T14:00:00-06:00",
                "isDaytime": true,
                "temperature": 88,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "3 mph",
                "windDirection": "E",
                "icon": "https://api.weather.gov/icons/land/day/tsra_hi?size=small",
                "shortForecast": "Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 66,
                "name": "",
                "startTime": "2019-09-04T14:00:00-06:00",
                "endTime": "2019-09-04T15:00:00-06:00",
                "isDaytime": true,
                "temperature": 89,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "3 mph",
                "windDirection": "E",
                "icon": "https://api.weather.gov/icons/land/day/tsra_hi?size=small",
                "shortForecast": "Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 67,
                "name": "",
                "startTime": "2019-09-04T15:00:00-06:00",
                "endTime": "2019-09-04T16:00:00-06:00",
                "isDaytime": true,
                "temperature": 90,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "5 mph",
                "windDirection": "E",
                "icon": "https://api.weather.gov/icons/land/day/tsra_hi?size=small",
                "shortForecast": "Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 68,
                "name": "",
                "startTime": "2019-09-04T16:00:00-06:00",
                "endTime": "2019-09-04T17:00:00-06:00",
                "isDaytime": true,
                "temperature": 90,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "5 mph",
                "windDirection": "ESE",
                "icon": "https://api.weather.gov/icons/land/day/tsra_hi?size=small",
                "shortForecast": "Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 69,
                "name": "",
                "startTime": "2019-09-04T17:00:00-06:00",
                "endTime": "2019-09-04T18:00:00-06:00",
                "isDaytime": true,
                "temperature": 89,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "5 mph",
                "windDirection": "SSE",
                "icon": "https://api.weather.gov/icons/land/day/tsra_hi?size=small",
                "shortForecast": "Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 70,
                "name": "",
                "startTime": "2019-09-04T18:00:00-06:00",
                "endTime": "2019-09-04T19:00:00-06:00",
                "isDaytime": false,
                "temperature": 86,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "5 mph",
                "windDirection": "S",
                "icon": "https://api.weather.gov/icons/land/night/tsra_hi?size=small",
                "shortForecast": "Slight Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 71,
                "name": "",
                "startTime": "2019-09-04T19:00:00-06:00",
                "endTime": "2019-09-04T20:00:00-06:00",
                "isDaytime": false,
                "temperature": 81,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "5 mph",
                "windDirection": "S",
                "icon": "https://api.weather.gov/icons/land/night/tsra_hi?size=small",
                "shortForecast": "Slight Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 72,
                "name": "",
                "startTime": "2019-09-04T20:00:00-06:00",
                "endTime": "2019-09-04T21:00:00-06:00",
                "isDaytime": false,
                "temperature": 76,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "SW",
                "icon": "https://api.weather.gov/icons/land/night/tsra_hi?size=small",
                "shortForecast": "Slight Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 73,
                "name": "",
                "startTime": "2019-09-04T21:00:00-06:00",
                "endTime": "2019-09-04T22:00:00-06:00",
                "isDaytime": false,
                "temperature": 72,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "SW",
                "icon": "https://api.weather.gov/icons/land/night/tsra_hi?size=small",
                "shortForecast": "Slight Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 74,
                "name": "",
                "startTime": "2019-09-04T22:00:00-06:00",
                "endTime": "2019-09-04T23:00:00-06:00",
                "isDaytime": false,
                "temperature": 70,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "6 mph",
                "windDirection": "WSW",
                "icon": "https://api.weather.gov/icons/land/night/tsra_hi?size=small",
                "shortForecast": "Slight Chance Showers And Thunderstorms",
                "detailedForecast": ""
            },
            {
                "number": 75,
                "name": "",
                "startTime": "2019-09-04T23:00:00-06:00",
                "endTime": "2019-09-05T00:00:00-06:00",
                "isDaytime": false,
                "temperature": 68,
                "temperatureUnit": "F",
                "temperatureTrend": null,
                "windSpeed": "5 mph",
                "windDirection": "WSW",
                "icon": "https://api.weather.gov/icons/land/night/tsra_hi?size=small",
                "shortForecast": "Slight Chance Showers And Thunderstorms",
                "detailedForecast": ""
            }
        ]
    }
}'''
}
