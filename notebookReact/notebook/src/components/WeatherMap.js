import { MapContainer, TileLayer, Marker, Popup, useMapEvents } from "react-leaflet";
import { useState } from "react";
import axios from "axios";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import "../styles/weathermap.css"

import iconUrl from "leaflet/dist/images/marker-icon.png";
import iconShadow from "leaflet/dist/images/marker-shadow.png";
import Navbar from "./Navbar";

let DefaultIcon = L.icon({
    iconUrl,
    shadowUrl: iconShadow,
});

L.Marker.prototype.options.icon = DefaultIcon;

function WeatherMarker({ setWeather }) {
    useMapEvents({
        click: async (e) => {
            const { lat, lng } = e.latlng;

            try {
                const res = await axios.get(
                    `https://api.openweathermap.org/data/2.5/weather`,
                    {
                        params: {
                            lat,
                            lon: lng,
                            appid: "9303c3f8b021c935ec609abcef3fa97d",
                            units: "metric",
                        },
                    }
                );

                setWeather({
                    position: [lat, lng],
                    data: res.data,
                });
            } catch (err) {
                console.error("Weather error:", err);
            }
        },
    });

    return null;
}

export default function WeatherMap() {
    const [weather, setWeather] = useState(null);

    return (
<div>

        <div style={{ height: "100vh", width: "100%" }}>
            <Navbar />
            <MapContainer
                center={[49.8, 30.1]}
                zoom={7}
                className={"map-container"}

            >

                <TileLayer
                    attribution='&copy; OpenStreetMap contributors'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />

                <WeatherMarker setWeather={setWeather} />

                {weather && (
                    <Marker position={weather.position}>
                        <Popup>
                            <div className="weather-popup">
                                <h3 className={"weather-header"}>{weather.data.name}</h3>
                                <p>
                                     Temp: {weather.data.main.temp}°C
                                    <br />
                                     Weather: {weather.data.weather[0].description}
                                    <br />
                                     Wind: {weather.data.wind.speed} m/s
                                </p>
                            </div>
                        </Popup>
                    </Marker>
                )}
            </MapContainer>
        </div>
</div>
    );
}

