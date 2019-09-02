package dev.natewells.location

import groovy.util.logging.Log4j
import wslite.rest.ContentType
import wslite.rest.RESTClient

@Log4j
class LocationService {

    def grailsApplication

    List<LocationSearchResult> searchInCity( String searchTerm, String city, String state ) {
        List<LocationSearchResult> results = []

        def params = [
                key: grailsApplication.config.gameForecast.credentials.googleApiKey,
                inputtype: 'textquery',
                fields: 'formatted_address,geometry,name',
                input: "$searchTerm $city, $state"
        ]
        log.debug("Parameters for calling Google: ${ params.toMapString(500) }")

        RESTClient client = new RESTClient("https://maps.googleapis.com/maps/api/place" )
        def response = client.get(
                path:'/findplacefromtext/json',
                query: params,
                accept: ContentType.JSON,
                connectTimeout: 2000,
                readTimeout: 10000
        )

        log.info( "Response from Google with a status code of ${ response.statusCode }")
        log.debug( "  Response payload:\n${ response.contentAsString }" )
        if( response.statusCode == 200 ){
            if( response.json.status == "OK" ){
                response.json.candidates.each{
                    results << new LocationSearchResult(
                            name: it.name,
                            fullAddress: it.formatted_address,
                            latitude: it.geometry.location.lat,
                            longitude: it.geometry.location.lng
                    )
                }
            }
        }
        results
    }
}
