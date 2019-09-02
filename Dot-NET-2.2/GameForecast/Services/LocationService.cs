using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;

namespace GameForecast.Models {
    public class LocationService {
        public HttpClient _client { get; }

        public LocationService( HttpClient client ){
            client.BaseAddress = new System.Uri( "https://maps.googleapis.com" );
            _client = client;
        }

        public async Task<List<LocationSearchResult>> searchInCity( string searchTerm, string city, string stateAbbrevation ){
            List<LocationSearchResult> results = new List<LocationSearchResult>();

            // stub out for now... 

            results.Add( new LocationSearchResult{ latitude=40.001, longitude=-105.269 } );

            return results;
        }


    }
}