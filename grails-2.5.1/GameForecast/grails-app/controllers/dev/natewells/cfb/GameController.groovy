package dev.natewells.cfb

import grails.converters.JSON

class GameController {

    static responseFormats = [ 'json' ]

    def index() {
        respond Game.list()
    }

    def show( Game game ){
        JSON.use( 'deep' ){
            respond game
        }
    }
}
