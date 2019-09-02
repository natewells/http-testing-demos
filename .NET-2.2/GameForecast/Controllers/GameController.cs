using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Threading.Tasks;
using GameForecast.Models;

namespace GameForecast.Controllers
{

    [Route("/api/[controller]")]
    [ApiController]
    public class GameController : Controller 
    {
        private readonly GameForecastContext _context;
        private readonly LocationService _locationService;
        private readonly WeatherService _weatherService;

        public GameController( GameForecastContext context, LocationService locationService, WeatherService weatherService ){
            _context = context;
            _locationService = locationService;
            _weatherService = weatherService;

            if( _context.games.Count() == 0 ){
                // create some data
                Team nu = new Team{ school = "University of Nebraska-Licon", mascot = "Cornhuskers" };
                Team cu = new Team{ school = "University of Colorado", mascot = "Buffaloes" };
                Stadium folsom = new Stadium{ name="Folsom Field", city = "Boulder", stateAbbreviation = "CO" };
                Game game = new Game{ 
                    home = cu, 
                    visitor = nu, 
                    location = folsom
                };

                DateTime realKickoff = DateTime.ParseExact("2019-09-07 19:30:00Z", "yyyy-MM-dd HH:mm:ssK", CultureInfo.CreateSpecificCulture("en-US") );
                if( DateTime.Now > realKickoff - new System.TimeSpan( 7, 0, 0, 0 ) ){
                    game.kickoffTime = realKickoff;
                } else {
                    game.kickoffTime = DateTime.Now + new System.TimeSpan( 0, 1, 0, 0 );
                }

                _context.stadiums.Add( folsom );
                _context.teams.Add( nu );
                _context.teams.Add( cu );
                _context.games.Add( game );
                _context.SaveChanges();
            }
        }

        // GET /api/Game/
        [HttpGet]
        public async Task<ActionResult<IEnumerable<Game>>> GetAllGames(){
            return await _context.games.ToListAsync();
        }

        // GET: /api/Game/1
        [HttpGet("{id}")]
        public async Task<ActionResult<Game>> GetGame(int id){
            var game = await _context.games.FindAsync( id );
            if( game == null ){
                return NotFound();
            }
            LocationSearchResult location = ( await _locationService.searchInCity( game.location.name, game.location.city, game.location.stateAbbreviation ) )[0];
            game.forecast = await _weatherService.GetForecast( location.latitude, location.longitude, game.kickoffTime );

            return game;
        }

    }
}