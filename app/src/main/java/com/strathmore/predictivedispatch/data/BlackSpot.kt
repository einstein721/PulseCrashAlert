package com.strathmore.predictivedispatch.data

/**
 * Represents a historical road accident black spot identified from the
 * Kenya Accidents Database (clean_data.csv / NTSA Nairobi Crash Records).
 *
 * @param id Unique identifier for the black spot corridor.
 * @param roadName Name of the primary highway or urban artery (e.g., Mombasa Road, Waiyaki Way).
 * @param locationName Specific landmark or intersection (e.g., Kangemi Stage, Cabanas).
 * @param latitude Geographic latitude.
 * @param longitude Geographic longitude.
 * @param totalAccidents Number of recorded fatal/serious crashes in the dataset.
 * @param severityLevel 1 = Critical/High Fatalities (Red Alert), 0 = Moderate/Frequent Collisions (Amber Alert).
 * @param primaryCause Most frequent cause code/description (e.g., Pedestrian Crossing, Head-on, Speeding).
 * @param riskFactor Environmental and infrastructure risk factor summary.
 */
data class BlackSpot(
    val id: String,
    val roadName: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val totalAccidents: Int,
    val severityLevel: Int,
    val primaryCause: String,
    val riskFactor: String
)

object BlackSpotRepository {

    /**
     * Top accident black spots in Nairobi compiled from clean_data.csv
     * and historical NTSA traffic safety records.
     */
    val nairobiBlackSpots: List<BlackSpot> = listOf(
        BlackSpot(
            id = "BS-01",
            roadName = "Mombasa Road",
            locationName = "City Cabanas & Airport Turnoff",
            latitude = -1.3325,
            longitude = 36.8833,
            totalAccidents = 23,
            severityLevel = 1,
            primaryCause = "High-Speed Overtaking & Multi-Vehicle Collisions",
            riskFactor = "Dense multi-lane commercial corridor with rapid merging trucks and high nocturnal speeds."
        ),
        BlackSpot(
            id = "BS-02",
            roadName = "Waiyaki Way",
            locationName = "Near Kangemi Stage & Uthiru",
            latitude = -1.2683,
            longitude = 36.7511,
            totalAccidents = 17,
            severityLevel = 1,
            primaryCause = "Pedestrian Knock-Downs & High-Speed Traffic",
            riskFactor = "High-volume commuter highway cutting through dense pedestrian settlement without continuous barriers."
        ),
        BlackSpot(
            id = "BS-03",
            roadName = "Thika Superhighway",
            locationName = "Roasters & Homeland Zebra Crossing",
            latitude = -1.2335,
            longitude = 36.8785,
            totalAccidents = 12,
            severityLevel = 1,
            primaryCause = "Speeding & Jaywalking under Footbridges",
            riskFactor = "Multi-lane express carriageway with high night-time vehicle velocities."
        ),
        BlackSpot(
            id = "BS-04",
            roadName = "Kangundo Road",
            locationName = "Komarock & Kayole Junction",
            latitude = -1.2720,
            longitude = 36.9050,
            totalAccidents = 11,
            severityLevel = 1,
            primaryCause = "Boda-Boda (Motorcycle) & Matatu Collisions",
            riskFactor = "Narrow corridor with heavy mixed traffic and sudden pedestrian boarding."
        ),
        BlackSpot(
            id = "BS-05",
            roadName = "Jogoo Road",
            locationName = "City Stadium Roundabout & Makadara",
            latitude = -1.2950,
            longitude = 36.8520,
            totalAccidents = 10,
            severityLevel = 1,
            primaryCause = "Public Service Vehicle (PSV) Pedestrian Impacts",
            riskFactor = "Dense transit artery during morning and evening rush hours."
        ),
        BlackSpot(
            id = "BS-06",
            roadName = "Outer Ring Road",
            locationName = "Allsops & Kariobangi Interchange",
            latitude = -1.2505,
            longitude = 36.8795,
            totalAccidents = 8,
            severityLevel = 1,
            primaryCause = "Hit & Run & High-Impact Pedestrian Strikes",
            riskFactor = "High-speed multi-lane transit artery with complex uncontrolled crossing points."
        ),
        BlackSpot(
            id = "BS-07",
            roadName = "Eastern Bypass",
            locationName = "Near Gulf Petrol Station / Cabanas Link",
            latitude = -1.3385,
            longitude = 36.9150,
            totalAccidents = 7,
            severityLevel = 1,
            primaryCause = "Head-on Collisions on Single Carriageway Sections",
            riskFactor = "Heavy long-distance haulage freight traffic overtaking in low visibility."
        ),
        BlackSpot(
            id = "BS-08",
            roadName = "Southern Bypass",
            locationName = "Lang'ata Junction & National Park Boundary",
            latitude = -1.3250,
            longitude = 36.7650,
            totalAccidents = 5,
            severityLevel = 0,
            primaryCause = "Loss of Vehicle Control & Overturning",
            riskFactor = "Long sweeping curves with high-speed night traffic and occasional wildlife crossing."
        ),
        BlackSpot(
            id = "BS-09",
            roadName = "Haile Selassie Avenue",
            locationName = "CBD Railway Roundabout & Choo No 8",
            latitude = -1.2905,
            longitude = 36.8250,
            totalAccidents = 5,
            severityLevel = 0,
            primaryCause = "Rush Hour Congestion & Pedestrian Knock-downs",
            riskFactor = "Extreme volume of commuter foot traffic crossing multilane roundabouts."
        ),
        BlackSpot(
            id = "BS-10",
            roadName = "Ngong Road",
            locationName = "Prestige Plaza & Dagoretti Corner",
            latitude = -1.3005,
            longitude = 36.7850,
            totalAccidents = 4,
            severityLevel = 0,
            primaryCause = "U-Turn / Lane-Change Sideswipes & Cyclist Collisions",
            riskFactor = "Multiple service lane intersections and unsignalized pedestrian turns."
        )
    )
}
