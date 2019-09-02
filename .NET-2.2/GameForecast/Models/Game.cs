using System;
using System.ComponentModel.DataAnnotations.Schema;

namespace GameForecast.Models
{
    public class Game
    {
        public int id { get; set; }
        public Team visitor { get; set; }
        public Team home { get; set; }
        public Stadium location { get; set; }
        public DateTime kickoffTime { get; set; }

        [NotMapped]
        public HourForecast forecast { get; set; }
    }
}