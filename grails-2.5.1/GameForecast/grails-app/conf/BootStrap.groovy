import dev.natewells.cfb.Game
import dev.natewells.cfb.Stadium
import dev.natewells.cfb.Team
import dev.natewells.location.LocationSearchResult
import dev.natewells.location.LocationService
import dev.natewells.weather.ForecastService
import dev.natewells.weather.HourForecast
import grails.converters.JSON

class BootStrap {

    def grailsApplication
    ForecastService forecastService
    LocationService locationService

    def init = { servletContext ->

        JSON.createNamedConfig( 'deep' ) {
            it.registerObjectMarshaller( Game ) { Game game ->

                LocationSearchResult stadiumLocation = locationService.searchInCity( game.location.name, game.location.city, game.location.stateAbbreviation ).first()
                HourForecast forecast = forecastService.getForecastForHour( stadiumLocation.latitude, stadiumLocation.longitude, game.kickoffTime )

                [
                        kickoff: game.kickoffTime.format('yyyy-MM-dd HH:mm:ssZ'),
                        home: [
                                school: game.home.school,
                                mascot: game.home.mascot
                        ],
                        visitor: [
                                school: game.visitor.school,
                                mascot: game.visitor.mascot
                        ],
                        forecast: [
                                temperature: forecast.temperature,
                                windSpeed: forecast.windSpeed,
                                windDirection: forecast.windDirection,
                                weatherDescription: forecast.weatherDescription
                        ]
                ]
            }
        }

        // sample data
        def nu = Team.findOrSaveBySchoolAndMascot( 'University of Nebraska-Lincoln', 'Cornhuskers' )
        def cu = Team.findOrSaveBySchoolAndMascot( 'University of Colorado', 'Buffaloes')
        def folsomField = Stadium.findOrSaveByNameAndCityAndStateAbbreviation( 'Folsom Field', 'Boulder', 'CO')

        def game01 = Game.findOrCreateByHomeAndVisitorAndLocation( cu, nu, folsomField )
        game01.kickoffTime = Date.parse('yyyy-MM-dd HH:mm:ss', '2019-09-07 14:30:00', TimeZone.getTimeZone('America/Chicago'))
        game01.save()
    }
    def destroy = {
    }
}
