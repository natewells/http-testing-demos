namespace GameForecast.Models
{
    public class HourForecast 
    {
        public int temperature{ get; set; }
        public string windSpeed{ get; set; }
        public string windDirection{ get; set; }
        public string weatherDescription{ get; set; }
        public string errorMessage{ get; set; }
    }
}