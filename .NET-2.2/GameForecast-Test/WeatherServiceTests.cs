using System;
using GameForecast.Models; 
using Xunit;
using Xunit.Abstractions;
using System.Net.Http;
using System.Threading.Tasks;


namespace GameForecast.test {
    public class WeatherServiceTests {

        private readonly ITestOutputHelper _output;
        private WeatherService _service; 
        
        public WeatherServiceTests( ITestOutputHelper output ){
            _output = output;
            _service = new WeatherService( new HttpClient( new NwsMockHttpMessageHandler() ) );
        }
        
        [Theory]
        [InlineData( 40.001,  -105.269, "2019-09-03T19:30:00-06:00", 75, "5 mph", "E", "Chance Showers And Thunderstorms", "" )] // happy path
        [InlineData( 40.001,  -105.269, "2019-08-31T22:00:00-06:00", 71, "3 mph", "WNW", "Partly Cloudy", ""  )] // happy path; earliest possible time
        [InlineData( 40.001,  -105.269, "2019-09-07T09:59:59-06:00", 67, "2 mph", "W", "Partly Sunny", ""  )] // happy path; latest possible time
        [InlineData( 40.001,  -105.269, "2019-09-07T10:00:00-06:00", 0, null, null, null, "Forecast not availble for requested time."  )] // too far in the future
        [InlineData( 40.001,  -105.269, "2019-08-31T21:59:59-06:00", 0, null, null, null, "Forecast not availble for requested time."  )] // in the past
        [InlineData( -105.269, 40.001,  "2019-09-03T19:30:00-06:00", 0, null, null, null, "Unsupported location."  )] // invalid location (Returns a 500)
        [InlineData( 0,        0,       "2019-09-03T19:30:00-06:00", 0, null, null, null, "Unsupported location."  )] // invalid location (Returns a 404)
        [InlineData( 10,       0,       "2019-09-03T19:30:00-06:00", 0, null, null, null, "Hourly forecast product not availble."  )] // location without hourly forecasts
        [InlineData( 80,       0,       "2019-09-03T19:30:00-06:00", 0, null, null, null, "Error retrieving the forecast."  )] // hourly forecast call returns 404
        [InlineData( 81,       0,       "2019-09-03T19:30:00-06:00", 0, null, null, null, "Error retrieving the forecast."  )] // hourly forecast call returns 500
        [InlineData( 82,       0,       "2019-09-03T19:30:00-06:00", 0, null, null, null, "Unexcepted response from NWS." )] // hourly forecast call returns unexpected JSON
        [InlineData( 83,       0,       "2019-09-03T19:30:00-06:00", 0, null, null, null, "Fake exception thrown to mimic a timeout on the hourly forecast call."  )] // hourly forecast call times out
        [InlineData( 99,       0,       "2019-09-03T19:30:00-06:00", 0, null, null, null, "fake exception thrown to mimic a timeout"  )] // The point request times out
        public void test_GetForecast( 
            double latitude, 
            double longitude, 
            string dateTimeString, 
            int temp, 
            string windSpeed, 
            string windDirection, 
            string description, 
            string errorMessage 
        ){
            DateTime hour = DateTime.Parse( dateTimeString, null, System.Globalization.DateTimeStyles.RoundtripKind);
            var task = _service.GetForecast( latitude, longitude, hour );
            task.Wait();
            var forecast = task.Result;

            if( errorMessage.Length > 0 ){
                Assert.Equal( errorMessage, forecast.errorMessage );
            } else {
                Assert.Equal( temp, forecast.temperature );
                Assert.Equal( windSpeed, forecast.windSpeed );
                Assert.Equal( windDirection, forecast.windDirection );
                Assert.Equal( description, forecast.weatherDescription );
            }
            
        }

    }
}