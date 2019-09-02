using Microsoft.EntityFrameworkCore;
using GameForecast.Models;

namespace GameForecast.Models
{
    public class GameForecastContext : DbContext
    {
        public GameForecastContext( DbContextOptions<GameForecastContext> options ) : base( options ) { }

        public DbSet<Game> games { get; set; }
        public DbSet<Team> teams { get; set; }
        public DbSet<Stadium> stadiums { get; set; }
/*
        protected override void OnModelCreating( ModelBuilder builder ){
            base.OnModelCreating( builder );

            builder.Entity<Game>().ToTable("Game");
            builder.Entity<Game>().HasKey( p => p.id )
        }
 */
    }
}