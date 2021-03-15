package com.uwi.btmap

import com.mapbox.geojson.Point
import com.mapbox.turf.TurfMeasurement
import kotlin.math.*


//Work In Progress
class PickUpPointGenerator() {

    fun generatePickupPoint(origin : Point, dropOff : Point, passengerOrigin : Point) : Point{
        val distance = 0.05

        val closestPoint = generateClosetPoint(origin, dropOff, passengerOrigin);
        val pickUpPoint = generatePointBetween(passengerOrigin,closestPoint, distance)

        return pickUpPoint
    }

    private fun generateClosetPoint(a : Point, b: Point, c : Point): Point {
        val threshold = 10f

        val distanceAB = getDistanceBetween(a,b)
        val pointD = generatePointBetween(a,b,distanceAB/2)

        val distanceCA = getDistanceBetween(c,a)
        val distanceCB = getDistanceBetween(c,b)

        if (abs(distanceCA - distanceCB) <= threshold)
            return pointD
        if (distanceCA > distanceCB)
            return generateClosetPoint(a, pointD, c)
        else
            return generateClosetPoint(pointD, b, c)
    }

    private fun generatePointBetween(a : Point, b : Point, distance : Double) : Point {
        if (distance >= getDistanceBetween(a , b))
            return b
        if (distance <= 0)
            return a

        val xa = a.latitude()
        val ya = a.longitude()

        val xb = b.latitude()
        val yb = b.longitude()

        val x1 = xa - xb
        val y1 = ya - yb

        var angle: Double = 0.0

        if (x1 != 0.0)
            angle = atan(y1/x1)
        else
            angle = Math.PI/2

        val x2 = cos(angle)*distance
        val y2 = sin(angle)*distance

        var xc = 0.0
        var yc = 0.0

        xc = if (xa > xb){xa - x2} else{xa + x2}
        yc = if (ya > yb){ya - y2} else{ya + y2}

        return Point.fromLngLat(xc,yc)
    }

    private fun getDistanceBetween(a : Point, b : Point) : Double{
        val x = a.latitude() - b.altitude()
        val y = a.longitude() - b.longitude()

        val distance = sqrt(x.pow(2)+y.pow(2))

//        val distance = TurfMeasurement.distance(a,b)

        return distance
    }
}