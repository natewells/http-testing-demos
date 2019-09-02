using System;
using System.Net.Http;
using System.Threading.Tasks;
using Newtonsoft.Json.Linq;

namespace GameForecast.Models
{
    public class WeatherService {
        public HttpClient _client { get; }

        public WeatherService( HttpClient client ){
            client.BaseAddress = new System.Uri( "https://api.weather.gov" );
            client.DefaultRequestHeaders.Add( "user-agent", "dev.natewells.gameForecast-.netCore-2.2");
            _client = client;
        }

        public async Task<HourForecast> GetForecast( double latitude, double longitude, DateTime dateTime ){
            HourForecast hourForecast = new HourForecast();
            try{
                var pointResponse = await _client.GetAsync( String.Format( "/points/{0:F4},{1:F4}", latitude, longitude ) );

                if( !pointResponse.IsSuccessStatusCode ){
                    hourForecast.errorMessage= "Unsupported location.";
                    return hourForecast;
                }

                string content = await pointResponse.Content.ReadAsStringAsync();

                JObject pointInfo = JObject.Parse( content );

                string forecastUrl = pointInfo["properties"].Value<string>("forecastHourly");
                
                if( forecastUrl == null ){
                    hourForecast.errorMessage = "Hourly forecast product not availble.";
                    return hourForecast;
                }

                var forecastResponse = await _client.GetAsync( forecastUrl.Replace(_client.BaseAddress.ToString(), "") );

                if( !forecastResponse.IsSuccessStatusCode ){
                    hourForecast.errorMessage = "Error retrieving the forecast.";
                    return hourForecast;
                }
            
                string forecastContent = await forecastResponse.Content.ReadAsStringAsync();

                JObject forecastData = JObject.Parse( forecastContent );

                
                if( forecastData["properties"] != null && forecastData["properties"]["periods"] != null ){
                    hourForecast.errorMessage = "Forecast not availble for requested time.";
                    foreach( var p in forecastData["properties"]["periods"] ){
                        if( dateTime >= p.Value<DateTime>("startTime") && dateTime < p.Value<DateTime>("endTime") ){
                            hourForecast.temperature = p.Value<int>("temperature");
                            hourForecast.windSpeed = p.Value<string>("windSpeed");
                            hourForecast.windDirection = p.Value<string>("windDirection");
                            hourForecast.weatherDescription = p.Value<string>("shortForecast");
                            hourForecast.errorMessage = "";
                            break;
                        }
                    }
                } else {
                    hourForecast.errorMessage = "Unexcepted response from NWS.";
                }
            } catch( Exception e ){
                hourForecast.errorMessage = e.Message;
            }

            return hourForecast;
        }
    }


}